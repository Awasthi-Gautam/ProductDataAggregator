package com.aggregation.data_aggregation.collector.discovery;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.enums.Platform;
import org.springframework.stereotype.Component;

/**
 * Discovers trending grocery and household products on Swiggy Instamart.
 *
 * Swiggy Instamart is a quick-commerce service embedded in the Swiggy app.
 * The web interface at https://www.swiggy.com/instamart is a fully client-side SPA
 * that requires geolocation and a valid session to load products.
 *
 * TODO: Implement via Swiggy's internal Instamart catalog API. Requires:
 *   1. A lat/lon based store ID (obtained by calling Swiggy's store-finder API)
 *   2. Session cookies from a logged-in Swiggy account
 *   Candidate endpoint: https://www.swiggy.com/api/instamart/home?...
 *
 * Registered as a skeleton — supportsDiscovery() returns false until implemented.
 */
@Component
public class SwiggyInstamartCollector extends AbstractCollector {

    @Override
    public Platform getPlatform() { return Platform.SWIGGY_INSTAMART; }

    @Override
    public boolean supportsDiscovery() { return false; } // TODO: enable when implemented

    @Override
    public CollectionResult discover() {
        log.debug("SwiggyInstamartCollector: not yet implemented");
        return new CollectionResult(getPlatform());
    }
}
