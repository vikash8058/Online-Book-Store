package com.bookstore.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bookstore.dto.request.UserRegisterRequest;
import com.bookstore.dto.request.UserUpdateRequest;
import com.bookstore.dto.response.UserResponse;
import com.bookstore.exception.DuplicateEmailException;
import com.bookstore.exception.UserNotFoundException;
import com.bookstore.model.Role;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;

@Service
public class UserService {
	
	@Autowired
	private UserRepository userRepository;
	
	//helper method that convert entity -> dto
	public UserResponse toResponse(User user) {
		return UserResponse.builder()
				.id(user.getId())
				.name(user.getName())
				.email(user.getEmail())
				.role(user.getRole())
				.build();
		
	}

	//method to register user
	public UserResponse registerUser(UserRegisterRequest request) {
		
		//check if email already exist
		if(userRepository.findByEmail(request.getEmail()).isPresent()) {
			throw new DuplicateEmailException("Email already registered: " + request.getEmail());
		}
		
		User user=User.builder()
				.name(request.getName())
				.email(request.getEmail())
				.password(request.getPassword())
				.role(Role.CUSTOMER)
				.build();
		
		return toResponse(userRepository.save(user));
	}
	
	//method to get All Users
	public List<UserResponse> getAllUsers(){
		return userRepository.findAll()
				.stream()
				.map(this::toResponse)
				.toList();
	}
	
	//method to get single user by ID
	public UserResponse getUserById(Long id) {
		User user=userRepository.findById(id)
				.orElseThrow(()->new UserNotFoundException(id));
		
		return toResponse(user);
	}
	
	//method to delete user
	public void deleteUser(Long id) {
		if(!userRepository.existsById(id)) {
			throw new UserNotFoundException(id);
		}
		
		userRepository.deleteById(id);
	}
	
	//method to update user 
	public UserResponse updateUser(Long id, UserUpdateRequest request) {
		User user=userRepository.findById(id)
				.orElseThrow(()->new UserNotFoundException(id));
		
		if(request.getName()!=null) {
			user.setName(request.getName());
		}
		
		if(request.getEmail()!=null) {
			user.setEmail(request.getEmail());
		}
		
		if(request.getPassword()!=null) {
			user.setPassword(request.getPassword());
		}
		
		return toResponse(userRepository.save(user));
	}
}
