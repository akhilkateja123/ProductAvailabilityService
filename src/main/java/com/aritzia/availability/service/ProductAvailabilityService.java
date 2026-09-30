package com.aritzia.availability.service;

import com.aritzia.availability.dto.AvailabilityResponse;
import com.aritzia.availability.exception.ProductNotFoundException;
import com.aritzia.availability.model.Product;
import com.aritzia.availability.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProductAvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(ProductAvailabilityService.class);

    private final ProductRepository productRepository;
    private final ProductIdValidator productIdValidator;

    public ProductAvailabilityService(ProductRepository productRepository, ProductIdValidator productIdValidator) {
        this.productRepository = productRepository;
        this.productIdValidator = productIdValidator;
    }

    // Key on the validated ID so " 10001" and "10001" share one entry; invalid IDs throw before caching.
    @Cacheable(cacheNames = "productAvailability", key = "@productIdValidator.validate(#rawProductId)")
    public AvailabilityResponse getAvailability(String rawProductId) {
        String productId = productIdValidator.validate(rawProductId);

        log.info("Fetching availability for productId={}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return AvailabilityResponse.from(product);
    }
}
