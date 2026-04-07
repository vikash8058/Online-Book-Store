package com.bookstore.repository;

import com.bookstore.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
	
//	List<Book> findByTitle(String title);
//	
//	List<Book> findByAuthorName(String authorName);
	
	List<Book> findByTitleContainingIgnoreCase(String title);
	List<Book> findByAuthorNameContainingIgnoreCase(String authorName);
}