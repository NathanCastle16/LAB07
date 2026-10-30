package com.example.lab7.repository;

import com.example.lab7.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    Product findByNameIgnoreCase(String name);
}
