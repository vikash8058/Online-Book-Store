package com.bookstore.service;

import com.bookstore.dto.request.AddToCartRequest;
import com.bookstore.dto.request.UpdateCartItemRequest;
import com.bookstore.dto.response.CartItemResponse;
import com.bookstore.dto.response.CartResponse;
import com.bookstore.exception.BookNotFoundException;
import com.bookstore.exception.CartItemNotFoundException;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.model.*;
import com.bookstore.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    // ── Helper: get currently logged-in user from JWT token
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName(); // email from JWT
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Helper: get or create cart for user 
    // If user has no cart yet → create one automatically on first add
    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().user(user).build()
                ));
    }

    // ── Helper: map CartItem entity → CartItemResponse DTO 
    private CartItemResponse toItemResponse(CartItem item) {
        Book book = item.getBook();
        return CartItemResponse.builder()
                .cartItemId(item.getId())
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .authorName(book.getAuthorName())
                .price(book.getPrice())
                .quantity(item.getQuantity())
                .itemTotal(book.getPrice() * item.getQuantity())
                .build();
    }

    // ── Helper: build full CartResponse from Cart entity 
    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        double grandTotal = itemResponses.stream()
                .mapToDouble(CartItemResponse::getItemTotal)
                .sum();

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemResponses)
                .totalItems(itemResponses.size())
                .grandTotal(grandTotal)
                .build();
    }

    // ── POST /api/cart/add 
    // If book already in cart → increase quantity
    // If new book → add as new CartItem
    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {

        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new BookNotFoundException(
                        "Book not found with id: " + request.getBookId()));

        Optional<CartItem> optionalItem =
                cartItemRepository.findByCartAndBook(cart, book);

        int alreadyInCart = optionalItem
                .map(CartItem::getQuantity)
                .orElse(0);

        int totalRequested = alreadyInCart + request.getQuantity();

        // Step 1: Validate first
        if (totalRequested > book.getStock()) {
            throw new InsufficientStockException(
                    "Cannot add " + request.getQuantity() +
                    ". Available: " + book.getStock() +
                    ", Already in cart: " + alreadyInCart
            );
        }

        // Step 2: Then update
        if (optionalItem.isPresent()) {
            CartItem existingItem = optionalItem.get();
            existingItem.setQuantity(totalRequested);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .book(book)
                    .quantity(request.getQuantity())
                    .build();

            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        // Step 3: Return updated cart
        Cart updatedCart = cartRepository.findByUser(user).orElseThrow();
        return toCartResponse(updatedCart);
    }

    // ── GET /api/cart 
    public CartResponse getCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        return toCartResponse(cart);
    }

    // ── PATCH /api/cart/update/{cartItemId} 
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, UpdateCartItemRequest request) {
        User user = getCurrentUser();

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        // Security check: make sure this item belongs to the current user's cart
        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new UsernameNotFoundException("Access denied: this cart item does not belong to you");
        }

        // After fetching the item, before saving:
        Book book = item.getBook();
        if (request.getQuantity() > book.getStock()) {
            throw new InsufficientStockException(
                    "Cannot set quantity to " + request.getQuantity() +
                    ". Available stock: " + book.getStock()
            );
        }
        
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        Cart cart = cartRepository.findByUser(user).orElseThrow();
        return toCartResponse(cart);
    }

    // ── DELETE /api/cart/remove/{cartItemId}
    @Transactional
    public CartResponse removeCartItem(Long cartItemId) {
        User user = getCurrentUser();

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        // Security check: item must belong to current user
        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: this cart item does not belong to you");
        }

        Cart cart = item.getCart();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        Cart updatedCart = cartRepository.findByUser(user).orElseThrow();
        return toCartResponse(updatedCart);
    }

    // ── DELETE /api/cart/clear
    // Clears all items from cart (useful after order is placed)
    @Transactional
    public void clearCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}