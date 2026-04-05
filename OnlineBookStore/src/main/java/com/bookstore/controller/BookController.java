package com.bookstore.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookstore.dto.request.BookRequest;
import com.bookstore.dto.request.BookUpdateRequest;
import com.bookstore.dto.response.BookResponse;
import com.bookstore.service.BookService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/books")
public class BookController {

	@Autowired
	private BookService bookService;

	@PostMapping("/create")
	public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request));
	}

	@GetMapping("/get")
	public ResponseEntity<List<BookResponse>> getAllBooks() {
		return ResponseEntity.ok(bookService.getAllBooks());
	}

	@GetMapping("/get/{id}")
	public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
		return ResponseEntity.ok(bookService.getBookyId(id));
	}

	@PutMapping("/update/{id}")
	public ResponseEntity<BookResponse> updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
		return ResponseEntity.ok(bookService.updateBook(id, request));
	}

	@PatchMapping("/partialUpdate/{id}")
	public ResponseEntity<BookResponse> partialUpdateBook(@PathVariable Long id,
			@Valid @RequestBody BookUpdateRequest request) {
		return ResponseEntity.ok(bookService.partialUpdateBook(id, request));
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
		bookService.deleteBook(id);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}

	@GetMapping("/search")
	public ResponseEntity<List<BookResponse>> searchByTitle(@RequestParam String title) {
		return ResponseEntity.ok(bookService.searchBookByTitle(title));
	}
	
	@GetMapping("/author")
	public ResponseEntity<List<BookResponse>> searchByAuthor(@RequestParam String authorName) {
	    return ResponseEntity.ok(bookService.searchBookByAuthor(authorName));
	}
	
	/*
	 * GET /api/books/paged — UC10
	 *
	 * Returns paginated list of books.
	 * Query parameters:
	 *   page → page number (default 0 = first page)
	 *   size → books per page (default 10)
	 *   sort → sort field and direction
	 *         e.g. sort=price,asc or sort=title,desc
	 *
	 * Example calls:
	 *   /api/books/paged              → page 0, size 10, default sort
	 *   /api/books/paged?page=1&size=5 → page 2, 5 books per page
	 *   /api/books/paged?sort=price,asc → sorted by price ascending
	 *
	 * @PageableDefault → sets default values if not provided in request
	 */
	@GetMapping("/paged")
	public ResponseEntity<Page<BookResponse>> getAllBooksPaginated(@PageableDefault(page = 0, size = 10, sort = "id") Pageable pageable) {
	    return ResponseEntity.ok(bookService.getAllBooksPaginated(pageable));
	}

}
