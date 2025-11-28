package za.co.capitecbank.assessment.domain.entity;

import jakarta.persistence.*;
import za.co.capitecbank.assessment.service.TxCategorizationEngineImpl;

@Entity
@Table(name = "category_keywords")
public class CategoryKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String keyword;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private TransactionCategory category;

    // Constructors, getters, setters
    public CategoryKeyword() {}

    public CategoryKeyword(String keyword) {
        this.keyword = keyword.toLowerCase();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword.toLowerCase(); }

    public TransactionCategory getCategory() { return category; }
    public void setCategory(TransactionCategory category) { this.category = category; }
}
