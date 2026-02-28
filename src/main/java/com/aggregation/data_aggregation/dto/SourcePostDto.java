package com.aggregation.data_aggregation.dto;

import java.time.LocalDateTime;

public record SourcePostDto(
    String id,
    String platform,
    String type,
    String title,
    String content,
    String url,
    String authorName,
    Double rating,
    String regionState,
    String regionCity,
    LocalDateTime collectedAt
) {}
