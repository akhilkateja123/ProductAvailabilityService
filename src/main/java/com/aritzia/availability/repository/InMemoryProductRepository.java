package com.aritzia.availability.repository;

import com.aritzia.availability.model.Product;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public InMemoryProductRepository() {
        seed();
    }

    @Override
    public Optional<Product> findById(String productId) {
        return Optional.ofNullable(products.get(productId));
    }

    private void seed() {
        Instant now = Instant.parse("2026-03-18T18:20:00Z");
        put("10001", 42, now);
        put("10002", 7, now);
        put("10003", 0, now);
        put("10004", 15, now);
        put("10005", 3, now);
        put("10006", 0, now);
        put("10007", 120, now);
        put("10008", 1, now);
        put("10009", 58, now);
        put("10010", 9, now);
    }

    private void put(String productId, int quantity, Instant lastUpdated) {
        products.put(productId, new Product(productId, quantity, lastUpdated));
    }
}
