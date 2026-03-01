package com.aggregation.data_aggregation.collector.enrichment;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.Region;
import com.aggregation.data_aggregation.model.entity.SourcePost;
import com.aggregation.data_aggregation.model.enums.Platform;
import com.aggregation.data_aggregation.model.enums.PostType;
import com.aggregation.data_aggregation.service.RedditAuthService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Enriches products by searching Reddit for India-relevant discussions.
 *
 * Two modes (automatic fallback):
 *
 *   OAuth mode  — used when REDDIT_CLIENT_ID + REDDIT_CLIENT_SECRET are set.
 *                 Hits oauth.reddit.com with a Bearer token.
 *                 Rate limit: 60 req/min. Works reliably from AWS IPs.
 *
 *   Public mode — used when credentials are not configured.
 *                 Hits www.reddit.com/search.json (no auth).
 *                 Rate limit: ~10 req/min. May be blocked from cloud IPs.
 *
 * To enable OAuth, register a free "script" app at reddit.com/prefs/apps
 * and set REDDIT_CLIENT_ID, REDDIT_CLIENT_SECRET, REDDIT_USERNAME in .env.
 */
@Component
public class RedditCollector extends AbstractCollector {

    // OAuth endpoint — higher limits, works from any IP including AWS
    private static final String OAUTH_SEARCH_URL =
        "https://oauth.reddit.com/search.json?q=%s&sort=relevance&limit=10&type=link&restrict_sr=false";

    // Public fallback — no auth needed, lower limits
    private static final String PUBLIC_SEARCH_URL =
        "https://www.reddit.com/search.json?q=%s&sort=relevance&limit=10&type=link&restrict_sr=false";

    private static final List<String> INDIA_SUBREDDITS = List.of(
        "india", "IndiaShopping", "indianonlinebuying", "IndiaInvestments",
        "AskIndia", "IndiaFashionCosmetics"
    );

    private final RedditAuthService authService;

    public RedditCollector(RedditAuthService authService) {
        this.authService = authService;
    }

    @Override
    public Platform getPlatform() { return Platform.REDDIT; }

    @Override
    public boolean supportsEnrichment() { return true; }

    @Override
    public CollectionResult enrich(String productName, String productId) {
        CollectionResult result = new CollectionResult(getPlatform());
        try {
            String encodedQuery = URLEncoder.encode(buildQuery(productName), StandardCharsets.UTF_8);
            JsonNode root = fetchReddit(encodedQuery);
            JsonNode posts = root.path("data").path("children");

            if (!posts.isArray()) {
                log.debug("Reddit: no results for '{}'", productName);
                return result;
            }

            for (JsonNode child : posts) {
                JsonNode data = child.path("data");

                String title      = data.path("title").asText("").trim();
                String selfUrl    = "https://www.reddit.com" + data.path("permalink").asText("");
                String subreddit  = data.path("subreddit").asText("");
                int score         = data.path("score").asInt(0);
                int numComments   = data.path("num_comments").asInt(0);
                double upvoteRatio = data.path("upvote_ratio").asDouble(0); // 0..1
                String selfText   = data.path("selftext").asText("").trim();
                if (selfText.length() > 500) selfText = selfText.substring(0, 497) + "...";

                if (title.isBlank()) continue;

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
                post.setRegion(new Region());
                post.setAuthorName(data.path("author").asText(null));
                if (upvoteRatio > 0) post.setRating(upvoteRatio * 5); // 0..1 → 0..5

                result.addPost(post);
            }

            log.debug("Reddit enrichment for '{}': {} posts ({})",
                productName, result.postCount(),
                authService.isConfigured() ? "OAuth" : "public");
        } catch (Exception e) {
            log.warn("Reddit enrichment failed for '{}': {}", productName, e.getMessage());
        }
        return result;
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private JsonNode fetchReddit(String encodedQuery) throws Exception {
        if (authService.isConfigured()) {
            String token = authService.getAccessToken();
            String url = String.format(OAUTH_SEARCH_URL, encodedQuery);
            return fetchJson(url,
                "Authorization", "Bearer " + token,
                "User-Agent", authService.userAgent());
        } else {
            log.debug("Reddit OAuth not configured — using public API (may be rate-limited on AWS)");
            String url = String.format(PUBLIC_SEARCH_URL, encodedQuery);
            return fetchJson(url, "User-Agent", authService.userAgent());
        }
    }

    private String buildQuery(String productName) {
        // Take only the first pipe-separated segment, max 80 chars
        String shortName = productName.split("[|\\u2014]")[0].trim();
        if (shortName.length() > 80) shortName = shortName.substring(0, 80).trim();

        String subredditFilter = INDIA_SUBREDDITS.stream()
            .map(s -> "subreddit:" + s)
            .reduce((a, b) -> a + " OR " + b)
            .orElse("");
        return shortName + " (" + subredditFilter + ")";
    }

    private PostType inferPostType(String title, String body) {
        String lower = (title + " " + body).toLowerCase();
        if (lower.contains("review") || lower.contains("experience") || lower.contains("honest"))
            return PostType.REVIEW;
        if (lower.contains("problem") || lower.contains("issue") || lower.contains("defect")
                || lower.contains("broken") || lower.contains("complaint"))
            return PostType.COMPLAINT;
        if (lower.contains("?") || lower.contains("which") || lower.contains("recommend")
                || lower.contains("suggest"))
            return PostType.QA;
        return PostType.DISCUSSION;
    }
}
