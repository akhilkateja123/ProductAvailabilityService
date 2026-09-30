package com.aritzia.availability.repository;

import com.aritzia.availability.model.Product;

import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(String productId);
}
