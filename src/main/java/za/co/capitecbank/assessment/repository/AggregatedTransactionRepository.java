package za.co.capitecbank.assessment.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.capitecbank.assessment.domain.entity.AggregatedTransaction;


import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface AggregatedTransactionRepository extends JpaRepository<AggregatedTransaction, Long> {
    List<AggregatedTransaction> findByCustomerIdOrderByTimestampDesc(String accountId);
    List<AggregatedTransaction> findByCustomerIdAndTimestampBetweenOrderByTimestampDesc(String accountId, LocalDateTime start, LocalDateTime end);
}
