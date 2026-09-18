package com.jatinbagga.ledger.dto;

import com.jatinbagga.ledger.model.Category;
import com.jatinbagga.ledger.model.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
    Long id,
    String description,
    BigDecimal amount,
    Category category,
    LocalDate spentOn
) {
    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(
            e.getId(), e.getDescription(), e.getAmount(), e.getCategory(), e.getSpentOn());
    }
}
