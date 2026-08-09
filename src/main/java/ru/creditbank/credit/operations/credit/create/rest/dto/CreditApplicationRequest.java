package ru.creditbank.credit.operations.credit.create.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreditApplicationRequest(

        @NotBlank(message = "ФИО обязательно для заполнения")
        @Size(min = 5, max = 100, message = "ФИО должно содержать от 5 до 100 символов")
        @Pattern(regexp = "^[А-Яа-яЁёA-Za-z\\s-]+$", message = "ФИО может содержать только буквы, пробелы и дефис")
        String fullName,

        @NotNull(message = "Сумма кредита обязательна для заполнения")
        @DecimalMin(value = "0.01", message = "Сумма кредита должна быть положительной")
        BigDecimal requestedAmount,

        @NotNull(message = "Срок кредита обязателен для заполнения")
        @Min(value = 1, message = "Срок кредита должен быть не менее 1 месяца")
        Integer termMonths
) {
}