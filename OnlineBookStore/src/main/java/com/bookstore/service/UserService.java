package com.bookstore.service;

import com.bookstore.dto.request.UserUpdateRequest;
import com.bookstore.dto.response.UserResponse;
import com.bookstore.exception.UserNotFoundException;
import com.bookstore.model.AuthProvider;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Helper method entity → dto
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .build();
    }

    // Get all users
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    //Get single user by ID 
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toResponse(user);
    }

    //Delete user
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        try {
            userRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            // User has orders in DB — cannot delete due to FK constraint
            throw new RuntimeException(
                "Cannot delete this user. They have existing orders in the system."
            );
        }
    }

    // Update user
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) {
            user.setPassword(request.getPassword());
        }

        return toResponse(userRepository.save(user));
    }
    
    public UserResponse getCurrentUser() {
        // Get current user from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        return toResponse(user);
    }
    
    //search user by name
    public List<UserResponse> searchUserByName(String name){
    	List<User> users=userRepository.findByNameContainingIgnoreCase(name);
    	return users.stream()
    			.map(this::toResponse)
    			.toList();
    }
    
    //method to search user by auth provider
    public List<UserResponse> searchUserByProvider(String provider) {

        AuthProvider authProvider;

        try {
            authProvider = AuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid provider value: " + provider);
        }

        List<User> users=userRepository.findByAuthProvider(authProvider);
        return users.stream()
        		.map(this::toResponse)
        		.toList();
        		
    }
}