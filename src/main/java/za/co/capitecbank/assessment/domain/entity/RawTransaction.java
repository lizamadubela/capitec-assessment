package za.co.capitecbank.assessment.domain.entity;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
public class RawTransaction {

    private Long id;
    private String customerId;
    private String description;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String source;

    public RawTransaction(String customerId, String description, BigDecimal amount, LocalDateTime timestamp, String source) {
        this.customerId = customerId;
        this.description = description;
        this.amount = amount;
        this.timestamp = timestamp;
        this.source = source;
    }
}