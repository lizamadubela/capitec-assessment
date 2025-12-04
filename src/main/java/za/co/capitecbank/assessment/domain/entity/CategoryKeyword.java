package za.co.capitecbank.assessment.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "category_keywords", schema = "data_aggregation")
public class CategoryKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String keyword;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private TransactionCategory category;

    public CategoryKeyword() {}

    public CategoryKeyword(String keyword) {
        this.keyword = keyword.toLowerCase();
    }
}