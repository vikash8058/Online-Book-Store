package com.bookstore.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookstore.dto.request.OrderRequest;
import com.bookstore.dto.request.OrderStatusUpdateRequest;
import com.bookstore.dto.response.OrderResponse;
import com.bookstore.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
	
	@Autowired
	private OrderService orderService;
	
	@PostMapping("/create/{userId}")
	public ResponseEntity<OrderResponse> createOrder(@PathVariable Long userId, @Valid @RequestBody OrderRequest request){
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(orderService.createOrder(userId, request));
	}
	
	@GetMapping("/get")
	public ResponseEntity<List<OrderResponse>> getAllOrders(){
		return ResponseEntity.ok(orderService.getAllOrders());
	}
	
	@GetMapping("/get/{id}")
	public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id){
		return ResponseEntity.ok(orderService.getOrderById(id));
	}
	
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<Void> deleteOrder(@PathVariable Long id){
		orderService.deleteOrder(id);
		
		return ResponseEntity.noContent()
				.build();
	}
	
	// update order status
	@PatchMapping("/status/{id}")
	public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
		return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
	}

	// get all orders of a user
	@GetMapping("/user/{userId}")
	public ResponseEntity<List<OrderResponse>> getOrdersByUser(@PathVariable Long userId) {
	    return ResponseEntity.ok(orderService.getOrdersByUser(userId));
	}
}
