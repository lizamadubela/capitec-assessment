package za.co.capitecbank.assessment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import za.co.capitecbank.assessment.controller.AggregatedTransactionController;
import za.co.capitecbank.assessment.domain.Transaction;
import za.co.capitecbank.assessment.service.AggregationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private AggregationService aggregationService;

    @InjectMocks
    private AggregatedTransactionController aggregatedTransactionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aggregatedTransactionController).build();
    }

    @Test
    void getAll_ShouldReturnAllTransactions_WhenCustomerIdProvided() throws Exception {
        String customerId = "CUST123";
        Transaction tx1 = createTransaction("TX1", "Groceries", new BigDecimal("150.00"));
        Transaction tx2 = createTransaction("TX2", "Transport", new BigDecimal("50.00"));
        List<Transaction> transactions = Arrays.asList(tx1, tx2);

        when(aggregationService.getAllTransactions(customerId)).thenReturn(transactions);

        mockMvc.perform(get("/api/transactions/{customerId}", customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].transactionId").value("TX1"))
                .andExpect(jsonPath("$[0].category").value("Groceries"))
                .andExpect(jsonPath("$[1].transactionId").value("TX2"))
                .andExpect(jsonPath("$[1].category").value("Transport"));

        verify(aggregationService).getAllTransactions(customerId);
    }

    @Test
    void getAll_ShouldReturnEmptyList_WhenNoTransactionsExist() throws Exception {
        String customerId = "CUST456";
        when(aggregationService.getAllTransactions(customerId)).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions/{customerId}", customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(aggregationService).getAllTransactions(customerId);
    }

    @Test
    void getTotals_ShouldReturnCategoryTotals_WhenCustomerIdProvided() throws Exception {
        String customerId = "CUST123";
        Map<String, BigDecimal> totals = new HashMap<>();
        totals.put("Groceries", new BigDecimal("350.50"));
        totals.put("Transport", new BigDecimal("120.00"));
        totals.put("Entertainment", new BigDecimal("75.25"));

        when(aggregationService.getTotalsByCategory(customerId)).thenReturn(totals);

        mockMvc.perform(get("/api/transactions/{customerId}/categories", customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Groceries").value(350.50))
                .andExpect(jsonPath("$.Transport").value(120.00))
                .andExpect(jsonPath("$.Entertainment").value(75.25));

        verify(aggregationService).getTotalsByCategory(customerId);
    }

    @Test
    void getTotals_ShouldReturnEmptyMap_WhenNoTransactionsExist() throws Exception {

        String customerId = "CUST789";
        when(aggregationService.getTotalsByCategory(customerId)).thenReturn(Map.of());

        mockMvc.perform(get("/api/transactions/{customerId}/categories", customerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());

        verify(aggregationService).getTotalsByCategory(customerId);
    }

    @Test
    void getByRange_ShouldReturnTransactions_WhenValidDateRangeProvided() throws Exception {
        String customerId = "CUST123";
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        Transaction tx1 = createTransaction("TX1", "Groceries", new BigDecimal("100.00"));
        List<Transaction> transactions = List.of(tx1);

        when(aggregationService.getByDateRange(customerId, start, end)).thenReturn(transactions);

        mockMvc.perform(get("/api/transactions/{customerId}/range", customerId)
                        .param("start", "2024-01-01")
                        .param("end", "2024-01-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionId").value("TX1"));

        verify(aggregationService).getByDateRange(customerId, start, end);
    }

    @Test
    void getByRange_ShouldReturnEmptyList_WhenNoTransactionsInRange() throws Exception {
        String customerId = "CUST123";
        LocalDate start = LocalDate.of(2024, 6, 1);
        LocalDate end = LocalDate.of(2024, 6, 30);

        when(aggregationService.getByDateRange(eq(customerId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/transactions/{customerId}/range", customerId)
                        .param("start", "2024-06-01")
                        .param("end", "2024-06-30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(aggregationService).getByDateRange(any(String.class), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void getByRange_ShouldReturn400_WhenStartDateMissing() throws Exception {
        mockMvc.perform(get("/api/transactions/{customerId}/range", "CUST123")
                        .param("end", "2024-01-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByRange_ShouldReturn400_WhenEndDateMissing() throws Exception {
        mockMvc.perform(get("/api/transactions/{customerId}/range", "CUST123")
                        .param("start", "2024-01-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByRange_ShouldReturn400_WhenInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/transactions/{customerId}/range", "CUST123")
                        .param("start", "01-01-2024")
                        .param("end", "31-01-2024")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // Helper method to create Transaction objects for testing
    private Transaction createTransaction(String id, String category, BigDecimal amount) {
        return new Transaction(
                id,
                "CUST123",
                amount,
                LocalDateTime.now(),
                "Test transaction",
                category,
                "TEST_SOURCE"
        );
    }
}