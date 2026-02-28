package com.aggregation.data_aggregation.collector.discovery;

import com.aggregation.data_aggregation.collector.AbstractCollector;
import com.aggregation.data_aggregation.collector.CollectionResult;
import com.aggregation.data_aggregation.model.enums.Platform;
import org.springframework.stereotype.Component;

/**
 * Discovers bestselling grocery and household products on BigBasket.
 *
 * BigBasket's website loads product data via authenticated XHR calls after login.
 * The public-facing catalog pages partially render server-side.
 *
 * TODO: Implement scraping of publicly accessible bestseller/top-selling pages:
 *   https://www.bigbasket.com/pc/fruits-vegetables/?nc=nb
 *   Parse product tiles: div.SKUDeck___StyledDiv, span.Label-sc-15v1nk5
 *
 * Registered as a skeleton — supportsDiscovery() returns false until implemented.
 */
@Component
public class BigBasketCollector extends AbstractCollector {

    @Override
    public Platform getPlatform() { return Platform.BIGBASKET; }

    @Override
    public boolean supportsDiscovery() { return false; } // TODO: enable when implemented

    @Override
    public CollectionResult discover() {
        log.debug("BigBasketCollector: not yet implemented");
        return new CollectionResult(getPlatform());
    }
}
