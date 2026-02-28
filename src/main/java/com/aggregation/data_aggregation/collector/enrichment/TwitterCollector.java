package com.aggregation.data_aggregation.collector.enrichment;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.enums.Platform;
import org.springframework.stereotype.Component;

/**
 * Enriches products by searching Twitter/X for mentions.
 *
 * Twitter/X API v2 requires a Bearer token (free tier: 1 app, read-only, 500k tweets/month).
 * Paid access required for full search-recent endpoint at scale.
 *
 * TODO: Implement using Twitter API v2 recent search:
 *   GET https://api.twitter.com/2/tweets/search/recent?query={productName}+lang:en&max_results=10
 *   Headers: Authorization: Bearer {TWITTER_BEARER_TOKEN}
 *
 * Configure via: app.twitter.bearer-token in application.yml / .env
 * Registered as skeleton — supportsEnrichment() returns false until implemented.
 */
@Component
public class TwitterCollector extends AbstractCollector {

    @Override
    public Platform getPlatform() { return Platform.TWITTER; }

    @Override
    public boolean supportsEnrichment() { return false; } // TODO: enable when implemented

    @Override
    public CollectionResult enrich(String productName, String productId) {
        log.debug("TwitterCollector: not yet implemented");
        return new CollectionResult(getPlatform());
    }
}
