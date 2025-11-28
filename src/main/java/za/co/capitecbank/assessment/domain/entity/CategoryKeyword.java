package za.co.capitecbank.assessment.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import za.co.capitecbank.assessment.service.TxCategorizationEngineImpl;

@Entity
@Table(name = "category_keywords")
public class CategoryKeyword {
    @Getter
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(nullable = false)
    private String keyword;

    @Setter
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private TransactionCategory category;

    public CategoryKeyword() {}

    public CategoryKeyword(String keyword) {
        this.keyword = keyword.toLowerCase();
    }

}
