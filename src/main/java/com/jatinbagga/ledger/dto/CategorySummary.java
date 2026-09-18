package com.jatinbagga.ledger.dto;

import com.jatinbagga.ledger.model.Category;
import java.math.BigDecimal;

public record CategorySummary(Category category, BigDecimal total, long count) {}
