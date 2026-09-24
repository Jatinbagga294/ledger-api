package com.jatinbagga.ledger;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jatinbagga.ledger.repository.ExpenseRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private ExpenseRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private String body(String desc, String amount, String category, String date) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("description", desc);
        m.put("amount", amount);
        m.put("category", category);
        m.put("spentOn", date);
        return json.writeValueAsString(m);
    }

    private long create(String desc, String amount, String category, String date) throws Exception {
        String res = mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
                .content(body(desc, amount, category, date)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asLong();
    }

    @Test
    @DisplayName("creating an expense returns 201 and the saved body")
    void createsExpense() throws Exception {
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
                .content(body("Groceries at No Frills", "82.45", "GROCERIES", "2026-09-01")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.description", is("Groceries at No Frills")))
            .andExpect(jsonPath("$.amount", is(82.45)));
    }

    @Test
    @DisplayName("amounts keep exact decimal precision")
    void amountPrecisionIsExact() throws Exception {
        create("Coffee", "0.10", "DINING", "2026-09-01");
        create("Coffee", "0.20", "DINING", "2026-09-01");
        // 0.10 + 0.20 must be exactly 0.30. With double it would not be, which
        // is the entire reason the column is BigDecimal.
        mvc.perform(get("/api/expenses/summary")
                .param("from", "2026-09-01").param("to", "2026-09-30"))
            .andExpect(jsonPath("$[0].total", is(0.30)));
    }

    @Test
    @DisplayName("a negative amount is rejected with field-level detail")
    void rejectsNegativeAmount() throws Exception {
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
                .content(body("Refund", "-5.00", "OTHER", "2026-09-01")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.amount").exists());
    }

    @Test
    @DisplayName("a blank description is rejected")
    void rejectsBlankDescription() throws Exception {
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
                .content(body("", "10.00", "OTHER", "2026-09-01")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.description").exists());
    }

    @Test
    @DisplayName("a future date is rejected")
    void rejectsFutureDate() throws Exception {
        String future = LocalDate.now().plusDays(30).toString();
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON)
                .content(body("Time travel", "10.00", "OTHER", future)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.spentOn").exists());
    }

    @Test
    @DisplayName("an unknown id returns 404, not 500")
    void unknownIdIsNotFound() throws Exception {
        mvc.perform(get("/api/expenses/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("listing filters by category")
    void filtersByCategory() throws Exception {
        create("Bus pass", "156.00", "TRANSPORT", "2026-09-02");
        create("Bread", "4.50", "GROCERIES", "2026-09-03");
        mvc.perform(get("/api/expenses").param("category", "TRANSPORT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].description", is("Bus pass")));
    }

    @Test
    @DisplayName("summary groups totals by category, largest first")
    void summaryGroupsByCategory() throws Exception {
        create("Rent", "1800.00", "RENT", "2026-09-01");
        create("Bread", "4.50", "GROCERIES", "2026-09-02");
        create("Milk", "5.50", "GROCERIES", "2026-09-03");
        mvc.perform(get("/api/expenses/summary")
                .param("from", "2026-09-01").param("to", "2026-09-30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].category", is("RENT")))
            .andExpect(jsonPath("$[0].total", is(1800.00)))
            .andExpect(jsonPath("$[1].category", is("GROCERIES")))
            .andExpect(jsonPath("$[1].total", is(10.00)))
            .andExpect(jsonPath("$[1].count", is(2)));
    }

    @Test
    @DisplayName("summary excludes rows outside the date range")
    void summaryRespectsDateRange() throws Exception {
        create("August rent", "1800.00", "RENT", "2026-08-01");
        create("September bread", "4.50", "GROCERIES", "2026-09-02");
        mvc.perform(get("/api/expenses/summary")
                .param("from", "2026-09-01").param("to", "2026-09-30"))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].category", is("GROCERIES")));
    }

    @Test
    @DisplayName("updating changes the stored row")
    void updatesExpense() throws Exception {
        long id = create("Typo", "10.00", "OTHER", "2026-09-01");
        mvc.perform(put("/api/expenses/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(body("Corrected", "12.50", "DINING", "2026-09-01")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.description", is("Corrected")))
            .andExpect(jsonPath("$.amount", is(12.50)));
    }

    @Test
    @DisplayName("deleting returns 204 and the row is gone")
    void deletesExpense() throws Exception {
        long id = create("Mistake", "1.00", "OTHER", "2026-09-01");
        mvc.perform(delete("/api/expenses/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/expenses/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleting something that is not there returns 404")
    void deleteUnknownIsNotFound() throws Exception {
        mvc.perform(delete("/api/expenses/999999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("health endpoint responds")
    void health() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("ok")));
    }

    @Test
    @DisplayName("an existing expense can be fetched by id")
    void fetchesOneById() throws Exception {
        long id = create("Monthly pass", "156.00", "TRANSPORT", "2026-09-02");
        mvc.perform(get("/api/expenses/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is((int) id)))
            .andExpect(jsonPath("$.description", is("Monthly pass")));
    }

    @Test
    @DisplayName("the list is paginated, newest first")
    void paginatesNewestFirst() throws Exception {
        create("First", "10.00", "DINING", "2026-09-01");
        create("Second", "20.00", "DINING", "2026-09-02");
        create("Third", "30.00", "DINING", "2026-09-03");
        mvc.perform(get("/api/expenses").param("page", "0").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].description", is("Third")))
            .andExpect(jsonPath("$.totalElements", is(3)));
        mvc.perform(get("/api/expenses").param("page", "1").param("size", "2"))
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].description", is("First")));
    }
}
