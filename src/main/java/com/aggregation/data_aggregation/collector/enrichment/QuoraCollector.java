package com.aggregation.data_aggregation.collector.enrichment;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.Region;
import com.aggregation.data_aggregation.model.entity.SourcePost;
import com.aggregation.data_aggregation.model.enums.Platform;
import com.aggregation.data_aggregation.model.enums.PostType;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Enriches products by scraping Quora search results for Q&A threads.
 *
 * Quora does not have a public API. We use Jsoup to scrape the search page.
 * Quora heavily uses JavaScript rendering, so only server-side content is captured.
 * Results may be limited — this is a best-effort collector.
 *
 * Search URL: https://www.quora.com/search?q={query}
 */
@Component
public class QuoraCollector extends AbstractCollector {

    private static final String SEARCH_URL = "https://www.quora.com/search?q=%s";
    private static final int MAX_RESULTS = 10;

    @Override
    public Platform getPlatform() { return Platform.QUORA; }

    @Override
    public boolean supportsEnrichment() { return true; }

    @Override
    public CollectionResult enrich(String productName, String productId) {
        CollectionResult result = new CollectionResult(getPlatform());
        try {
            // Trim to a short searchable name — product names from scrapers can include
            // rating/price text (e.g. "Product Name | Feature... 4.5 stars ₹399"). Take
            // the first pipe-separated segment up to 80 chars.
            String shortName = productName.split("[|\\u2014]")[0].trim();
            if (shortName.length() > 80) shortName = shortName.substring(0, 80).trim();
            String query = URLEncoder.encode(shortName + " India", StandardCharsets.UTF_8);
            String url = String.format(SEARCH_URL, query);

            Document doc = fetchHtml(url);

            // Quora question links appear as <a> with href starting /...? or /What-...
            Elements links = doc.select("a[href^='/']");
            int count = 0;

            for (Element link : links) {
                if (count >= MAX_RESULTS) break;

                String href = link.attr("abs:href");
                String text = link.text().trim();

                // Filter: must look like a question (contains '?') and not be navigation
                if (text.length() < 15 || !text.contains("?")) continue;
                if (href.contains("/profile/") || href.contains("/topic/")) continue;
                if (href.contains("/search")) continue;

                SourcePost post = new SourcePost();
                post.setProductId(productId);
                post.setPlatform(Platform.QUORA);
                post.setType(PostType.QA);
                post.setTitle(text);
                post.setContent("Quora question: " + text);
                post.setUrl(href);
                post.setRegion(new Region());

                result.addPost(post);
                count++;
            }

            log.debug("Quora enrichment for '{}': {} questions", productName, count);
        } catch (Exception e) {
            log.warn("Quora enrichment failed for '{}': {}", productName, e.getMessage());
        }
        return result;
    }
}
