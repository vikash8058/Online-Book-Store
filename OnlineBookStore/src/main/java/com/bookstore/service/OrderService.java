package com.bookstore.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.bookstore.dto.request.OrderItemRequest;
import com.bookstore.dto.request.OrderRequest;
import com.bookstore.dto.request.OrderStatusUpdateRequest;
import com.bookstore.dto.response.OrderItemResponse;
import com.bookstore.dto.response.OrderResponse;
import com.bookstore.exception.BookNotFoundException;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.InvalidOrderStatusException;
import com.bookstore.exception.OrderNotFoundException;
import com.bookstore.exception.UserNotFoundException;
import com.bookstore.model.Book;
import com.bookstore.model.Cart;
import com.bookstore.model.CartItem;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.model.OrderStatus;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final BookRepository bookRepository;
	private final UserRepository userRepository;
	private final OrderRepository orderRepository;
	private final CartService cartService;
	private final CartRepository cartRepository;
	
	//helper method that convert entity to dto
	private OrderResponse toResponse(Order order) {
		List<OrderItemResponse> itemsResponse=order.getOrderItems()
				.stream()
				.map(item->OrderItemResponse.builder()
						.id(item.getId())
						.bookId(item.getBook().getId())
						.bookTitle(item.getBook().getTitle())
						.price(item.getPrice())
						.quantity(item.getQuantity())
						.build()
						)
			.toList();
		
		return OrderResponse.builder()
				.id(order.getId())
				.orderDate(order.getOrderDate())
				.totalAmount(order.getTotalAmount())
				.status(order.getStatus())
				.userId(order.getUser().getId())
				.items(itemsResponse)
				.build();
				
	}
	
	//method to create order
	/*
	 * POST /api/orders/place — UC12
	 * Cart-driven order placement.
	 *
	 * Flow:
	 *  1. Get logged-in user from JWT (same as CartService pattern)
	 *  2. Load their cart
	 *  3. Validate cart is not empty
	 *  4. For each cart item → check stock → deduct stock → build OrderItem
	 *  5. Save order
	 *  6. Clear cart
	 */
	@Transactional
	public OrderResponse placeOrderFromCart() {

	    // 1 — get logged-in user
	    String email = SecurityContextHolder.getContext()
	            .getAuthentication().getName();
	    User user = userRepository.findByEmail(email)
	            .orElseThrow(() -> new RuntimeException("User not found"));

	    // 2 — load cart
	    Cart cart = cartRepository.findByUser(user)
	            .orElseThrow(() -> new RuntimeException("Cart not found. Please add items first."));

	    // 3 — empty cart check
	    if (cart.getItems().isEmpty()) {
	        throw new RuntimeException("Cart is empty. Please add items before placing an order.");
	    }

	    // 4 — build order
	    Order order = Order.builder()
	            .orderDate(LocalDateTime.now())
	            .user(user)
	            .totalAmount(0.0)
	            .status(OrderStatus.PENDING)
	            .orderItems(new ArrayList<>())
	            .build();

	    double total = 0.0;

	    for (CartItem cartItem : cart.getItems()) {

	        Book book = cartItem.getBook();

	        // Stock check — always re-verify at order time
	        // (stock may have changed since item was added to cart)
	        if (book.getStock() < cartItem.getQuantity()) {
	            throw new InsufficientStockException(
	                    "Insufficient stock for: " + book.getTitle() +
	                    ". Available: " + book.getStock() +
	                    ", In your cart: " + cartItem.getQuantity()
	            );
	        }

	        // Deduct stock
	        book.setStock(book.getStock() - cartItem.getQuantity());
	        bookRepository.save(book);

	        // Build order item
	        OrderItem item = OrderItem.builder()
	                .book(book)
	                .quantity(cartItem.getQuantity())
	                .price(book.getPrice())   // snapshot price at order time
	                .order(order)
	                .build();

	        order.getOrderItems().add(item);
	        total += book.getPrice() * cartItem.getQuantity();
	    }

	    // 5 — save order
	    order.setTotalAmount(total);
	    Order saved = orderRepository.save(order);

	    // 6 — clear cart after successful order
	    cartService.clearCart();

	    return toResponse(saved);
	}
	
	//method get all orders
	public List<OrderResponse> getAllOrders(){
		return orderRepository.findAll()
				.stream()
				.map(this::toResponse)
				.toList();
	}
	
	//method to get order by id
	public OrderResponse getOrderById(Long id) {
		Order order=orderRepository.findById(id)
				.orElseThrow(()->new OrderNotFoundException(id));
		
		return toResponse(order);
	}
	
	//method to delete order
	public void deleteOrder(Long id) {
		if(!orderRepository.existsById(id)){
			throw new OrderNotFoundException(id);
		}
		
		orderRepository.deleteById(id);
	}
	
	//method to update status or order
	@Transactional
	public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
		
		//1- fetch order
		Order order=orderRepository.findById(id)
				.orElseThrow(() -> new OrderNotFoundException(id));
		
		//2- validate status transition
		validateStatusTransition(order.getStatus(), request.getStatus());
		
		//3- if cancelling, restore stock of each book
		if(request.getStatus() == OrderStatus.CANCELLED) {
			for(OrderItem item:order.getOrderItems()) {
				Book book=item.getBook();
				book.setStock(book.getStock()+item.getQuantity());
				bookRepository.save(book);
			}
		}
		
		
		// 4. update status
		order.setStatus(request.getStatus());
		
		// 5. save and return
		return toResponse(orderRepository.save(order));
		
		
	}
	
	private void validateStatusTransition(OrderStatus current, OrderStatus next) {
		
		if(current==OrderStatus.CANCELLED) {
			throw new InvalidOrderStatusException("Cannot update status of a CANCELLED order");
		}
		
		if (current == OrderStatus.DELIVERED) {
			throw new InvalidOrderStatusException("Cannot update status of a DELIVERED order");
		}
		
		if(next==OrderStatus.PENDING) {
			throw new InvalidOrderStatusException("Cannot move order back to PENDING");
		}
	}
	
	// getOrdersByUser() — UC12 updated
	// Reads logged-in user from JWT — no userId param needed
	public List<OrderResponse> getOrdersByUser() {

	    // Get email directly from JWT — already verified by JwtAuthFilter
	    String email = SecurityContextHolder.getContext()
	            .getAuthentication()
	            .getName();

	    // Fetch user by email
	    User user = userRepository.findByEmail(email)
	            .orElseThrow(() -> new RuntimeException("User not found"));

	    // Return only this user's orders
	    return orderRepository.findByUserId(user.getId())
	            .stream()
	            .map(this::toResponse)
	            .toList();
	}
	
	/*
	 * cancelOrder() — UC12
	 * Customer can cancel their own order — only if status is PENDING.
	 * Ownership check → order must belong to logged-in user.
	 * Stock is restored on cancel (same logic as admin cancel).
	 */
	@Transactional
	public OrderResponse cancelOrder(Long orderId) {

	    // Get logged-in user from JWT
	    String email = SecurityContextHolder.getContext()
	            .getAuthentication().getName();
	    User user = userRepository.findByEmail(email)
	            .orElseThrow(() -> new RuntimeException("User not found"));

	    // Fetch order
	    Order order = orderRepository.findById(orderId)
	            .orElseThrow(() -> new OrderNotFoundException(orderId));

	    // Ownership check — order must belong to this user
	    if (!order.getUser().getId().equals(user.getId())) {
	        throw new RuntimeException("You are not authorized to cancel this order");
	    }

	    // Only PENDING orders can be cancelled by customer
	    if (order.getStatus() != OrderStatus.PENDING) {
	        throw new InvalidOrderStatusException(
	            "Order cannot be cancelled. Current status: " + order.getStatus() +
	            ". Only PENDING orders can be cancelled."
	        );
	    }

	    // Restore stock for each book
	    for (OrderItem item : order.getOrderItems()) {
	        Book book = item.getBook();
	        book.setStock(book.getStock() + item.getQuantity());
	        bookRepository.save(book);
	    }

	    // Update status to CANCELLED
	    order.setStatus(OrderStatus.CANCELLED);
	    return toResponse(orderRepository.save(order));
	}
}
