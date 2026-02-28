package com.aggregation.data_aggregation.collector.discovery;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.enums.Platform;
import org.springframework.stereotype.Component;

/**
 * Discovers trending fashion products on AJIO (ajio.com).
 *
 * AJIO uses a React SPA with API calls to an internal GraphQL or REST gateway.
 * TODO: Reverse-engineer category listing endpoints from DevTools network tab.
 *   Candidate URL: https://www.ajio.com/api/category/women-western-wear?start=0&sz=20&sortBy=popularity
 *
 * Registered as a skeleton — supportsDiscovery() returns false until implemented.
 */
@Component
public class AjioCollector extends AbstractCollector {

    @Override
    public Platform getPlatform() { return Platform.AJIO; }

    @Override
    public boolean supportsDiscovery() { return false; } // TODO: enable when implemented

    @Override
    public CollectionResult discover() {
        log.debug("AjioCollector: not yet implemented");
        return new CollectionResult(getPlatform());
    }
}
