package com.bookstore.repository;

import com.bookstore.model.AuthProvider;
import com.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    List<User> findByNameContainingIgnoreCase(String name);
    
    List<User> findByAuthProvider(AuthProvider authProvider);
}