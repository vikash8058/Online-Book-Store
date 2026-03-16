package com.bookstore.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookstore.dto.request.BookRequest;
import com.bookstore.dto.request.BookUpdateRequest;
import com.bookstore.dto.response.BookResponse;
import com.bookstore.exception.BookNotFoundException;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;

@Service
public class BookService {

	@Autowired
	private BookRepository bookRepository;

	// this is helper method that convert an entity to DTO
	private BookResponse toResponse(Book book) {
		return BookResponse.builder()
				.id(book.getId())
				.title(book.getTitle())
				.authorName(book.getAuthorName())
				.price(book.getPrice())
				.stock(book.getStock())
				.build();
	}

	// Method to create a Book
	public BookResponse createBook(BookRequest request) {
		Book book = Book.builder()
				.title(request.getTitle())
				.authorName(request.getAuthorName())
				.price(request.getPrice())
				.stock(request.getStock())
				.build();

		return toResponse(bookRepository.save(book));
	}

	// method to get all books
	public List<BookResponse> getAllBooks() {
		return bookRepository.findAll()
				.stream()
				.map(this::toResponse)
				.toList();
	}

	// method to get a single book by id
	public BookResponse getBookyId(Long id) {
		Book book = bookRepository.findById(id).
				orElseThrow(() -> new BookNotFoundException(id));

		return toResponse(book);
	}

	// method to update book(Fully)
	public BookResponse updateBook(Long id, BookRequest request) {
		Book book = bookRepository.findById(id).
				orElseThrow(() -> new BookNotFoundException(id));

		book.setTitle(request.getTitle());
		book.setAuthorName(request.getAuthorName());
		book.setPrice(request.getPrice());
		book.setStock(request.getStock());

		return toResponse(bookRepository.save(book));
	}
	
	//method to update book partially
	public BookResponse partialUpdateBook(Long id, BookUpdateRequest request) {
		Book book = bookRepository.findById(id).
				orElseThrow(() -> new BookNotFoundException(id));
		
		// Only update the field if the client actually sent it (not null)
        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }

        if (request.getAuthorName() != null) {
            book.setAuthorName(request.getAuthorName());
        }

        if (request.getPrice() != null) {
            book.setPrice(request.getPrice());
        }

        if (request.getStock() != null) {
            book.setStock(request.getStock());
        }
        
        return toResponse(bookRepository.save(book));
	}
	
	
	//method to delete book
	public void deleteBook(Long id) {
		if(!bookRepository.existsById(id)) {
			throw new BookNotFoundException(id);
		}
		
		bookRepository.deleteById(id);
	}
	
	//method to search a book
	public List<BookResponse> searchBookByTitle(String title) {
		return bookRepository.findByTitle(title)
				.stream()
				.map(this::toResponse)
				.toList();	
	}
}
