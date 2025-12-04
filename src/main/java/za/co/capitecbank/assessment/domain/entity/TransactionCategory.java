package za.co.capitecbank.assessment.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "transaction_categories", schema = "data_aggregation")
public class TransactionCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "requires_positive_amount")
    private boolean requiresPositiveAmount = false;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<CategoryKeyword> keywords = new ArrayList<>();

    public TransactionCategory() {}

    public TransactionCategory(String name, String displayName, boolean requiresPositiveAmount) {
        this.name = name;
        this.displayName = displayName;
        this.requiresPositiveAmount = requiresPositiveAmount;
    }
}