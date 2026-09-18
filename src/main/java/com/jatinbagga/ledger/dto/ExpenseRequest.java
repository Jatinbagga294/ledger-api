package com.jatinbagga.ledger.dto;

import com.jatinbagga.ledger.model.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** What a client may send. Deliberately not the entity: no id, no createdAt. */
public record ExpenseRequest(

    @NotBlank(message = "description is required")
    @Size(max = 200, message = "description must be 200 characters or fewer")
    String description,

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "amount must have at most 2 decimal places")
    BigDecimal amount,

    @NotNull(message = "category is required")
    Category category,

    @NotNull(message = "spentOn is required")
    @PastOrPresent(message = "spentOn cannot be in the future")
    LocalDate spentOn
) {}
