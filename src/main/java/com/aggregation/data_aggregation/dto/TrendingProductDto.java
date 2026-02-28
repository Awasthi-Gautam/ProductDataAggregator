package com.aggregation.data_aggregation.dto;

public record TrendingProductDto(
    long rank,
    String productId,
    String name,
    String brand,
    String category,
    String imageUrl,
    double trendScore
) {}
