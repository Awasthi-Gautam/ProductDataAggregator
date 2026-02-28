package com.aggregation.data_aggregation.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ProductDto(
    String id,
    String name,
    String brand,
    String category,
    String imageUrl,
    Map<String, String> externalIds,  // platform → id (serialised as string keys for JSON)
    Double trendScore,
    LocalDateTime firstSeenAt,
    LocalDateTime updatedAt
) {}
