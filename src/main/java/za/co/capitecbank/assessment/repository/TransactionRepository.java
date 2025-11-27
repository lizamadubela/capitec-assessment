package za.co.capitecbank.assessment.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.co.capitecbank.assessment.domain.entity.TransactionEntity;


import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    List<TransactionEntity> findByAccountIdOrderByTimestampDesc(String accountId);
    List<TransactionEntity> findByAccountIdAndTimestampBetweenOrderByTimestampDesc(String accountId, LocalDateTime start, LocalDateTime end);
}
