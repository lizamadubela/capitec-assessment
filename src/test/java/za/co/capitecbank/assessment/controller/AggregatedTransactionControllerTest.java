package za.co.capitecbank.assessment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import za.co.capitecbank.assessment.service.AggregationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AggregatedTransactionControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AggregationService aggregationService;

    @InjectMocks
    private AggregatedTransactionController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    private za.co.capitecbank.assessment.domain.entity.AggregatedTransaction createMockEntityTransaction(
            Long id, String description, BigDecimal amount) {
        za.co.capitecbank.assessment.domain.entity.AggregatedTransaction transaction =
                new za.co.capitecbank.assessment.domain.entity.AggregatedTransaction();
        transaction.setId(id);
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setCategory("GROCERIES");
        transaction.setSource("POS");
        transaction.setTimestamp(LocalDateTime.of(2025, 1, 15, 10, 30));
        transaction.setCustomerId("CUST-1");
        return transaction;
    }

    private List<za.co.capitecbank.assessment.domain.entity.AggregatedTransaction> createMockEntityTransactionList() {
        List<za.co.capitecbank.assessment.domain.entity.AggregatedTransaction> transactions = new ArrayList<>();
        transactions.add(createMockEntityTransaction(1L, "Checkers", new BigDecimal("150.50")));
        transactions.add(createMockEntityTransaction(2L, "Spur", new BigDecimal("85.25")));
        transactions.add(createMockEntityTransaction(3L, "Shell Garage", new BigDecimal("600.00")));
        return transactions;
    }

    @Test
    void shouldReturnAllTransactionsForCustomer() throws Exception {
        String customerId = "CUST-1";
        List<za.co.capitecbank.assessment.domain.entity.AggregatedTransaction> mockEntityTransactions =
                createMockEntityTransactionList();

        when(aggregationService.getAllTransactions(customerId))
                .thenReturn(mockEntityTransactions);

        mockMvc.perform(get("/api/transactions/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));

        verify(aggregationService, times(1)).getAllTransactions(customerId);
    }

    @Test
    void shouldReturnCategoryTotals() throws Exception {
        String customerId = "CUST-1";
        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        categoryTotals.put("GROCERIES", new BigDecimal("500.00"));
        categoryTotals.put("ENTERTAINMENT", new BigDecimal("200.00"));

        when(aggregationService.getTotalsByCategory(customerId))
                .thenReturn(categoryTotals);

        mockMvc.perform(get("/api/transactions/{customerId}/categories", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.GROCERIES").value(500.00))
                .andExpect(jsonPath("$.ENTERTAINMENT").value(200.00));

        verify(aggregationService, times(1)).getTotalsByCategory(customerId);
    }

    @Test
    void shouldReturnTransactionsWithinDateRange() throws Exception {
        String customerId = "CUST-1";
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        List<za.co.capitecbank.assessment.domain.entity.AggregatedTransaction> mockEntityTransactions =
                createMockEntityTransactionList();

        when(aggregationService.getByDateRange(customerId, startDate, endDate))
                .thenReturn(mockEntityTransactions);

        mockMvc.perform(get("/api/transactions/{customerId}/range", customerId)
                        .param("start", "2025-01-01")
                        .param("end", "2025-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));

        verify(aggregationService, times(1))
                .getByDateRange(customerId, startDate, endDate);
    }

    @Test
    void shouldReturn400WhenDateFormatIsInvalid() throws Exception {
        String customerId = "CUST-1";

        mockMvc.perform(get("/api/transactions/{customerId}/range", customerId)
                        .param("start", "invalid-date")
                        .param("end", "2024-12-31"))
                .andExpect(status().isBadRequest());

        verify(aggregationService, never()).getByDateRange(any(), any(), any());
    }

    @Test
    void shouldReturnSingleTransaction() throws Exception {
        String customerId = "CUST-1";
        Long transactionId = 456L;
        za.co.capitecbank.assessment.domain.entity.AggregatedTransaction mockEntityTransaction =
                createMockEntityTransaction(transactionId, "Test Transaction", new BigDecimal("100.00"));

        when(aggregationService.getTransactionById(customerId, transactionId))
                .thenReturn(mockEntityTransaction);
        mockMvc.perform(get("/api/transactions/{customerId}/{transactionId}",
                        customerId, transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId))
                .andExpect(jsonPath("$.description").value("Test Transaction"))
                .andExpect(jsonPath("$.amount").value(100.00));

        verify(aggregationService, times(1))
                .getTransactionById(customerId, transactionId);
    }

    @Test
    void shouldReturnSourceTotals() throws Exception {
        String customerId = "CUST-1";
        Map<String, BigDecimal> sourceTotals = new HashMap<>();
        sourceTotals.put("ATM", new BigDecimal("1000.00"));
        sourceTotals.put("POS", new BigDecimal("750.00"));

        when(aggregationService.getTotalsBySource(customerId))
                .thenReturn(sourceTotals);
        mockMvc.perform(get("/api/transactions/{customerId}/sources", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ATM").value(1000.00))
                .andExpect(jsonPath("$.POS").value(750.00));

        verify(aggregationService, times(1)).getTotalsBySource(customerId);
    }

    @Test
    void shouldReturnMatchingTransactions() throws Exception {
        String customerId = "CUST-1";
        String searchParam = "grocery";
        List<za.co.capitecbank.assessment.domain.entity.AggregatedTransaction> mockEntityTransactions =
                List.of(createMockEntityTransaction(1L, "Spar", new BigDecimal("150.50")));

        when(aggregationService.searchTransactions(customerId, searchParam))
                .thenReturn(mockEntityTransactions);
        mockMvc.perform(post("/api/transactions/{customerId}/search", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchParameter\":\"grocery\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Spar"));

        verify(aggregationService, times(1))
                .searchTransactions(customerId, searchParam);
    }

    @Test
    void shouldHandleEmptySearchParameter() throws Exception {
        String customerId = "CUST-1";
        List<za.co.capitecbank.assessment.domain.entity.AggregatedTransaction> allEntityTransactions =
                createMockEntityTransactionList();

        when(aggregationService.searchTransactions(eq(customerId), isNull()))
                .thenReturn(allEntityTransactions);
        mockMvc.perform(post("/api/transactions/{customerId}/search", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"searchParameter\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(aggregationService, times(1))
                .searchTransactions(eq(customerId), isNull());
    }

    @Test
    void shouldReturnEmptyMapWhenNoTransactions() throws Exception {
        String customerId = "CUST-2";
        Map<String, BigDecimal> emptyMap = new HashMap<>();

        when(aggregationService.getTotalsByCategory(customerId))
                .thenReturn(emptyMap);
        mockMvc.perform(get("/api/transactions/{customerId}/categories", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(aggregationService, times(1)).getTotalsByCategory(customerId);
    }

}