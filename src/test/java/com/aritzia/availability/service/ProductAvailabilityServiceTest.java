package com.aritzia.availability.service;

import com.aritzia.availability.dto.AvailabilityResponse;
import com.aritzia.availability.exception.InvalidProductIdException;
import com.aritzia.availability.exception.ProductNotFoundException;
import com.aritzia.availability.model.Product;
import com.aritzia.availability.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductAvailabilityServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductIdValidator productIdValidator = new ProductIdValidator();
    private ProductAvailabilityService service;

    @BeforeEach
    void setUp() {
        service = new ProductAvailabilityService(productRepository, productIdValidator);
    }

    @Test
    void returnsInStockTrueWhenQuantityPositive() {
        Instant now = Instant.parse("2026-03-18T18:20:00Z");
        when(productRepository.findById("12345")).thenReturn(Optional.of(new Product("12345", 42, now)));

        AvailabilityResponse response = service.getAvailability("12345");

        assertThat(response.productId()).isEqualTo("12345");
        assertThat(response.inStock()).isTrue();
        assertThat(response.availableQuantity()).isEqualTo(42);
        assertThat(response.lastUpdated()).isEqualTo(now);
    }

    @Test
    void returnsInStockFalseWhenQuantityZero() {
        Instant now = Instant.parse("2026-03-18T18:20:00Z");
        when(productRepository.findById("10003")).thenReturn(Optional.of(new Product("10003", 0, now)));

        AvailabilityResponse response = service.getAvailability("10003");

        assertThat(response.inStock()).isFalse();
        assertThat(response.availableQuantity()).isEqualTo(0);
    }

    @Test
    void throwsNotFoundWhenProductMissing() {
        when(productRepository.findById("99999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAvailability("99999"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void throwsInvalidBeforeTouchingRepositoryForMalformedId() {
        assertThatThrownBy(() -> service.getAvailability("abc"))
                .isInstanceOf(InvalidProductIdException.class);

        verifyNoInteractions(productRepository);
    }

    @Test
    void throwsInvalidForNullId() {
        assertThatThrownBy(() -> service.getAvailability(null))
                .isInstanceOf(InvalidProductIdException.class);

        verifyNoInteractions(productRepository);
    }
}
