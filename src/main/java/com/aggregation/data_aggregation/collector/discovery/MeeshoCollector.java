package com.aggregation.data_aggregation.collector.discovery;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.enums.Platform;
import org.springframework.stereotype.Component;

/**
 * Discovers trending products on Meesho (meesho.com).
 *
 * Meesho serves a React SPA — direct Jsoup scraping returns a bare shell page.
 * TODO: Implement via one of:
 *   1. Meesho Partner/Affiliate API (requires approval)
 *   2. Headless browser (Playwright/Selenium) for JS-rendered pages
 *   3. Meesho's unofficial mobile API endpoints (reverse-engineered, fragile)
 * Until then this collector is a registered no-op so the framework compiles cleanly.
 */
@Component
public class MeeshoCollector extends AbstractCollector {

    @Override
    public Platform getPlatform() { return Platform.MEESHO; }

    @Override
    public boolean supportsDiscovery() { return false; } // TODO: enable when implemented

    @Override
    public CollectionResult discover() {
        log.debug("MeeshoCollector: not yet implemented");
        return new CollectionResult(getPlatform());
    }
}
