package com.aggregation.data_aggregation.model.entity;

import com.aggregation.data_aggregation.model.Region;
import com.aggregation.data_aggregation.model.enums.ProductCategory;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trend_scores", indexes = {
    @Index(columnList = "productId, periodStart"),
    @Index(columnList = "region_country, region_state, region_city")
})
public class TrendScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String productId;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    // Composite score: volume × recency weight × sentiment
    private double score;

    private int mentionCount;
    private int reviewCount;

    // -1.0 (very negative) to 1.0 (very positive)
    private double sentimentScore;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "country", column = @Column(name = "region_country")),
        @AttributeOverride(name = "state",   column = @Column(name = "region_state")),
        @AttributeOverride(name = "city",    column = @Column(name = "region_city"))
    })
    private Region region = new Region();

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public int getMentionCount() { return mentionCount; }
    public void setMentionCount(int mentionCount) { this.mentionCount = mentionCount; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(double sentimentScore) { this.sentimentScore = sentimentScore; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }

    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
}
