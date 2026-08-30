package ru.creditbank.credit.operations.credit.create.rest.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreditApplicationRequestValidationTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validRequest_hasNoViolations() {
        CreditApplicationRequest request = new CreditApplicationRequest(
                "Иванов Иван Иванович", BigDecimal.valueOf(50000), 12);

        Set<ConstraintViolation<CreditApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Ив", "Ivan123", "Ivanov_Ivan"})
    void invalidFullName_isRejected(String fullName) {
        CreditApplicationRequest request = new CreditApplicationRequest(
                fullName, BigDecimal.valueOf(50000), 12);

        Set<ConstraintViolation<CreditApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void nullRequestedAmount_isRejected() {
        CreditApplicationRequest request = new CreditApplicationRequest(
                "Иванов Иван Иванович", null, 12);

        Set<ConstraintViolation<CreditApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void nonPositiveRequestedAmount_isRejected() {
        CreditApplicationRequest request = new CreditApplicationRequest(
                "Иванов Иван Иванович", BigDecimal.ZERO, 12);

        Set<ConstraintViolation<CreditApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void termMonthsLessThanOne_isRejected() {
        CreditApplicationRequest request = new CreditApplicationRequest(
                "Иванов Иван Иванович", BigDecimal.valueOf(50000), 0);

        Set<ConstraintViolation<CreditApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void nullTermMonths_isRejected() {
        CreditApplicationRequest request = new CreditApplicationRequest(
                "Иванов Иван Иванович", BigDecimal.valueOf(50000), null);

        Set<ConstraintViolation<CreditApplicationRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}