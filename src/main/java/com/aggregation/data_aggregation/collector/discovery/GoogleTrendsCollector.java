package com.aggregation.data_aggregation.collector.discovery;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.Region;
import com.aggregation.data_aggregation.model.entity.Product;
import com.aggregation.data_aggregation.model.entity.SourcePost;
import com.aggregation.data_aggregation.model.enums.Platform;
import com.aggregation.data_aggregation.model.enums.PostType;
import com.aggregation.data_aggregation.model.enums.ProductCategory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fetches Google Trends daily trending searches for India via the public RSS feed.
 *
 * Endpoint: https://trends.google.com/trending/rss?geo=IN&hours=48
 * Returns RSS/XML — no API key required, works from any IP.
 *
 * Each trending topic becomes a Product so the enrichment stage can later find
 * YouTube reviews and Reddit discussions for it.
 */
@Component
public class GoogleTrendsCollector extends AbstractCollector {

    private static final String RSS_URL =
        "https://trends.google.com/trending/rss?geo=IN&hours=48";

    private static final Map<String, ProductCategory> CATEGORY_HINTS = Map.of(
        "phone",    ProductCategory.ELECTRONICS_GADGETS,
        "mobile",   ProductCategory.ELECTRONICS_GADGETS,
        "laptop",   ProductCategory.ELECTRONICS_GADGETS,
        "watch",    ProductCategory.ELECTRONICS_GADGETS,
        "earbuds",  ProductCategory.ELECTRONICS_GADGETS,
        "headphones", ProductCategory.ELECTRONICS_GADGETS,
        "dress",    ProductCategory.FASHION_APPAREL,
        "shoes",    ProductCategory.FASHION_APPAREL,
        "book",     ProductCategory.BOOKS_MEDIA,
        "kitchen",  ProductCategory.HOME_KITCHEN
    );

    @Override
    public Platform getPlatform() { return Platform.GOOGLE_TRENDS; }

    @Override
    public boolean supportsDiscovery() { return true; }

    @Override
    public CollectionResult discover() {
        CollectionResult result = new CollectionResult(getPlatform());
        try {
            Document doc = Jsoup.connect(RSS_URL)
                .userAgent(USER_AGENT)
                .timeout(15_000)
                .ignoreContentType(true)
                .parser(Parser.xmlParser())
                .get();

            Elements items = doc.select("item");
            if (items.isEmpty()) {
                log.warn("Google Trends RSS: no items found — feed may have changed");
                return result;
            }

            for (Element item : items) {
                String title   = text(item, "title");
                String link    = text(item, "link");
                String traffic = text(item, "ht|approx_traffic"); // e.g. "200K+"

                if (title.isBlank()) continue;

                Product product = new Product();
                product.setName(title);
                product.setCategory(guessCategory(title));

                SourcePost post = new SourcePost();
                post.setPlatform(Platform.GOOGLE_TRENDS);
                post.setType(PostType.MENTION);
                post.setTitle("Trending in India: " + title);
                post.setContent("Google Trends India — searches: " + (traffic.isBlank() ? "N/A" : traffic));
                post.setUrl(link.isBlank() ? RSS_URL : link);
                post.setRegion(new Region());

                result.addProduct(product).addPost(post);
            }

            log.info("Google Trends RSS: {} trending topics collected", result.productCount());
        } catch (Exception e) {
            log.warn("Google Trends discovery failed: {}", e.getMessage());
        }
        return result;
    }

    private String text(Element parent, String cssQuery) {
        Element el = parent.selectFirst(cssQuery);
        return el != null ? el.text().trim() : "";
    }

    private ProductCategory guessCategory(String title) {
        String lower = title.toLowerCase();
        for (Map.Entry<String, ProductCategory> hint : CATEGORY_HINTS.entrySet()) {
            if (lower.contains(hint.getKey())) return hint.getValue();
        }
        return ProductCategory.UNCATEGORIZED;
    }
}
