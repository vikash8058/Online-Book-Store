package com.bookstore.repository;

import com.bookstore.model.Cart;
import com.bookstore.model.CartItem;
import com.bookstore.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Check if this book already exists in the cart → update qty instead of duplicate
    Optional<CartItem> findByCartAndBook(Cart cart, Book book);
}