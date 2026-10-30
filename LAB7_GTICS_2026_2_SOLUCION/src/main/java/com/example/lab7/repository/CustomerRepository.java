package com.example.lab7.repository;

import com.example.lab7.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Customer findByDocument(String document);
}
