package com.aritzia.availability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.aritzia.availability.model.Product;

import java.time.Instant;

public record AvailabilityResponse(
        String productId,
        boolean inStock,
        int availableQuantity,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant lastUpdated) {

    public static AvailabilityResponse from(Product product) {
        return new AvailabilityResponse(
                product.productId(),
                product.inStock(),
                product.quantity(),
                product.lastUpdated());
    }
}
