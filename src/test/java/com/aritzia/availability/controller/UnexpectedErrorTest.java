package com.aritzia.availability.controller;

import com.aritzia.availability.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UnexpectedErrorTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private ProductRepository productRepository;

    @Test
    void returns500WithGenericMessageWhenDataSourceFails() {
        when(productRepository.findById(anyString())).thenThrow(new IllegalStateException("data source unavailable"));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/availability/10001", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).contains("An unexpected error occurred");
        assertThat(response.getBody()).doesNotContain("data source unavailable");
    }
}
