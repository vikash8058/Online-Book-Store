package com.bookstore.service;

import com.bookstore.dto.request.PaymentVerifyRequest;
import com.bookstore.dto.request.PlaceOrderRequest;
import com.bookstore.dto.response.OrderResponse;
import com.bookstore.dto.response.PaymentResponse;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.UserNotFoundException;
import com.bookstore.model.*;
import com.bookstore.repository.*;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final AddressService addressService;
    private final OrderService orderService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // ── Helper: get logged-in user ────────────────────────────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    /*
     * Step 1 — Called when user clicks "Pay Now" or "Place COD Order"
     *
     * COD flow:
     *   → Build order from cart
     *   → Save address snapshot on order
     *   → Save payment (mode=COD, status=PENDING)
     *   → Clear cart
     *   → Return order directly
     *
     * ONLINE flow:
     *   → Create Razorpay order (just a payment intent, NOT our order yet)
     *   → Return razorpayOrderId + amount to frontend
     *   → Frontend opens Razorpay popup
     *   → After payment → verify() is called
     */
    @Transactional
    public Object initiatePayment(PlaceOrderRequest request) throws RazorpayException {

        User user = getCurrentUser();
        Address address = addressService.getAddressById(request.getAddressId());

        // Ownership check on address
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not your address");
        }

        // Load cart
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty. Add items before checkout.");
        }

        // Calculate total
        double total = cart.getItems().stream()
                .mapToDouble(item -> item.getBook().getPrice() * item.getQuantity())
                .sum();

        PaymentMode mode = PaymentMode.valueOf(request.getPaymentMode().toUpperCase());

        if (mode == PaymentMode.COD) {
            // COD — create order immediately
            Order order = buildOrderFromCart(user, cart, address);
            orderRepository.save(order);

            // Save payment record
            Payment payment = Payment.builder()
                    .order(order)
                    .amount(total)
                    .paymentMode(PaymentMode.COD)
                    .status(PaymentStatus.PENDING)
                    .build();
            paymentRepository.save(payment);

            // Clear cart
            cart.getItems().clear();
            cartRepository.save(cart);

            return orderService.toResponse(order);

        } else {
            // ONLINE — create Razorpay order first
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject options = new JSONObject();
            // Razorpay needs amount in paise (1 INR = 100 paise)
            options.put("amount", (int)(total * 100));
            options.put("currency", "INR");
            options.put("receipt", "order_" + user.getId() + "_" + System.currentTimeMillis());

            com.razorpay.Order razorpayOrder = razorpay.orders.create(options);
            String rzpOrderId = razorpayOrder.get("id");

            // Save a pending payment record with razorpayOrderId
            // Actual order is created only after payment verification
            Payment payment = Payment.builder()
                    .amount(total)
                    .paymentMode(PaymentMode.ONLINE)
                    .status(PaymentStatus.PENDING)
                    .razorpayOrderId(rzpOrderId)
                    .build();

            // Temporarily store addressId + cartId in razorpay receipt
            // We retrieve them in verify()
            // Store in a temp map approach — use razorpayOrderId as key
            // Simple approach: store in payment notes via a transient field approach
            // We'll pass addressId back to frontend and re-send in verify request

            // Return payment info to frontend
            return PaymentResponse.builder()
                    .razorpayOrderId(rzpOrderId)
                    .amount(total)
                    .paymentMode("ONLINE")
                    .status("PENDING")
                    .build();
        }
    }

    /*
     * Step 2 — Called after Razorpay payment success on frontend
     * Verifies payment signature → creates order → clears cart
     */
    @Transactional
    public OrderResponse verifyPayment(PaymentVerifyRequest request, Long addressId)
            throws Exception {

        // Verify Razorpay signature
        // Signature = HMAC-SHA256(razorpayOrderId + "|" + razorpayPaymentId, secret)
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        String generatedSignature = hmacSha256(payload, razorpayKeySecret);

        if (!generatedSignature.equals(request.getRazorpaySignature())) {
            throw new RuntimeException("Payment verification failed. Invalid signature.");
        }

        // Signature valid — create order now
        User user = getCurrentUser();
        Address address = addressService.getAddressById(addressId);

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        double total = cart.getItems().stream()
                .mapToDouble(item -> item.getBook().getPrice() * item.getQuantity())
                .sum();

        Order order = buildOrderFromCart(user, cart, address);
        orderRepository.save(order);

        // Save payment as SUCCESS
        Payment payment = Payment.builder()
                .order(order)
                .razorpayOrderId(request.getRazorpayOrderId())
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .amount(total)
                .paymentMode(PaymentMode.ONLINE)
                .status(PaymentStatus.SUCCESS)
                .paidAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        // Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        return orderService.toResponse(order);
    }

    /*
     * Shared helper — builds Order entity from cart
     * Validates stock, deducts stock, snapshots address
     */
    private Order buildOrderFromCart(User user, Cart cart, Address address) {

        Order order = Order.builder()
                .user(user)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .totalAmount(0.0)
                .orderItems(new ArrayList<>())
                // Snapshot address fields directly on order
                // So even if user deletes address, order history is preserved
                .deliveryFullName(address.getFullName())
                .deliveryPhone(address.getPhone())
                .deliveryAddressLine(address.getAddressLine())
                .deliveryCity(address.getCity())
                .deliveryState(address.getState())
                .deliveryPincode(address.getPincode())
                .build();

        double total = 0.0;
        for (CartItem cartItem : cart.getItems()) {
            Book book = cartItem.getBook();

            // Stock check
            if (book.getStock() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for: " + book.getTitle() +
                        ". Available: " + book.getStock() +
                        ", In cart: " + cartItem.getQuantity()
                );
            }

            // Deduct stock
            book.setStock(book.getStock() - cartItem.getQuantity());
            bookRepository.save(book);

            OrderItem item = OrderItem.builder()
                    .book(book)
                    .quantity(cartItem.getQuantity())
                    .price(book.getPrice())
                    .order(order)
                    .build();

            order.getOrderItems().add(item);
            total += book.getPrice() * cartItem.getQuantity();
        }

        order.setTotalAmount(total);
        return order;
    }

    // ── HMAC-SHA256 signature verification ───────────────────────────────────
    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes()));
    }
}