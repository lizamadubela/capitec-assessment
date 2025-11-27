package za.co.capitecbank.assessment.tx_source;

import za.co.capitecbank.assessment.domain.RawTransaction;
import org.springframework.stereotype.Component;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Component
public class AbsaMock implements TransactionSource {


    @Override
    public List<RawTransaction> fetchTransactions(String customerId) {
        //TODO  static mock data, need to call an external api
        return List.of(
                new RawTransaction(customerId, "Grocery Store - SPAR", new BigDecimal("120.55"), LocalDateTime.now().minusDays(2)),
                new RawTransaction(customerId, "Uber BV - Ride", new BigDecimal("47.30"), LocalDateTime.now().minusDays(1)),
                new RawTransaction(customerId, "Salary", new BigDecimal("3500.00"), LocalDateTime.now().minusDays(10))
        );
    }
}
