package com.aggregation.data_aggregation.collector.discovery;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.enums.Platform;
import org.springframework.stereotype.Component;

/**
 * Discovers fast-moving consumer goods (FMCG) trending on Blinkit.
 *
 * Blinkit (formerly Grofers) is a quick-commerce app. Its web frontend is a React SPA
 * that requires location consent and authentication before showing products.
 *
 * TODO: Implement via Blinkit's internal catalog API. Requires:
 *   1. Valid location header (lat/lon for a major city, e.g. Bengaluru: 12.9716,77.5946)
 *   2. Cookie session or device token
 *   Candidate endpoint: https://blinkit.com/v2/products/category/?category_id=...
 *
 * Registered as a skeleton — supportsDiscovery() returns false until implemented.
 */
@Component
public class BlinkitCollector extends AbstractCollector {

    @Override
    public Platform getPlatform() { return Platform.BLINKIT; }

    @Override
    public boolean supportsDiscovery() { return false; } // TODO: enable when implemented

    @Override
    public CollectionResult discover() {
        log.debug("BlinkitCollector: not yet implemented");
        return new CollectionResult(getPlatform());
    }
}
