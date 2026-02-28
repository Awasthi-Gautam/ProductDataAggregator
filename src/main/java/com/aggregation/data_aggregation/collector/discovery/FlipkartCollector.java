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
 * Scrapes Flipkart's "Best Sellers" / top-rated pages per category.
 *
 * URL pattern: https://www.flipkart.com/{category-slug}?sort=popularity
 * Note: Flipkart aggressively blocks bots. This collector is best-effort —
 * it gracefully skips categories where scraping is blocked (403/CAPTCHA).
 */
@Component
public class FlipkartCollector extends AbstractCollector {

    private static final String BASE_URL = "https://www.flipkart.com/";
    private static final int MAX_PER_CATEGORY = 20;

    private static final Map<String, ProductCategory> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("mobiles/pr?sid=tyy%2C4io&sort=popularity_desc",  ProductCategory.ELECTRONICS_GADGETS);
        CATEGORIES.put("laptops/pr?sid=6bo%2Cb5g&sort=popularity_desc",  ProductCategory.ELECTRONICS_GADGETS);
        CATEGORIES.put("clothing/pr?sid=clo&sort=popularity_desc",       ProductCategory.FASHION_APPAREL);
        CATEGORIES.put("kitchen-dining/pr?sid=pfu&sort=popularity_desc", ProductCategory.HOME_KITCHEN);
        CATEGORIES.put("beauty/pr?sid=bar&sort=popularity_desc",         ProductCategory.BEAUTY_PERSONAL_CARE);
        CATEGORIES.put("books/pr?sid=bks&sort=popularity_desc",          ProductCategory.BOOKS_MEDIA);
        CATEGORIES.put("toys-games/pr?sid=toy&sort=popularity_desc",     ProductCategory.TOYS_GAMES);
        CATEGORIES.put("sports-fitness/pr?sid=spo&sort=popularity_desc", ProductCategory.SPORTS_FITNESS);
    }

    @Override
    public Platform getPlatform() { return Platform.FLIPKART; }

    @Override
    public boolean supportsDiscovery() { return true; }

    @Override
    public CollectionResult discover() {
        CollectionResult result = new CollectionResult(getPlatform());
        for (Map.Entry<String, ProductCategory> entry : CATEGORIES.entrySet()) {
            try {
                scrapeCategory(entry.getKey(), entry.getValue(), result);
                sleep(4_000);
            } catch (Exception e) {
                log.warn("Flipkart scrape failed for '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        return result;
    }

    private void scrapeCategory(String slug, ProductCategory category, CollectionResult result) throws Exception {
        Document doc = fetchHtml(BASE_URL + slug);

        // Flipkart product cards use different container classes over time — try all known variants
        Elements items = doc.select("div._1AtVbE");
        if (items.isEmpty()) items = doc.select("div._13oc-S");
        if (items.isEmpty()) items = doc.select("div[class*='productCard']");
        if (items.isEmpty()) items = doc.select("div._2kHMtA");

        if (items.isEmpty()) {
            log.debug("Flipkart/{}: no product cards found — may be blocked or layout changed", slug);
            return;
        }

        int rank = 1;
        for (Element item : items) {
            if (rank > MAX_PER_CATEGORY) break;

            String title = extractTitle(item);
            if (title.isBlank()) continue;

            String href = item.select("a[href*='/p/']").attr("abs:href");
            if (href.contains("?")) href = href.substring(0, href.indexOf('?'));

            String price = extractPrice(item);
            String ratingText = item.select("div._3LWZlK").text().trim();
            double rating = parseRating(ratingText);

            Product product = new Product();
            product.setName(title);
            product.setCategory(category);

            String imageUrl = item.select("img._396cs4, img._2r_T1I").attr("src");
            if (!imageUrl.isBlank()) product.setImageUrl(imageUrl);

            SourcePost post = new SourcePost();
            post.setPlatform(Platform.FLIPKART);
            post.setType(PostType.BESTSELLER_RANK);
            post.setTitle("Flipkart Popular #" + rank + " in " + category.name() + ": " + title);
            post.setContent("Rank: " + rank
                + " | Rating: " + (rating > 0 ? rating + "/5" : "N/A")
                + " | Price: " + (price.isBlank() ? "N/A" : "₹" + price));
            post.setUrl(href.isBlank() ? BASE_URL + slug : href);
            post.setRegion(new Region());
            if (rating > 0) post.setRating(rating);

            result.addProduct(product).addPost(post);
            rank++;
        }

        log.debug("Flipkart/{}: found {} products", slug, rank - 1);
    }

    private String extractTitle(Element item) {
        for (String sel : new String[]{
            "div._4rR01T", "a.s1Q9rs", "a.IRpwTa",
            "div[class*='title']", "a[title]"
        }) {
            String text = item.select(sel).text().trim();
            if (!text.isBlank() && text.length() > 3) return text;
        }
        // Fallback: use the anchor title attribute
        String title = item.select("a[title]").attr("title").trim();
        return title.length() > 3 ? title : "";
    }

    private String extractPrice(Element item) {
        for (String sel : new String[]{
            "div._30jeq3", "div._1_WHN1",
            "span[class*='price']", "div[class*='Price']"
        }) {
            String price = item.select(sel).text()
                .replace("₹", "").replace(",", "").trim();
            if (!price.isBlank()) return price;
        }
        return "";
    }
}
