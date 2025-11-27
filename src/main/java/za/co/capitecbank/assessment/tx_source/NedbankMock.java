package za.co.capitecbank.assessment.tx_source;

import org.springframework.stereotype.Component;
import za.co.capitecbank.assessment.domain.RawTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class NedbankMock implements TransactionSource {


    @Override
    public List<RawTransaction> fetchTransactions(String customerId) {
        return List.of(
                new RawTransaction(customerId, "Lotto Purchase", new BigDecimal("22.50"), LocalDateTime.now().minusHours(7)),
                new RawTransaction(customerId, "Caltex V00002358", new BigDecimal("1200.00"), LocalDateTime.now().minusDays(13))
        );
    }
}