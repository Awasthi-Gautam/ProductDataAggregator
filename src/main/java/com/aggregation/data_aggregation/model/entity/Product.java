package com.aggregation.data_aggregation.model.entity;

import com.aggregation.data_aggregation.model.enums.Platform;
import com.aggregation.data_aggregation.model.enums.ProductCategory;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "products", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "brand"})
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    private String brand;

    @Enumerated(EnumType.STRING)
    private ProductCategory category = ProductCategory.UNCATEGORIZED;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;

    // Platform-specific product IDs (e.g., ASIN for Amazon, SKU for Flipkart)
    @ElementCollection
    @CollectionTable(name = "product_external_ids", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "platform")
    @Column(name = "external_id")
    private Map<Platform, String> externalIds = new HashMap<>();

    private String country = "IN";

    private LocalDateTime firstSeenAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        firstSeenAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Map<Platform, String> getExternalIds() { return externalIds; }
    public void setExternalIds(Map<Platform, String> externalIds) { this.externalIds = externalIds; }

    public String getCountry() { return country; }

    public LocalDateTime getFirstSeenAt() { return firstSeenAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
