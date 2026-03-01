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
 * Enriches products using Google Custom Search API.
 *
 * Searches Reddit, Quora, MouthShut, and Indian forums for product discussions
 * — without needing Reddit OAuth credentials. Works from any IP including AWS.
 *
 * Setup (5 minutes, 100 free searches/day):
 *   1. Go to https://programmablesearchengine.google.com → "Add"
 *   2. Name: "Product Discussion Search" (anything)
 *   3. Enable "Search the entire web" → gives you results from all sites
 *      (or restrict to reddit.com, quora.com, mouthshut.com for more targeted results)
 *   4. Copy the Search Engine ID (cx value) → GOOGLE_CSE_CX in .env
 *   5. Go to https://console.cloud.google.com → APIs & Services → Enable APIs
 *      → search "Custom Search API" → Enable
 *   6. Go to Credentials → Create API key → GOOGLE_CSE_API_KEY in .env
 *      (You can reuse the same Google Cloud project as YouTube — just enable
 *       the Custom Search API on it and generate one key for both services.)
 *
 * Free quota: 100 queries/day. Pay-as-you-go: $5 per 1,000 extra queries.
 * At 1 search per enriched product per cycle, 100/day is plenty for an MVP.
 */
@Component
public class GoogleDiscussionCollector extends AbstractCollector {

    private static final String SEARCH_URL =
        "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&num=10&q=%s";

    @Value("${app.google.cse-api-key:}")
    private String apiKey;

    @Value("${app.google.cse-cx:}")
    private String cx;

    @Override
    public Platform getPlatform() { return Platform.GOOGLE_CSE; }

    @Override
    public boolean supportsEnrichment() { return true; }

    @Override
    public CollectionResult enrich(String productName, String productId) {
        CollectionResult result = new CollectionResult(getPlatform());

        if (apiKey == null || apiKey.isBlank() || cx == null || cx.isBlank()) {
            log.debug("GoogleDiscussionCollector: api-key or cx not configured — skipping '{}'", productName);
            return result;
        }

        try {
            String shortName = shortName(productName);
            String query = URLEncoder.encode(shortName + " review India", StandardCharsets.UTF_8);
            String url = String.format(SEARCH_URL, apiKey, cx, query);

            JsonNode root = fetchJson(url);
            JsonNode items = root.path("items");

            if (!items.isArray()) {
                log.debug("GoogleCSE: no results for '{}'", productName);
                return result;
            }

            for (JsonNode item : items) {
                String title       = item.path("title").asText("").trim();
                String link        = item.path("link").asText("").trim();
                String snippet     = item.path("snippet").asText("").trim();
                String displayLink = item.path("displayLink").asText("");

                if (title.isBlank() || link.isBlank()) continue;

                PostType type = inferPostType(title, snippet);

                SourcePost post = new SourcePost();
                post.setProductId(productId);
                post.setPlatform(Platform.GOOGLE_CSE);
                post.setType(type);
                post.setTitle(title);
                post.setContent("[" + displayLink + "] " + snippet);
                post.setUrl(link);
                post.setRegion(new Region());

                result.addPost(post);
            }

            log.debug("Google CSE enrichment for '{}': {} results", productName, result.postCount());
        } catch (Exception e) {
            log.warn("Google CSE enrichment failed for '{}': {}", productName, e.getMessage());
        }
        return result;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** First pipe/em-dash segment, max 80 chars — keeps the search query short and relevant. */
    private String shortName(String productName) {
        String s = productName.split("[|\\u2014]")[0].trim();
        return s.length() > 80 ? s.substring(0, 80).trim() : s;
    }

    private PostType inferPostType(String title, String snippet) {
        String lower = (title + " " + snippet).toLowerCase();
        if (lower.contains("review") || lower.contains("experience") || lower.contains("honest"))
            return PostType.REVIEW;
        if (lower.contains("problem") || lower.contains("issue") || lower.contains("complaint")
                || lower.contains("defect") || lower.contains("broken"))
            return PostType.COMPLAINT;
        if (lower.contains("?") || lower.contains("which") || lower.contains("recommend")
                || lower.contains("suggest") || lower.contains("worth"))
            return PostType.QA;
        return PostType.DISCUSSION;
    }
}
