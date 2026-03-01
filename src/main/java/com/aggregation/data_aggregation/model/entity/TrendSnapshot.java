package com.aggregation.data_aggregation.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persists a product's weighted trend score at the end of each scoring cycle.
 * Used to compute velocity (rate of change) in the next cycle:
 *   velocity = currentScore - previousCycleScore
 *
 * Snapshots older than 48 hours are purged automatically by the scoring job.
 */
@Entity
@Table(name = "trend_snapshots", indexes = {
    @Index(columnList = "productId"),
    @Index(columnList = "recordedAt")
})
public class TrendSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String productId;

    /** Price-weighted score computed in this cycle */
    @Column(nullable = false)
    private double score;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    protected TrendSnapshot() {}

    public TrendSnapshot(String productId, double score, LocalDateTime recordedAt) {
        this.productId = productId;
        this.score = score;
        this.recordedAt = recordedAt;
    }

    public String getId() { return id; }
    public String getProductId() { return productId; }
    public double getScore() { return score; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
