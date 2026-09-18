package com.jatinbagga.ledger.controller;

import com.jatinbagga.ledger.dto.CategorySummary;
import com.jatinbagga.ledger.dto.ExpenseRequest;
import com.jatinbagga.ledger.dto.ExpenseResponse;
import com.jatinbagga.ledger.exception.NotFoundException;
import com.jatinbagga.ledger.model.Category;
import com.jatinbagga.ledger.model.Expense;
import com.jatinbagga.ledger.repository.ExpenseRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseRepository repository;

    public ExpenseController(ExpenseRepository repository) {
        // Constructor injection, not @Autowired on a field: the dependency is
        // required, so it belongs in the constructor where it cannot be null.
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request) {
        Expense saved = repository.save(new Expense(
            request.description(), request.amount(), request.category(), request.spentOn()));
        return ExpenseResponse.from(saved);
    }

    @GetMapping
    public Page<ExpenseResponse> list(
        @RequestParam(required = false) Category category,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, Math.min(size, 100),
            Sort.by(Sort.Direction.DESC, "spentOn"));
        var found = (category == null)
            ? repository.findAll(pageable)
            : repository.findByCategory(category, pageable);
        return found.map(ExpenseResponse::from);
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(@PathVariable Long id) {
        return repository.findById(id)
            .map(ExpenseResponse::from)
            .orElseThrow(() -> new NotFoundException("No expense with id " + id));
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        Expense existing = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("No expense with id " + id));
        existing.setDescription(request.description());
        existing.setAmount(request.amount());
        existing.setCategory(request.category());
        existing.setSpentOn(request.spentOn());
        return ExpenseResponse.from(repository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("No expense with id " + id);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public List<CategorySummary> summary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return repository.totalsByCategory(from, to).stream()
            .map(t -> new CategorySummary(t.getCategory(), t.getTotal(), t.getCount()))
            .toList();
    }
}
