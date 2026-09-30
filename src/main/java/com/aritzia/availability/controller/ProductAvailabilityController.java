package com.aritzia.availability.controller;

import com.aritzia.availability.dto.AvailabilityResponse;
import com.aritzia.availability.dto.ErrorResponse;
import com.aritzia.availability.service.ProductAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductAvailabilityController {

    private static final Logger log = LoggerFactory.getLogger(ProductAvailabilityController.class);

    private final ProductAvailabilityService productAvailabilityService;

    public ProductAvailabilityController(ProductAvailabilityService productAvailabilityService) {
        this.productAvailabilityService = productAvailabilityService;
    }

    @Operation(summary = "Get real-time availability for a product SKU")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability found",
                    content = @Content(schema = @Schema(implementation = AvailabilityResponse.class))),
            @ApiResponse(responseCode = "400", description = "productId is malformed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product does not exist",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/availability/{productId}")
    public ResponseEntity<AvailabilityResponse> getAvailability(@PathVariable String productId) {
        log.info("Received availability request for productId={}", productId);
        AvailabilityResponse response = productAvailabilityService.getAvailability(productId);
        return ResponseEntity.ok(response);
    }
}
