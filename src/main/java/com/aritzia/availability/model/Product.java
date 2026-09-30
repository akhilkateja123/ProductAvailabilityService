package com.aritzia.availability.model;

import java.time.Instant;

public record Product(String productId, int quantity, Instant lastUpdated) {

    public boolean inStock() {
        return quantity > 0;
    }
}
