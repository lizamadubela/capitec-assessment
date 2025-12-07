package za.co.capitecbank.assessment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import za.co.capitecbank.assessment.api.model.AggregatedTransaction;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import za.co.capitecbank.assessment.mapper.AggregatedTransactionResponseMapper;
import za.co.capitecbank.assessment.service.AggregationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Transaction Aggregation", description = "Transaction aggregation operations")
@RequestMapping("/api/transactions")
public class AggregatedTransactionController {

    private final AggregationService aggregationService;
    public AggregatedTransactionController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }
    @GetMapping("/{customerId}")
    public List<AggregatedTransaction> getAll(@PathVariable String customerId) {
        return AggregatedTransactionResponseMapper.INSTANCE.mapTransactionsFromDB(aggregationService.getAllTransactions(customerId));
    }
    @GetMapping("/{customerId}/categories")
    public Map<String, BigDecimal> getTotals(@PathVariable String
                                                     customerId) {
        return aggregationService.getTotalsByCategory(customerId);
    }
    @GetMapping("/{customerId}/range")
    public List<AggregatedTransaction> getByRange(
            @PathVariable String customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate end) {
        return AggregatedTransactionResponseMapper.INSTANCE.mapTransactionsFromDB(aggregationService.getByDateRange(customerId, start, end));
    }

    @GetMapping("/{customerId}/{transactionId}")
    public AggregatedTransaction getTransaction(@PathVariable String customerId, @PathVariable Long transactionId) {
        return AggregatedTransactionResponseMapper.INSTANCE.mapTransactionFromDB(aggregationService.getTransactionById(customerId, transactionId));
    }
}
