package com.aggregation.data_aggregation.collector.enrichment;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.Region;
import com.aggregation.data_aggregation.model.entity.SourcePost;
import com.aggregation.data_aggregation.model.enums.Platform;
import com.aggregation.data_aggregation.model.enums.PostType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Enriches a product by searching Reddit's public JSON API for relevant discussions.
 *
 * Endpoint: https://www.reddit.com/search.json?q={query}&sort=relevance&limit=10&type=link
 * No authentication required — Reddit allows public read-only JSON access.
 * India-relevant subreddits are boosted via an extra "site:reddit.com r/india OR r/IndiaInvestments" scope.
 */
@Component
public class RedditCollector extends AbstractCollector {

    private static final String SEARCH_URL =
        "https://www.reddit.com/search.json?q=%s&sort=relevance&limit=10&type=link&restrict_sr=false";

    // Subreddits with high India engagement — included in search queries for better relevance
    private static final List<String> INDIA_SUBREDDITS = List.of(
        "india", "IndiaShopping", "indianonlinebuying", "IndiaInvestments",
        "AskIndia", "IndiaFashionCosmetics"
    );

    @Override
    public Platform getPlatform() { return Platform.REDDIT; }

    @Override
    public boolean supportsEnrichment() { return true; }

    @Override
    public CollectionResult enrich(String productName, String productId) {
        CollectionResult result = new CollectionResult(getPlatform());
        try {
            String query = buildQuery(productName);
            String url = String.format(SEARCH_URL, URLEncoder.encode(query, StandardCharsets.UTF_8));

            JsonNode root = fetchJson(url, "User-Agent", "data-aggregation-bot/1.0 (by /u/aggregator)");
            JsonNode posts = root.path("data").path("children");

            if (!posts.isArray()) {
                log.debug("Reddit: no results for '{}'", productName);
                return result;
            }

            for (JsonNode child : posts) {
                JsonNode data = child.path("data");

                String title   = data.path("title").asText("").trim();
                String selfUrl = "https://www.reddit.com" + data.path("permalink").asText("");
                String subreddit = data.path("subreddit").asText("");
                int    score   = data.path("score").asInt(0);
                int    numComments = data.path("num_comments").asInt(0);
                double redditRating = data.path("upvote_ratio").asDouble(0); // 0..1
                String selfText = data.path("selftext").asText("").trim();
                if (selfText.length() > 500) selfText = selfText.substring(0, 497) + "...";

                if (title.isBlank() || selfUrl.isBlank()) continue;

                PostType type = inferPostType(title, selfText);

                SourcePost post = new SourcePost();
                post.setProductId(productId);
                post.setPlatform(Platform.REDDIT);
                post.setType(type);
                post.setTitle(title);
                post.setContent(
                    "r/" + subreddit + " | Score: " + score + " | Comments: " + numComments
                    + (selfText.isBlank() ? "" : "\n" + selfText)
                );
                post.setUrl(selfUrl);
                post.setRegion(new Region()); // Reddit is global; no city/state
                post.setAuthorName(data.path("author").asText(null));
                if (redditRating > 0) post.setRating(redditRating * 5); // normalise to 0-5 scale

                result.addPost(post);
            }

            log.debug("Reddit enrichment for '{}': {} posts", productName, result.postCount());
        } catch (Exception e) {
            log.warn("Reddit enrichment failed for '{}': {}", productName, e.getMessage());
        }
        return result;
    }

    /** Build a search query that favours India-focused subreddits */
    private String buildQuery(String productName) {
        // Trim to a short name — take first pipe-separated segment, max 80 chars
        String shortName = productName.split("[|\\u2014]")[0].trim();
        if (shortName.length() > 80) shortName = shortName.substring(0, 80).trim();

        // e.g. "OnePlus 12 (subreddit:india OR subreddit:IndiaShopping OR subreddit:indianonlinebuying)"
        String subredditFilter = INDIA_SUBREDDITS.stream()
            .map(s -> "subreddit:" + s)
            .reduce((a, b) -> a + " OR " + b)
            .orElse("");
        return shortName + " (" + subredditFilter + ")";
    }

    private PostType inferPostType(String title, String body) {
        String lower = (title + " " + body).toLowerCase();
        if (lower.contains("review") || lower.contains("experience") || lower.contains("honest")) {
            return PostType.REVIEW;
        }
        if (lower.contains("problem") || lower.contains("issue") || lower.contains("defect")
            || lower.contains("broken") || lower.contains("complaint")) {
            return PostType.COMPLAINT;
        }
        if (lower.contains("?") || lower.contains("which") || lower.contains("recommend")
            || lower.contains("suggest")) {
            return PostType.QA;
        }
        return PostType.DISCUSSION;
    }
}
