package com.aggregation.data_aggregation.collector;

import com.aggregation.data_aggregation.model.enums.Platform;

/**
 * Base interface for all data collectors.
 *
 * Discovery collectors (Stage 1): find trending/bestselling products from
 * e-commerce platforms. They don't need a product name upfront.
 *
 * Enrichment collectors (Stage 2): given a product name, search discussion
 * platforms (Reddit, Twitter, YouTube, etc.) for mentions and reviews.
 */
public interface Collector {

    Platform getPlatform();

    /**
     * Stage 1 — Discovery: collect trending/bestselling products.
     * Called once per hour with no input.
     */
    CollectionResult discover();

    /**
     * Stage 2 — Enrichment: find mentions/reviews for a known product.
     * Called for each product discovered in Stage 1.
     *
     * @param productName the product to search for
     * @param productId   the internal product ID to link posts back to
     */
    CollectionResult enrich(String productName, String productId);

    /**
     * Returns true if this collector supports discovery (Stage 1).
     * E-commerce scrapers return true; social platforms return false.
     */
    default boolean supportsDiscovery() { return false; }

    /**
     * Returns true if this collector supports enrichment (Stage 2).
     * Social/discussion platforms return true; e-commerce scrapers return false.
     */
    default boolean supportsEnrichment() { return false; }
}
