package com.bookstore.repository;

import com.bookstore.model.Order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
	
	// find all orders for a specific user
	List<Order> findByUserId(Long userId);
}