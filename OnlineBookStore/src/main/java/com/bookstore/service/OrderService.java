package com.bookstore.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.model.OrderStatus;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
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
	@Transactional
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
			
			if(book.getStock()<itemRequest.getQuantity()) {
				throw new InsufficientStockException(
                        "Insufficient stock for book: " + book.getTitle() +
                        ". Available stock: " + book.getStock() +
                        ", Requested quantity: " + itemRequest.getQuantity()
                );
			}
			
			book.setStock(book.getStock()-itemRequest.getQuantity());
			bookRepository.save(book);
			
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
        cartService.clearCart(); // UC12 — clear cart after order is placed
       
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
	
	// method to get orders by userId
	public List<OrderResponse> getOrdersByUser(Long userId) {

	    // throw exception if user does NOT exist
	    if (!userRepository.existsById(userId)) {
	        throw new UserNotFoundException(userId);
	    }

	    // find all orders belonging to this user
	    return orderRepository.findByUserId(userId)
	            .stream()
	            .map(this::toResponse)
	            .toList();
	}
}
