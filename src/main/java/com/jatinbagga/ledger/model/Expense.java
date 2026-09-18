package com.jatinbagga.ledger.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "expenses",
    // The common query is "this category, this month", so one composite index
    // serves it rather than two separate lookups.
    indexes = @Index(name = "idx_expense_category_date", columnList = "category,spent_on")
)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String description;

    // BigDecimal, never double. Binary floating point cannot represent 0.10
    // exactly, and money that does not add up is the whole problem here.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Category category;

    @Column(name = "spent_on", nullable = false)
    private LocalDate spentOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Expense() {
        // Required by JPA. Not for application code.
    }

    public Expense(String description, BigDecimal amount, Category category, LocalDate spentOn) {
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.spentOn = spentOn;
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public Category getCategory() { return category; }
    public LocalDate getSpentOn() { return spentOn; }
    public Instant getCreatedAt() { return createdAt; }

    public void setDescription(String description) { this.description = description; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCategory(Category category) { this.category = category; }
    public void setSpentOn(LocalDate spentOn) { this.spentOn = spentOn; }
}
