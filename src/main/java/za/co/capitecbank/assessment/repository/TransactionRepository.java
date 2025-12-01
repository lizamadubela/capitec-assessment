package za.co.capitecbank.assessment.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.capitecbank.assessment.domain.entity.Transaction;


import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCustomerIdOrderByTimestampDesc(String accountId);
    List<Transaction> findByCustomerIdAndTimestampBetweenOrderByTimestampDesc(String accountId, LocalDateTime start, LocalDateTime end);
}
