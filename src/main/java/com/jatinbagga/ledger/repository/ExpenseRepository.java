package com.jatinbagga.ledger.repository;

import com.jatinbagga.ledger.model.Category;
import com.jatinbagga.ledger.model.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByCategory(Category category, Pageable pageable);

    List<Expense> findBySpentOnBetweenOrderBySpentOnDesc(LocalDate from, LocalDate to);

    /**
     * Totals per category over a date range, summed in the database rather than
     * by loading every row and adding them up in Java.
     */
    @Query("""
           select e.category as category, sum(e.amount) as total, count(e) as count
           from Expense e
           where e.spentOn between :from and :to
           group by e.category
           order by sum(e.amount) desc
           """)
    List<CategoryTotal> totalsByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);

    interface CategoryTotal {
        Category getCategory();
        BigDecimal getTotal();
        long getCount();
    }
}
