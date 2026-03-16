package com.bookstore.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookstore.dto.request.OrderItemRequest;
import com.bookstore.dto.request.OrderRequest;
import com.bookstore.dto.response.OrderItemResponse;
import com.bookstore.dto.response.OrderResponse;
import com.bookstore.exception.BookNotFoundException;
import com.bookstore.exception.OrderNotFoundException;
import com.bookstore.exception.UserNotFoundException;
import com.bookstore.model.Book;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.model.OrderStatus;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;

@Service
public class OrderService {

	@Autowired
	private BookRepository bookRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private OrderRepository orderRepository;
	
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
	public OrderResponse createOrder(Long userId, OrderRequest request) {
		
		//1- Fetch User
		User user=userRepository.findById(userId)
				.orElseThrow(()->new UserNotFoundException(userId));
		
		///2- Build order shell
		Order order=Order.builder()
				.orderDate(LocalDateTime.now())
				.user(user)
				.totalAmount(0.0)
				.status(OrderStatus.PENDING)
				.orderItems(new ArrayList<>())
				.build();
		
		// 3. Build order items and calculate total
		double total=0.0;
		for(OrderItemRequest itemRequest:request.getItems()) {
			
			Book book=bookRepository.findById(itemRequest.getBookId())
					.orElseThrow(()->new BookNotFoundException(itemRequest.getBookId()));
			
			OrderItem item=OrderItem.builder()
					.book(book)
					.quantity(itemRequest.getQuantity())
					.price(book.getPrice())
					.order(order)
					.build();
			
			order.getOrderItems().add(item);
			total+=book.getPrice()*itemRequest.getQuantity();
		}
		
		// 4. Set total and save
		order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
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
}
