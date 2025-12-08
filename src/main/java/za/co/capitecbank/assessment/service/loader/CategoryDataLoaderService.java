package za.co.capitecbank.assessment.service.loader;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.entity.CategoryKeyword;
import za.co.capitecbank.assessment.domain.entity.TransactionCategory;
import za.co.capitecbank.assessment.repository.TransactionCategoryRepository;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@Order
public class CategoryDataLoaderService {

    private final TransactionCategoryRepository categoryRepository;
    private final ResourceLoader resourceLoader;
    private final CsvMapper csvMapper;
    private List<TransactionCategory> cachedCategories;
    private LocalDateTime lastCacheUpdate;
    private static final Duration CACHE_DURATION = Duration.ofMinutes(30);

    @Value("${app.category-data}")
    private String categoriesFile;

    public CategoryDataLoaderService(TransactionCategoryRepository categoryRepository,
                                     ResourceLoader resourceLoader) {
        this.categoryRepository = categoryRepository;
        this.resourceLoader = resourceLoader;
        this.csvMapper = new CsvMapper();
    }

    @PostConstruct
    public void initializeCategoriesFromCsv() {
        try {
            loadCategoriesFromCsv();
            refreshCache();
            logAllLoadedKeywords();
            log.info("Categories initialized successfully");
        } catch (Exception e) {
            log.error("Failed to load categories from CSV: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize categories", e);
        }
    }

    /**
     * Loads categories from CSV file using Jackson ObjectMapper.
     * CSV Format: category_name,display_name,requires_positive_amount,keywords
     * Example: FOOD,Food,false,"grocery,spar,supermarket,coffee"
     */
    public void loadCategoriesFromCsv() {
        try {
            Resource resource = resourceLoader.getResource(categoriesFile);

            if (!resource.exists()) {
                log.warn("CSV file not found at {}, skipping category load", categoriesFile);
                return;
            }

            log.info("Loading categories from CSV: {}", categoriesFile);

            CsvSchema schema = CsvSchema.emptySchema()
                    .withHeader()
                    .withColumnSeparator(',')
                    .withQuoteChar('"');

            try (InputStream is = resource.getInputStream()) {
                MappingIterator<CategoryCsvRow> iterator = csvMapper
                        .readerFor(CategoryCsvRow.class)
                        .with(schema)
                        .readValues(is);

                List<TransactionCategory> categories = iterator.readAll().stream()
                        .map(this::mapAndSaveCategory)
                        .filter(cat -> cat != null)
                        .collect(Collectors.toList());

                if (!categories.isEmpty()) {
                    log.info("Successfully loaded {} categories from CSV", categories.size());
                } else {
                    log.info("No categories found in {}", categoriesFile);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load categories from '{}': {}", categoriesFile, e.getMessage());
            throw new RuntimeException("Failed to load categories", e);
        }
    }

    private TransactionCategory mapAndSaveCategory(CategoryCsvRow row) {
        try {
            String categoryName = row.getCategoryName().trim();
            String displayName = row.getDisplayName().trim();
            boolean requiresPositiveAmount = row.isRequiresPositiveAmount();
            String keywordsStr = row.getKeywords() != null ? row.getKeywords().trim() : "";

            // Check if category already exists to prevent duplicates
            Optional<TransactionCategory> existingCategory = categoryRepository.findByName(categoryName);
            TransactionCategory category;

            if (existingCategory.isPresent()) {
                category = existingCategory.get();
                category.setDisplayName(displayName);
                category.setRequiresPositiveAmount(requiresPositiveAmount);
                category.getKeywords().clear();
                log.info("Updating existing category: {}", categoryName);
            } else {
                category = new TransactionCategory(categoryName, displayName, requiresPositiveAmount);
                log.info("Creating new category: {}", categoryName);
            }

            // Parse and add keywords
            if (!keywordsStr.isEmpty()) {
                Arrays.stream(keywordsStr.split(","))
                        .map(String::trim)
                        .filter(kw -> !kw.isEmpty())
                        .forEach(keyword -> {
                            CategoryKeyword kw = new CategoryKeyword(keyword);
                            kw.setCategory(category);
                            category.getKeywords().add(kw);
                        });
            }

            categoryRepository.save(category);

            // Enhanced logging with all keywords
            String keywordsList = category.getKeywords().stream()
                    .map(CategoryKeyword::getKeyword)
                    .collect(Collectors.joining(", "));

            log.info("✓ Saved category: {} (Display: {}) | Keywords [{}]: {}",
                    categoryName,
                    displayName,
                    category.getKeywords().size(),
                    keywordsList.isEmpty() ? "NONE" : keywordsList);

            return category;

        } catch (Exception e) {
            log.error("Error mapping category row: {}", row, e);
            return null;
        }
    }

    /**
     * Logs all loaded categories and their keywords in a formatted way
     */
    private void logAllLoadedKeywords() {
        log.info("=".repeat(80));
        log.info("LOADED CATEGORIES AND KEYWORDS SUMMARY");
        log.info("=".repeat(80));

        List<TransactionCategory> allCategories = categoryRepository.findAll();

        if (allCategories.isEmpty()) {
            log.warn("No categories found in database");
            return;
        }

        int totalKeywords = 0;

        for (TransactionCategory category : allCategories) {
            String keywordsList = category.getKeywords().stream()
                    .map(CategoryKeyword::getKeyword)
                    .sorted()
                    .collect(Collectors.joining(", "));

            totalKeywords += category.getKeywords().size();

            log.info("Category: {} ({})", category.getName(), category.getDisplayName());
            log.info("  - Requires Positive Amount: {}", category.isRequiresPositiveAmount());
            log.info("  - Keyword Count: {}", category.getKeywords().size());
            log.info("  - Keywords: {}", keywordsList.isEmpty() ? "NONE" : keywordsList);
            log.info("-".repeat(80));
        }

        log.info("TOTAL: {} categories with {} total keywords", allCategories.size(), totalKeywords);
        log.info("=".repeat(80));
    }

    public void refreshCache() {
        this.cachedCategories = categoryRepository.findAll();
        this.lastCacheUpdate = LocalDateTime.now();
        log.debug("Cache refreshed with {} categories", cachedCategories.size());
    }

    private List<TransactionCategory> getCategories() {
        // Refresh cache if stale
        if (cachedCategories == null || lastCacheUpdate == null ||
                Duration.between(lastCacheUpdate, LocalDateTime.now()).compareTo(CACHE_DURATION) > 0) {
            refreshCache();
        }
        return cachedCategories;
    }

    public TransactionCategory categorize(String description, BigDecimal amount) {
        if (description == null || description.isEmpty()) {
            return getCategoryByName("OTHER");
        }

        String lowerDesc = description.toLowerCase();
        log.info(lowerDesc);

        for (TransactionCategory category : getCategories()) {
            if (category.getName().equals("OTHER")) {
                continue; // Skip "Other" for now, use as fallback
            }

            // Check if any keyword matches
            boolean keywordMatch = category.getKeywords().stream()
                    .anyMatch(kw -> lowerDesc.contains(kw.getKeyword()));

            if (keywordMatch) {
                // If category requires positive amount, verify it
                if (category.isRequiresPositiveAmount()) {
                    if (amount.compareTo(BigDecimal.ZERO) > 0) {
                        log.debug("Matched category {} for description: {}", category.getName(), description);
                        return category;
                    }
                } else {
                    log.debug("Matched category {} for description: {}", category.getName(), description);
                    return category;
                }
            }
        }

        // Fallback to "Other"
        log.debug("No category match found, using OTHER for description: {}", description);
        return getCategoryByName("OTHER");
    }

    private TransactionCategory getCategoryByName(String name) {
        return getCategories().stream()
                .filter(cat -> cat.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Category not found: " + name));
    }

    /**
     * DTO for mapping CSV rows to objects
     */
    @Data
    public static class CategoryCsvRow {
        @com.fasterxml.jackson.annotation.JsonProperty("name")
        private String categoryName;

        @com.fasterxml.jackson.annotation.JsonProperty("display_name")
        private String displayName;

        @com.fasterxml.jackson.annotation.JsonProperty("requires_positive_amount")
        private boolean requiresPositiveAmount;

        @com.fasterxml.jackson.annotation.JsonProperty("keywords")
        private String keywords;
    }
}