package za.co.capitecbank.assessment.domain.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transaction_categories")
public class TransactionCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "requires_positive_amount")
    private boolean requiresPositiveAmount = false;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<CategoryKeyword> keywords = new ArrayList<>();

    // Constructors, getters, setters
    public TransactionCategory() {}

    public TransactionCategory(String name, String displayName, boolean requiresPositiveAmount) {
        this.name = name;
        this.displayName = displayName;
        this.requiresPositiveAmount = requiresPositiveAmount;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isRequiresPositiveAmount() { return requiresPositiveAmount; }
    public void setRequiresPositiveAmount(boolean requiresPositiveAmount) {
        this.requiresPositiveAmount = requiresPositiveAmount;
    }

    public List<CategoryKeyword> getKeywords() { return keywords; }
    public void setKeywords(List<CategoryKeyword> keywords) { this.keywords = keywords; }
}

