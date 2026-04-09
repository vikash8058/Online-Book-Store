package com.bookstore.service;

import com.bookstore.dto.request.AddressRequest;
import com.bookstore.dto.request.AddressUpdateRequest;
import com.bookstore.dto.response.AddressResponse;
import com.bookstore.exception.UserNotFoundException;
import com.bookstore.model.Address;
import com.bookstore.model.User;
import com.bookstore.repository.AddressRepository;
import com.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    //  Helper: get logged-in user from JWT 
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    // ── Helper: entity → DTO 
    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phone(address.getPhone())
                .addressLine(address.getAddressLine())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .isDefault(address.isDefault())
                .build();
    }

    // ── GET /api/address/my 
    public List<AddressResponse> getMyAddresses() {
        User user = getCurrentUser();
        return addressRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── POST /api/address/add 
    // If this is user's first address → auto set as default
    // If request says isDefault=true → unset old default first
    @Transactional
    public AddressResponse addAddress(AddressRequest request) {
        User user = getCurrentUser();

        boolean isFirstAddress = addressRepository.countByUser(user) == 0;

        // If new address is default OR it's the first address → clear old default
        if (request.isDefault() || isFirstAddress) {
            addressRepository.findByUserAndIsDefaultTrue(user)
                    .ifPresent(existing -> {
                        existing.setDefault(false);
                        addressRepository.save(existing);
                    });
        }

        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .isDefault(request.isDefault() || isFirstAddress)
                .build();

        return toResponse(addressRepository.save(address));
    }

    //partial update
    @Transactional
    public AddressResponse updateAddress(Long id, AddressUpdateRequest request) {

        User user = getCurrentUser();

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + id));

        // Ownership check
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to update this address");
        }

        // Partial update (PATCH behavior)
        if (request.getFullName() != null) {
            address.setFullName(request.getFullName());
        }

        if (request.getPhone() != null) {
            address.setPhone(request.getPhone());
        }

        if (request.getAddressLine() != null) {
            address.setAddressLine(request.getAddressLine());
        }

        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }

        if (request.getState() != null) {
            address.setState(request.getState());
        }

        if (request.getPincode() != null) {
            address.setPincode(request.getPincode());
        }

        return toResponse(addressRepository.save(address));
    }

    // ── DELETE /api/address/delete/{id}
    @Transactional
    public void deleteAddress(Long id) {
        User user = getCurrentUser();

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not your address");
        }

        addressRepository.delete(address);

        // If deleted address was default → set first remaining as default
        List<Address> remaining = addressRepository.findByUser(user);
        if (!remaining.isEmpty() && address.isDefault()) {
            remaining.get(0).setDefault(true);
            addressRepository.save(remaining.get(0));
        }
    }

    // ── PATCH /api/address/default/{id} 
    @Transactional
    public AddressResponse setDefault(Long id) {
        User user = getCurrentUser();

        // Unset current default
        addressRepository.findByUserAndIsDefaultTrue(user)
                .ifPresent(existing -> {
                    existing.setDefault(false);
                    addressRepository.save(existing);
                });

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not your address");
        }

        address.setDefault(true);
        return toResponse(addressRepository.save(address));
    }

    // ── Used by PaymentService/OrderService internally
    public Address getAddressById(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));
    }
}