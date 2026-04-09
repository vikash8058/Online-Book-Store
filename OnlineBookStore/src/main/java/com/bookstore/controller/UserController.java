package com.bookstore.controller;

import com.bookstore.dto.request.UserUpdateRequest;
import com.bookstore.dto.response.UserResponse;
import com.bookstore.model.User;
import com.bookstore.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    //Get all users (ADMIN only)
    @GetMapping("/get")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Get user by ID (ADMIN only)
    @GetMapping("/get/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    //Update user (ADMIN only)
    @PatchMapping("/update/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // Delete user (ADMIN only)
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    
    // Get current user profile (for authenticated users)
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }
    
    //get users by name
    @GetMapping("/search")  // Admin only
    public ResponseEntity<List<UserResponse>> getUserByName(@RequestParam String name){
    	return ResponseEntity.ok(userService.searchUserByName(name));
    }
    
    //get user by auth provider
    @GetMapping("/filter") //Admin only
    public ResponseEntity<List<UserResponse>> getUsersByProvider(
            @RequestParam String provider) {

        return ResponseEntity.ok(userService.searchUserByProvider(provider));
    }
}