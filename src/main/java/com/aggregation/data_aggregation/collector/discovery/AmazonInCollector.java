package com.aggregation.data_aggregation.collector.discovery;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.Region;
import com.aggregation.data_aggregation.model.entity.Product;
import com.aggregation.data_aggregation.model.entity.SourcePost;
import com.aggregation.data_aggregation.model.enums.Platform;
import com.aggregation.data_aggregation.model.enums.PostType;
import com.aggregation.data_aggregation.model.enums.ProductCategory;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scrapes amazon.in/gp/bestsellers/{category} for each product category.
 * Extracts top 20 products per category with rank, rating, and price.
 */
@Component
public class AmazonInCollector extends AbstractCollector {

    private static final String BASE_URL = "https://www.amazon.in/gp/bestsellers/";
    private static final int MAX_PER_CATEGORY = 20;

    // Amazon category slug → our ProductCategory
    private static final Map<String, ProductCategory> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("electronics",          ProductCategory.ELECTRONICS_GADGETS);
        CATEGORIES.put("clothing",             ProductCategory.FASHION_APPAREL);
        CATEGORIES.put("kitchen",              ProductCategory.HOME_KITCHEN);
        CATEGORIES.put("beauty",               ProductCategory.BEAUTY_PERSONAL_CARE);
        CATEGORIES.put("books",                ProductCategory.BOOKS_MEDIA);
        CATEGORIES.put("toys",                 ProductCategory.TOYS_GAMES);
        CATEGORIES.put("sports",               ProductCategory.SPORTS_FITNESS);
        CATEGORIES.put("grocery",              ProductCategory.FOOD_GROCERY);
        CATEGORIES.put("automotive",           ProductCategory.AUTOMOTIVE);
    }

    @Override
    public Platform getPlatform() { return Platform.AMAZON_IN; }

    @Override
    public boolean supportsDiscovery() { return true; }

    @Override
    public CollectionResult discover() {
        CollectionResult result = new CollectionResult(getPlatform());

        for (Map.Entry<String, ProductCategory> entry : CATEGORIES.entrySet()) {
            try {
                scrapeCategory(entry.getKey(), entry.getValue(), result);
                sleep(3_000); // respectful delay between category pages
            } catch (Exception e) {
                log.warn("Amazon.in scrape failed for category '{}': {}", entry.getKey(), e.getMessage());
            }
        }

        return result;
    }

    private void scrapeCategory(String slug, ProductCategory category, CollectionResult result) throws Exception {
        Document doc = fetchHtml(BASE_URL + slug);

        // Try multiple container selectors — Amazon's HTML structure changes periodically
        Elements items = doc.select("div.zg-grid-general-faceout");
        if (items.isEmpty()) items = doc.select("li.zg-item-immersion");
        if (items.isEmpty()) items = doc.select("div[class*='zg-item']");

        if (items.isEmpty()) {
            log.debug("No items found on Amazon.in/{} — page structure may have changed", slug);
            return;
        }

        int rank = 1;
        for (Element item : items) {
            if (rank > MAX_PER_CATEGORY) break;

            String title = extractTitle(item);
            if (title.isBlank()) continue;

            String href = extractProductUrl(item);
            String asin = extractAsin(href);
            double rating = parseRating(item.select("span.a-icon-alt").text());
            String price = extractPrice(item);
            String imageUrl = item.select("img").attr("src");

            Product product = new Product();
            product.setName(title);
            product.setCategory(category);
            product.setImageUrl(imageUrl.isBlank() ? null : imageUrl);
            if (asin != null) product.getExternalIds().put(Platform.AMAZON_IN, asin);

            SourcePost post = new SourcePost();
            post.setPlatform(Platform.AMAZON_IN);
            post.setType(PostType.BESTSELLER_RANK);
            post.setTitle("Amazon.in Bestseller #" + rank + " in " + slug + ": " + title);
            post.setContent("Rank: " + rank + " | Rating: " + (rating > 0 ? rating + "/5" : "N/A")
                + " | Price: " + (price.isBlank() ? "N/A" : "₹" + price));
            post.setUrl(href.isBlank() ? BASE_URL + slug : href);
            post.setRegion(new Region());
            if (rating > 0) post.setRating(rating);

            result.addProduct(product).addPost(post);
            rank++;
        }

        log.debug("Amazon.in/{}: found {} products", slug, rank - 1);
    }

    private String extractTitle(Element item) {
        // Try progressively broader selectors
        for (String selector : new String[]{
            "div[class*='line-clamp'] a span",
            "div[class*='p13n-sc-css-line-clamp'] span",
            "a[class*='a-link-normal'] span",
            "span[class*='product-title']"
        }) {
            String text = item.select(selector).text().trim();
            if (!text.isBlank() && text.length() > 3) return cleanTitle(text);
        }
        return "";
    }

    /**
     * Amazon listing text can include rating/review/price appended to the title
     * (e.g. "Product Name 4.5 out of 5 stars 1,234 ₹399.00").
     * Strip that suffix and cap at 200 characters.
     */
    private String cleanTitle(String raw) {
        // Cut at " X.X out of 5 stars" pattern if present
        int starsIdx = raw.indexOf(" out of 5 stars");
        if (starsIdx > 0) {
            // Walk back to find the start of the rating number
            int start = starsIdx;
            while (start > 0 && (Character.isDigit(raw.charAt(start - 1))
                    || raw.charAt(start - 1) == '.' || raw.charAt(start - 1) == ' ')) {
                start--;
            }
            raw = raw.substring(0, start).trim();
        }
        // Also strip trailing price patterns like "₹399.00" or "₹1,234"
        raw = raw.replaceAll("\\s+₹[\\d,]+(\\.\\d+)?\\s*$", "").trim();
        return raw.length() > 200 ? raw.substring(0, 197) + "..." : raw;
    }

    private String extractProductUrl(Element item) {
        for (String selector : new String[]{"a[href*='/dp/']", "a[href*='/gp/product/']"}) {
            String href = item.select(selector).attr("abs:href");
            if (!href.isBlank()) {
                // Strip query params and tracking tokens — keep just the product URL
                int queryIdx = href.indexOf('?');
                return queryIdx > 0 ? href.substring(0, queryIdx) : href;
            }
        }
        return "";
    }

    private String extractPrice(Element item) {
        for (String selector : new String[]{
            "span[class*='p13n-sc-price']",
            "span[class*='a-color-price']",
            "span.a-price span.a-offscreen"
        }) {
            String price = item.select(selector).text()
                .replace("₹", "").replace(",", "").trim();
            if (!price.isBlank()) return price;
        }
        return "";
    }
}
