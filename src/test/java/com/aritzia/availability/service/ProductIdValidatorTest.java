package com.aritzia.availability.service;

import com.aritzia.availability.exception.InvalidProductIdException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductIdValidatorTest {

    private final ProductIdValidator validator = new ProductIdValidator();

    @Test
    void acceptsNumericId() {
        assertThat(validator.validate("12345")).isEqualTo("12345");
    }

    @Test
    void trimsLeadingAndTrailingWhitespace() {
        assertThat(validator.validate("  12345  ")).isEqualTo("12345");
    }

    @Test
    void preservesLeadingZeros() {
        assertThat(validator.validate("00042")).isEqualTo("00042");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "abc", "123 456", "12.3", "-123", "+123", "12a45", "123\t456"})
    void rejectsInvalidProductIds(String invalid) {
        assertThatThrownBy(() -> validator.validate(invalid))
                .isInstanceOf(InvalidProductIdException.class);
    }

    @Test
    void trimsLeadingAndTrailingTabsToo() {
        assertThat(validator.validate("\t12345\t")).isEqualTo("12345");
    }

    @Test
    void rejectsExcessivelyLongNumericString() {
        String tooLong = "1".repeat(21);
        assertThatThrownBy(() -> validator.validate(tooLong))
                .isInstanceOf(InvalidProductIdException.class);
    }

    @Test
    void acceptsMaximumLengthNumericString() {
        String maxLength = "1".repeat(20);
        assertThat(validator.validate(maxLength)).isEqualTo(maxLength);
    }
}
