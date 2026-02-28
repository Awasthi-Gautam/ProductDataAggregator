package com.aggregation.data_aggregation.collector.discovery;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.Region;
import com.aggregation.data_aggregation.model.entity.Product;
import com.aggregation.data_aggregation.model.entity.SourcePost;
import com.aggregation.data_aggregation.model.enums.Platform;
import com.aggregation.data_aggregation.model.enums.PostType;
import com.aggregation.data_aggregation.model.enums.ProductCategory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fetches Google Trends daily trending searches for India.
 *
 * Endpoint: https://trends.google.com/trends/api/dailytrends?hl=en-IN&tz=-330&geo=IN&ns=15
 * The response is prefixed with ")]}',\n" (XSSI guard) which must be stripped before parsing.
 *
 * Each trending topic is stored as a MENTION SourcePost so the enrichment stage
 * can later search for it across social platforms.
 */
@Component
public class GoogleTrendsCollector extends AbstractCollector {

    private static final String DAILY_TRENDS_URL =
        "https://trends.google.com/trends/api/dailytrends?hl=en-IN&tz=-330&geo=IN&ns=15&ed=today";

    // Simple keyword filter — keep only topics that look like product / shopping queries
    private static final Map<String, ProductCategory> KEYWORD_CATEGORY_HINTS = Map.of(
        "phone",     ProductCategory.ELECTRONICS_GADGETS,
        "laptop",    ProductCategory.ELECTRONICS_GADGETS,
        "watch",     ProductCategory.ELECTRONICS_GADGETS,
        "earbuds",   ProductCategory.ELECTRONICS_GADGETS,
        "dress",     ProductCategory.FASHION_APPAREL,
        "shoes",     ProductCategory.FASHION_APPAREL,
        "book",      ProductCategory.BOOKS_MEDIA,
        "game",      ProductCategory.TOYS_GAMES,
        "kitchen",   ProductCategory.HOME_KITCHEN,
        "beauty",    ProductCategory.BEAUTY_PERSONAL_CARE
    );

    @Override
    public Platform getPlatform() { return Platform.GOOGLE_TRENDS; }

    @Override
    public boolean supportsDiscovery() { return true; }

    @Override
    public CollectionResult discover() {
        CollectionResult result = new CollectionResult(getPlatform());
        try {
            String raw = fetchRaw(DAILY_TRENDS_URL);
            // Strip XSSI guard prefix
            int jsonStart = raw.indexOf('{');
            if (jsonStart < 0) {
                log.warn("Google Trends: unexpected response format — no JSON object found");
                return result;
            }
            JsonNode root = objectMapper.readTree(raw.substring(jsonStart));
            JsonNode trendingSearches = root
                .path("default")
                .path("trendingSearchesDays");

            if (!trendingSearches.isArray() || trendingSearches.isEmpty()) {
                log.debug("Google Trends: no trending searches in response");
                return result;
            }

            // Take today's trending searches (first element = most recent day)
            JsonNode today = trendingSearches.get(0).path("trendingSearches");
            if (!today.isArray()) return result;

            for (JsonNode trend : today) {
                String title = trend.path("title").path("query").asText("").trim();
                if (title.isBlank()) continue;

                String traffic = trend.path("formattedTraffic").asText(""); // e.g. "200K+"
                String exploreUrl = trend.path("shareUrl").asText("").trim();

                Product product = new Product();
                product.setName(title);
                product.setCategory(guessCategoryFromTitle(title));

                SourcePost post = new SourcePost();
                post.setPlatform(Platform.GOOGLE_TRENDS);
                post.setType(PostType.MENTION);
                post.setTitle("Trending in India: " + title);
                post.setContent("Google Trends India — search volume: " + (traffic.isBlank() ? "N/A" : traffic));
                post.setUrl(exploreUrl.isBlank() ? DAILY_TRENDS_URL : exploreUrl);
                post.setRegion(new Region());

                result.addProduct(product).addPost(post);
            }

            log.debug("Google Trends: found {} trending topics", result.productCount());
        } catch (Exception e) {
            log.warn("Google Trends discovery failed: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Fetches the raw response body as a string (needed to strip XSSI prefix before JSON parsing).
     */
    private String fetchRaw(String url) throws Exception {
        return org.jsoup.Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(15_000)
            .ignoreContentType(true)
            .header("Accept", "application/json")
            .header("Accept-Language", "en-IN,en;q=0.9")
            .execute()
            .body();
    }

    private ProductCategory guessCategoryFromTitle(String title) {
        String lower = title.toLowerCase();
        for (Map.Entry<String, ProductCategory> hint : KEYWORD_CATEGORY_HINTS.entrySet()) {
            if (lower.contains(hint.getKey())) return hint.getValue();
        }
        return ProductCategory.UNCATEGORIZED;
    }
}
