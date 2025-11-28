package za.co.capitecbank.assessment.service;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import za.co.capitecbank.assessment.domain.entity.CategoryKeyword;
import za.co.capitecbank.assessment.domain.entity.TransactionCategory;
import za.co.capitecbank.assessment.repository.TransactionCategoryRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CategoryService {

    private final TransactionCategoryRepository categoryRepository;
    private final ResourceLoader resourceLoader;
    private List<TransactionCategory> cachedCategories;
    private LocalDateTime lastCacheUpdate;
    private static final Duration CACHE_DURATION = Duration.ofMinutes(30);

    @Value("${category.csv.path:classpath:categories.csv}")
    private String csvFilePath;

    public CategoryService(TransactionCategoryRepository categoryRepository,
                           ResourceLoader resourceLoader) {
        this.categoryRepository = categoryRepository;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initializeCategoriesFromCsv() {
        try {
            loadCategoriesFromCsv();
            refreshCache();
        } catch (Exception e) {
            log.error("Failed to load categories from CSV: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize categories", e);
        }
    }

    /**
     * Loads categories from CSV file.
     * CSV Format: category_name,display_name,requires_positive_amount,keywords
     * Example: FOOD,Food,false,"grocery,spar,supermarket,coffee"
     */
    public void loadCategoriesFromCsv() {
        try {
            Resource resource = resourceLoader.getResource(csvFilePath);

            if (!resource.exists()) {
                log.warn("CSV file not found at {}, skipping category load", csvFilePath);
                return;
            }

            log.info("Loading categories from CSV: {}", csvFilePath);

            try (var is = resource.getInputStream();
                 var reader = new BufferedReader(new InputStreamReader(is))) {

                // Expected CSV header: category_name,display_name,requires_positive_amount,keywords
                var categories = reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .filter(line -> !line.startsWith("#"))
                        .skip(1) // skip header
                        .map(this::parseCsvLineAndSave)
                        .filter(cat -> cat != null)
                        .collect(Collectors.toList());

                if (!categories.isEmpty()) {
                    log.info("Successfully loaded {} categories from CSV", categories.size());
                } else {
                    log.info("No categories found in {}", csvFilePath);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load categories from '{}': {}", csvFilePath, e.getMessage());
            throw new RuntimeException("Failed to load categories", e);
        }
    }

    private TransactionCategory parseCsvLineAndSave(String line) {
        try {
            // Parse CSV line (handles quotes and commas within quotes)
            String[] parts = parseCsvLine(line);

            if (parts.length < 4) {
                log.warn("Invalid CSV line (expected 4 columns): {}", line);
                return null;
            }

            String categoryName = parts[0].trim();
            String displayName = parts[1].trim();
            boolean requiresPositiveAmount = Boolean.parseBoolean(parts[2].trim());
            String keywordsStr = parts[3].trim();

            // Check if category already exists
            Optional<TransactionCategory> existingCategory = categoryRepository.findByName(categoryName);
            TransactionCategory category;

            if (existingCategory.isPresent()) {
                category = existingCategory.get();
                category.setDisplayName(displayName);
                category.setRequiresPositiveAmount(requiresPositiveAmount);
                category.getKeywords().clear();
            } else {
                category = new TransactionCategory(categoryName, displayName, requiresPositiveAmount);
            }

            // Parse and add keywords
            if (!keywordsStr.isEmpty()) {
                String[] keywords = keywordsStr.split(",");
                for (String keyword : keywords) {
                    String trimmedKeyword = keyword.trim();
                    if (!trimmedKeyword.isEmpty()) {
                        CategoryKeyword kw = new CategoryKeyword(trimmedKeyword);
                        kw.setCategory(category);
                        category.getKeywords().add(kw);
                    }
                }
            }

            categoryRepository.save(category);
            log.debug("Saved category: {} with {} keywords", categoryName, category.getKeywords().size());

            return category;

        } catch (Exception e) {
            log.error("Error parsing CSV line: {}", line, e);
            return null;
        }
    }

    /**
     * Simple CSV parser that handles quoted fields
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        result.add(currentField.toString());
        return result.toArray(new String[0]);
    }

    public void refreshCache() {
        this.cachedCategories = categoryRepository.findAll();
        this.lastCacheUpdate = LocalDateTime.now();
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
                        return category;
                    }
                } else {
                    return category;
                }
            }
        }

        // Fallback to "Other"
        return getCategoryByName("OTHER");
    }

    private TransactionCategory getCategoryByName(String name) {
        return getCategories().stream()
                .filter(cat -> cat.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Category not found: " + name));
    }

    // Admin methods for managing categories
    public TransactionCategory addCategory(String name, String displayName, boolean requiresPositiveAmount) {
        TransactionCategory category = new TransactionCategory(name, displayName, requiresPositiveAmount);
        return categoryRepository.save(category);
    }

    public void addKeywordToCategory(Long categoryId, String keyword) {
        TransactionCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        CategoryKeyword kw = new CategoryKeyword(keyword);
        kw.setCategory(category);
        category.getKeywords().add(kw);

        categoryRepository.save(category);
        refreshCache();
    }

    public void removeKeywordFromCategory(Long categoryId, String keyword) {
        TransactionCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        category.getKeywords().removeIf(kw -> kw.getKeyword().equalsIgnoreCase(keyword));
        categoryRepository.save(category);
        refreshCache();
    }
}
