package com.aggregation.data_aggregation.collector.enrichment;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.enums.Platform;
import org.springframework.stereotype.Component;

/**
 * Enriches products by searching Instagram for posts and hashtags.
 *
 * Instagram's Graph API requires a Business/Creator account connected to a Facebook Page.
 * The unofficial Basic Display API was shut down in Dec 2024.
 *
 * TODO: Implement via Meta Content Publishing API (Business account required):
 *   GET https://graph.facebook.com/v18.0/ig_hashtag_search?user_id={id}&q={hashtag}
 *   Then: GET https://graph.facebook.com/v18.0/{hashtag_id}/top_media?fields=...
 *
 * Configure via: app.instagram.access-token in application.yml / .env
 * Registered as skeleton — supportsEnrichment() returns false until implemented.
 */
@Component
public class InstagramCollector extends AbstractCollector {

    @Override
    public Platform getPlatform() { return Platform.INSTAGRAM; }

    @Override
    public boolean supportsEnrichment() { return false; } // TODO: enable when implemented

    @Override
    public CollectionResult enrich(String productName, String productId) {
        log.debug("InstagramCollector: not yet implemented");
        return new CollectionResult(getPlatform());
    }
}
