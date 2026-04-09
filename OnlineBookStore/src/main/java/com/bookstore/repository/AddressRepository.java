package com.bookstore.repository;

import com.bookstore.model.Address;
import com.bookstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // Get all addresses for a user
    List<Address> findByUser(User user);

    // Get default address for a user
    Optional<Address> findByUserAndIsDefaultTrue(User user);

    // Count how many addresses a user has
    int countByUser(User user);
}