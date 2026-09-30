package com.aritzia.availability.model;

import java.time.Instant;

public record Product(String productId, int quantity, Instant lastUpdated) {

    // The spec doesn't define negative stock; treat it as corrupt data rather than serving it.
    public Product {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative for productId=" + productId);
        }
    }

    public boolean inStock() {
        return quantity > 0;
    }
}
