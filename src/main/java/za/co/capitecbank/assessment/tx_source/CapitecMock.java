//package za.co.capitecbank.assessment.tx_source;
//
//import za.co.capitecbank.assessment.domain.RawTransaction;
//import org.springframework.stereotype.Component;
//
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//
//
//@Component
//public class CapitecMock implements TransactionSource {
//
//
//    @Override
//    public List<RawTransaction> fetchTransactions(String customerId) {
//        return List.of(
//                new RawTransaction(customerId, "Starbucks Coffee", new BigDecimal("5.80"), LocalDateTime.now().minusHours(5)),
//                new RawTransaction(customerId, "Netflix Subscription", new BigDecimal("12.99"), LocalDateTime.now().minusDays(15))
//        );
//    }
//
//
//}
