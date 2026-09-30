package com.aritzia.availability.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAvailabilityControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void returns200WithAvailabilityForExistingInStockProduct() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/availability/10001"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"productId\":\"10001\"");
        assertThat(response.getBody()).contains("\"inStock\":true");
    }

    @Test
    void returns200WithInStockFalseForZeroQuantityProduct() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/availability/10003"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"inStock\":false");
        assertThat(response.getBody()).contains("\"availableQuantity\":0");
    }

    @Test
    void returns404ForWellFormedButNonExistentProductId() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/availability/99999999"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).contains("\"status\":404");
    }

    @Test
    void returns400ForNonNumericProductId() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/availability/abc123"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("\"status\":400");
    }

    @Test
    void returns400ForProductIdWithInternalSpace() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/availability/123%20456"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
}
