package za.co.capitecbank.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.capitecbank.assessment.domain.entity.RawTransaction;

public interface RawTransactionRepository extends JpaRepository<RawTransaction, Long> {
}
