package com.aggregation.data_aggregation.collector.enrichment;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.Region;
import com.aggregation.data_aggregation.model.entity.SourcePost;
import com.aggregation.data_aggregation.model.enums.Platform;
import com.aggregation.data_aggregation.model.enums.PostType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Enriches products by searching YouTube for review and unboxing videos (regionCode=IN).
 *
 * Uses YouTube Data API v3 — requires a free API key.
 * Set YOUTUBE_API_KEY in your .env / application.yml.
 * Free quota: 10,000 units/day; each search costs 100 units → 100 searches/day free.
 *
 * Endpoint: GET https://www.googleapis.com/youtube/v3/search?part=snippet&q=...&regionCode=IN
 */
@Component
public class YouTubeCollector extends AbstractCollector {

    private static final String SEARCH_URL =
        "https://www.googleapis.com/youtube/v3/search"
        + "?part=snippet&type=video&regionCode=IN&relevanceLanguage=en"
        + "&maxResults=10&order=relevance&q=%s&key=%s";

    private static final String VIDEO_BASE_URL = "https://www.youtube.com/watch?v=";

    @Value("${app.youtube.api-key:}")
    private String apiKey;

    @Override
    public Platform getPlatform() { return Platform.YOUTUBE; }

    @Override
    public boolean supportsEnrichment() { return true; }

    @Override
    public CollectionResult enrich(String productName, String productId) {
        CollectionResult result = new CollectionResult(getPlatform());

        if (apiKey == null || apiKey.isBlank()) {
            log.debug("YouTubeCollector: api-key not configured — skipping enrichment for '{}'", productName);
            return result;
        }

        try {
            String query = URLEncoder.encode(productName + " review India", StandardCharsets.UTF_8);
            String url = String.format(SEARCH_URL, query, apiKey);

            JsonNode root = fetchJson(url);
            JsonNode items = root.path("items");

            if (!items.isArray()) {
                log.debug("YouTube: no results for '{}'", productName);
                return result;
            }

            for (JsonNode item : items) {
                JsonNode snippet = item.path("snippet");
                String videoId = item.path("id").path("videoId").asText("").trim();
                if (videoId.isBlank()) continue;

                String title       = snippet.path("title").asText("").trim();
                String description = snippet.path("description").asText("").trim();
                String channelName = snippet.path("channelTitle").asText(null);
                String publishedAt = snippet.path("publishedAt").asText(null); // ISO-8601

                if (description.length() > 400) description = description.substring(0, 397) + "...";

                PostType type = inferType(title, description);

                SourcePost post = new SourcePost();
                post.setProductId(productId);
                post.setPlatform(Platform.YOUTUBE);
                post.setType(type);
                post.setTitle(title);
                post.setContent(description);
                post.setUrl(VIDEO_BASE_URL + videoId);
                post.setRegion(new Region());
                post.setAuthorName(channelName);
                // publishedAt → postedAt handled if format is stable (skip for now to avoid parse complexity)

                result.addPost(post);
            }

            log.debug("YouTube enrichment for '{}': {} videos", productName, result.postCount());
        } catch (Exception e) {
            log.warn("YouTube enrichment failed for '{}': {}", productName, e.getMessage());
        }
        return result;
    }

    private PostType inferType(String title, String description) {
        String lower = (title + " " + description).toLowerCase();
        if (lower.contains("unboxing")) return PostType.MENTION;
        if (lower.contains("review")) return PostType.REVIEW;
        if (lower.contains("vs") || lower.contains("compare") || lower.contains("comparison")) return PostType.DISCUSSION;
        return PostType.MENTION;
    }
}
