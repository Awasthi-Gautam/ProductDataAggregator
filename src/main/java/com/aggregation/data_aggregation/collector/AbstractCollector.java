package com.aggregation.data_aggregation.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public abstract class AbstractCollector implements Collector {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36";

    private static final int TIMEOUT_MS = 15_000;

    @Autowired
    protected ObjectMapper objectMapper;

    // ── HTML scraping ─────────────────────────────────────────────────────

    protected Document fetchHtml(String url) throws IOException {
        return Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT_MS)
            .header("Accept-Language", "en-IN,en;q=0.9")
            .header("Accept", "text/html,application/xhtml+xml")
            .get();
    }

    // ── JSON API fetching ─────────────────────────────────────────────────

    protected JsonNode fetchJson(String url) throws IOException {
        String body = Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT_MS)
            .ignoreContentType(true)
            .header("Accept", "application/json")
            .execute()
            .body();
        return objectMapper.readTree(body);
    }

    protected JsonNode fetchJson(String url, String... headers) throws IOException {
        Connection conn = Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT_MS)
            .ignoreContentType(true);
        for (int i = 0; i + 1 < headers.length; i += 2) {
            conn.header(headers[i], headers[i + 1]);
        }
        return objectMapper.readTree(conn.execute().body());
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    protected void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected String extractAsin(String url) {
        if (url == null || url.isBlank()) return null;
        int dp = url.indexOf("/dp/");
        if (dp != -1 && url.length() > dp + 14) return url.substring(dp + 4, dp + 14);
        int gp = url.indexOf("/gp/product/");
        if (gp != -1 && url.length() > gp + 22) return url.substring(gp + 12, gp + 22);
        return null;
    }

    protected double parseRating(String text) {
        if (text == null || text.isBlank()) return 0;
        try {
            return Double.parseDouble(text.trim().split("\\s+")[0].replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ── Default no-op implementations ────────────────────────────────────

    @Override
    public CollectionResult discover() {
        return new CollectionResult(getPlatform());
    }

    @Override
    public CollectionResult enrich(String productName, String productId) {
        return new CollectionResult(getPlatform());
    }
}
