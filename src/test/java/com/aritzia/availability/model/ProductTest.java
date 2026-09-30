package com.aritzia.availability.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void rejectsNegativeQuantity() {
        assertThatThrownBy(() -> new Product("10001", -1, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroQuantityIsOutOfStock() {
        assertThat(new Product("10001", 0, Instant.now()).inStock()).isFalse();
    }
}
