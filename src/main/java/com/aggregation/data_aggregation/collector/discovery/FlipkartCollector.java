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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discovers trending products from Flipkart using the official Affiliate API.
 *
 * Why not scraping? Flipkart aggressively blocks bots (403) from cloud and
 * residential IPs alike. The Affiliate API is free, reliable, and returns
 * structured JSON with product data.
 *
 * Setup (free, ~5 minutes):
 *   1. Go to https://affiliate.flipkart.com and sign up
 *   2. After email verification, go to "API Access" in the dashboard
 *   3. Copy your Affiliate ID and Token
 *   4. Add to .env: FLIPKART_AFFILIATE_ID=... and FLIPKART_AFFILIATE_TOKEN=...
 *
 * Without credentials this collector is skipped gracefully.
 *
 * API Reference: https://affiliate.flipkart.com/api-docs
 * Endpoint: GET https://affiliate-api.flipkart.io/affiliate/offers/v0/top_offers
 */
@Component
public class FlipkartCollector extends AbstractCollector {

    private static final String API_BASE =
        "https://affiliate-api.flipkart.io/affiliate/offers/v0/top_offers";

    // Flipkart category IDs → our internal category
    private static final Map<String, ProductCategory> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("MOBILE",       ProductCategory.ELECTRONICS_GADGETS);
        CATEGORIES.put("LAPTOP",       ProductCategory.ELECTRONICS_GADGETS);
        CATEGORIES.put("HEADPHONES",   ProductCategory.ELECTRONICS_GADGETS);
        CATEGORIES.put("TELEVISION",   ProductCategory.ELECTRONICS_GADGETS);
        CATEGORIES.put("CLOTHING_SET", ProductCategory.FASHION_APPAREL);
        CATEGORIES.put("SHOE",         ProductCategory.FASHION_APPAREL);
        CATEGORIES.put("HOME_FURNISHING", ProductCategory.HOME_KITCHEN);
        CATEGORIES.put("BEAUTY",       ProductCategory.BEAUTY_PERSONAL_CARE);
        CATEGORIES.put("SPORTS",       ProductCategory.SPORTS_FITNESS);
        CATEGORIES.put("BOOK",         ProductCategory.BOOKS_MEDIA);
    }

    @Value("${app.flipkart.affiliate-id:}")
    private String affiliateId;

    @Value("${app.flipkart.affiliate-token:}")
    private String affiliateToken;

    @Override
    public Platform getPlatform() { return Platform.FLIPKART; }

    @Override
    public boolean supportsDiscovery() { return true; }

    @Override
    public CollectionResult discover() {
        CollectionResult result = new CollectionResult(getPlatform());

        if (affiliateId == null || affiliateId.isBlank()
                || affiliateToken == null || affiliateToken.isBlank()) {
            log.debug("FlipkartCollector: affiliate credentials not configured — skipping. "
                + "Register free at https://affiliate.flipkart.com to enable Flipkart data.");
            return result;
        }

        for (Map.Entry<String, ProductCategory> entry : CATEGORIES.entrySet()) {
            try {
                scrapeCategory(entry.getKey(), entry.getValue(), result);
                sleep(1_000);
            } catch (Exception e) {
                log.warn("Flipkart API failed for category '{}': {}", entry.getKey(), e.getMessage());
            }
        }

        log.info("Flipkart: collected {} products across {} categories",
            result.productCount(), CATEGORIES.size());
        return result;
    }

    private void scrapeCategory(String categoryId, ProductCategory category,
                                CollectionResult result) throws Exception {
        String url = API_BASE + "?id=" + categoryId + "&page=1&size=20";

        JsonNode root = fetchJson(url,
            "Fk-Affiliate-Id",    affiliateId,
            "Fk-Affiliate-Token", affiliateToken,
            "Accept",             "application/json");

        JsonNode products = root.path("products");
        if (!products.isArray()) {
            log.debug("Flipkart/{}: no products array in response", categoryId);
            return;
        }

        int rank = 1;
        for (JsonNode node : products) {
            JsonNode base  = node.path("productBaseInfo");
            JsonNode ident = base.path("productIdentifier");
            JsonNode attrs = base.path("productAttributes");

            String title     = attrs.path("title").asText("").trim();
            String brand     = attrs.path("productBrand").asText(null);
            String productUrl = ident.path("affProductUrl").asText(
                                ident.path("productUrl").asText("")).trim();
            String imageUrl  = attrs.path("imageUrls").path("400x400").asText(null);
            double price     = attrs.path("sellingPrice").path("amount").asDouble(0);
            String ratingStr = attrs.path("productRating").asText("0");

            if (title.isBlank()) continue;

            Product product = new Product();
            product.setName(title);
            product.setCategory(category);
            if (brand != null && !brand.isBlank()) product.setBrand(brand);
            if (imageUrl != null && !imageUrl.isBlank()) product.setImageUrl(imageUrl);

            SourcePost post = new SourcePost();
            post.setPlatform(Platform.FLIPKART);
            post.setType(PostType.BESTSELLER_RANK);
            post.setTitle("Flipkart Top #" + rank + " in " + category.name() + ": " + title);
            post.setContent("Rank: " + rank
                + " | Rating: " + (ratingStr.equals("0") ? "N/A" : ratingStr + "/5")
                + (price > 0 ? " | Price: ₹" + String.format("%.0f", price) : ""));
            post.setUrl(productUrl.isBlank() ? "https://www.flipkart.com" : productUrl);
            post.setRegion(new Region());
            if (!ratingStr.equals("0")) post.setRating(parseRating(ratingStr));

            result.addProduct(product).addPost(post);
            rank++;
        }

        log.debug("Flipkart/{}: {} products", categoryId, rank - 1);
    }
}
