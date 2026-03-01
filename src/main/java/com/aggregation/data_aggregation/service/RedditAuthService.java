package com.aggregation.data_aggregation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Manages Reddit OAuth2 client-credentials tokens.
 *
 * Reddit "script" app flow (no user login required):
 *   POST https://www.reddit.com/api/v1/access_token
 *   Authorization: Basic base64(clientId:clientSecret)
 *   Body: grant_type=client_credentials
 *
 * Tokens last 24 hours. This service caches the token and transparently
 * refreshes it 5 minutes before expiry so callers never get a stale token.
 *
 * Setup (one-time, free):
 *   1. Go to https://www.reddit.com/prefs/apps
 *   2. Click "create another app" → choose type "script"
 *   3. Name: anything (e.g. "data-aggregation-bot")
 *   4. Redirect URI: http://localhost (required but unused for script apps)
 *   5. Copy the app ID (under the name) → REDDIT_CLIENT_ID
 *   6. Copy the secret → REDDIT_CLIENT_SECRET
 *   7. Set REDDIT_USERNAME to your Reddit account username
 *   8. Add all three to your .env file and restart the app
 */
@Service
public class RedditAuthService {

    private static final Logger log = LoggerFactory.getLogger(RedditAuthService.class);
    private static final String TOKEN_URL = "https://www.reddit.com/api/v1/access_token";

    @Value("${app.reddit.client-id:}")
    private String clientId;

    @Value("${app.reddit.client-secret:}")
    private String clientSecret;

    @Value("${app.reddit.username:data-aggregation-bot}")
    private String botUsername;

    private final ObjectMapper objectMapper;

    // Token cache — volatile for thread visibility, synchronized on fetch
    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.MIN;

    public RedditAuthService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** True when client credentials are configured in the environment. */
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank();
    }

    /**
     * User-Agent string required by Reddit's API policy.
     * Format: <platform>:<app ID>:<version> (by /u/<username>)
     */
    public String userAgent() {
        return "java:com.aggregation.data-aggregation:v1.0 (by /u/" + botUsername + ")";
    }

    /**
     * Returns a valid Bearer token, fetching or refreshing as needed.
     * Thread-safe — multiple enrichment threads can call this concurrently.
     */
    public synchronized String getAccessToken() throws IOException {
        // Refresh 5 minutes early to avoid expiry mid-request
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(300))) {
            return cachedToken;
        }
        return fetchNewToken();
    }

    private String fetchNewToken() throws IOException {
        String credentials = Base64.getEncoder().encodeToString(
            (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        String body = Jsoup.connect(TOKEN_URL)
            .userAgent(userAgent())
            .header("Authorization", "Basic " + credentials)
            .data("grant_type", "client_credentials")
            .method(Connection.Method.POST)
            .ignoreContentType(true)
            .timeout(15_000)
            .execute()
            .body();

        JsonNode json = objectMapper.readTree(body);

        String error = json.path("error").asText(null);
        if (error != null) {
            throw new IOException("Reddit OAuth failed: " + error
                + " — check REDDIT_CLIENT_ID and REDDIT_CLIENT_SECRET in your .env");
        }

        cachedToken = json.path("access_token").asText();
        long expiresIn = json.path("expires_in").asLong(86400);
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn);

        log.info("Reddit OAuth: new token obtained, valid for {}h", expiresIn / 3600);
        return cachedToken;
    }
}
