package com.aggregation.data_aggregation.collector.enrichment;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.enums.Platform;
import org.springframework.stereotype.Component;

/**
 * Enriches products by searching TikTok for product-related videos.
 *
 * TikTok Research API (research.tiktok.com) allows video search by keyword.
 * It requires approval from TikTok and is primarily intended for academic research.
 *
 * TODO: Implement via TikTok Research API:
 *   POST https://open.tiktokapis.com/v2/research/video/query/
 *   Body: {"query": {"and": [{"operation":"IN","field_name":"keyword","field_values":["productName"]}]},
 *          "start_date":"...", "end_date":"...", "max_count":20, "cursor":0, "search_id":"..."}
 *   Headers: Authorization: Bearer {TIKTOK_ACCESS_TOKEN}
 *
 * Configure via: app.tiktok.access-token in application.yml / .env
 * Registered as skeleton — supportsEnrichment() returns false until implemented.
 */
@Component
public class TikTokCollector extends AbstractCollector {

    @Override
    public Platform getPlatform() { return Platform.TIKTOK; }

    @Override
    public boolean supportsEnrichment() { return false; } // TODO: enable when implemented

    @Override
    public CollectionResult enrich(String productName, String productId) {
        log.debug("TikTokCollector: not yet implemented");
        return new CollectionResult(getPlatform());
    }
}
