package com.bookstore.controller;

import com.bookstore.dto.request.AddressRequest;
import com.bookstore.dto.request.AddressUpdateRequest;
import com.bookstore.dto.response.AddressResponse;
import com.bookstore.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

	private final AddressService addressService;

	@GetMapping("/my")
	public ResponseEntity<List<AddressResponse>> getMyAddresses() {
		return ResponseEntity.ok(addressService.getMyAddresses());
	}

	@PostMapping("/add")
	public ResponseEntity<AddressResponse> addAddress(@Valid @RequestBody AddressRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(addressService.addAddress(request));
	}

	@PatchMapping("/update/{id}")
	public ResponseEntity<AddressResponse> updateAddress(@PathVariable Long id, @RequestBody AddressUpdateRequest request) {
	    return ResponseEntity.ok(addressService.updateAddress(id, request));
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
		addressService.deleteAddress(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/default/{id}")
	public ResponseEntity<AddressResponse> setDefault(@PathVariable Long id) {
		return ResponseEntity.ok(addressService.setDefault(id));
	}
}