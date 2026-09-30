package com.aritzia.availability.service;

import com.aritzia.availability.model.Product;
import com.aritzia.availability.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class ProductAvailabilityCachingTest {

    @Autowired
    private ProductAvailabilityService service;

    @MockBean
    private ProductRepository productRepository;

    @Test
    void repeatedRequestsForTheSameSkuHitTheRepositoryOnce() {
        when(productRepository.findById("10002"))
                .thenReturn(Optional.of(new Product("10002", 7, Instant.parse("2026-03-18T18:20:00Z"))));

        service.getAvailability("10002");
        service.getAvailability("10002");
        service.getAvailability(" 10002 ");

        verify(productRepository, times(1)).findById("10002");
    }
}
