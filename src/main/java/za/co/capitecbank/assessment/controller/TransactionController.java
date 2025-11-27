package za.co.capitecbank.assessment.controller;

;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.capitecbank.assessment.domain.Transaction;
import za.co.capitecbank.assessment.service.AggregationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final AggregationService aggregationService;
    public TransactionController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }
    @GetMapping("/{customerId}")
    public List<Transaction> getAll(@PathVariable String customerId) {
        return aggregationService.getAllTransactions(customerId);
    }
    @GetMapping("/{customerId}/categories")
    public Map<String, BigDecimal> getTotals(@PathVariable String
                                                     customerId) {
        return aggregationService.getTotalsByCategory(customerId);
    }
    @GetMapping("/{customerId}/range")
    public List<Transaction> getByRange(
            @PathVariable String customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate end) {
        return aggregationService.getByDateRange(customerId, start, end);
    }
}
