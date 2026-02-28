package com.aggregation.data_aggregation.model.event;

import com.aggregation.data_aggregation.model.enums.Platform;
import com.aggregation.data_aggregation.model.enums.PostType;

import java.time.Instant;

public record RawEvent(
    Platform source,
    PostType type,
    String rawData,      // JSON payload from the source
    String country,
    String state,
    String city,
    Instant collectedAt
) {
    public static RawEvent of(Platform source, PostType type, String rawData) {
        return new RawEvent(source, type, rawData, "IN", null, null, Instant.now());
    }

    public static RawEvent of(Platform source, PostType type, String rawData, String state, String city) {
        return new RawEvent(source, type, rawData, "IN", state, city, Instant.now());
    }
}
