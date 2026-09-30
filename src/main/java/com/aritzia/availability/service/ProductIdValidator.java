package com.aritzia.availability.service;

import com.aritzia.availability.exception.InvalidProductIdException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ProductIdValidator {

    private static final Pattern VALID_PRODUCT_ID = Pattern.compile("^[0-9]{1,20}$");

    public String validate(String rawProductId) {
        if (rawProductId == null) {
            throw new InvalidProductIdException("productId must not be null");
        }
        String trimmed = rawProductId.trim();
        if (!VALID_PRODUCT_ID.matcher(trimmed).matches()) {
            throw new InvalidProductIdException(
                    "productId must be 1-20 numeric digits with no spaces; got: '" + rawProductId + "'");
        }
        return trimmed;
    }
}
