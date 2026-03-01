package com.aggregation.data_aggregation.dto;

public record TrendingProductDto(
    long rank,
    String productId,
    String name,
    String brand,
    String category,
    String imageUrl,

    /** Cumulative price-weighted score — how consistently popular this product is */
    double popularityScore,

    /**
     * Rate of change vs previous collection cycle.
     * Positive = rising, negative = falling, null = first time seen.
     * The /api/trending endpoint sorts by this field.
     */
    Double velocityScore
) {}
