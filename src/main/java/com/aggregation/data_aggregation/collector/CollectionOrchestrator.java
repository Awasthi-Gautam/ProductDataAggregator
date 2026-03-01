package com.aggregation.data_aggregation.collector;

import com.aggregation.data_aggregation.model.entity.Product;
import com.aggregation.data_aggregation.model.entity.SourcePost;
import com.aggregation.data_aggregation.model.entity.TrendSnapshot;
import com.aggregation.data_aggregation.model.enums.PostType;
import com.aggregation.data_aggregation.repository.ProductRepository;
import com.aggregation.data_aggregation.repository.SourcePostRepository;
import com.aggregation.data_aggregation.repository.SourcePostSearchRepository;
import com.aggregation.data_aggregation.repository.TrendLeaderboardRepository;
import com.aggregation.data_aggregation.repository.TrendSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hourly collection pipeline:
 *
 *  :00 — Stage 1 Discovery  : scrape bestseller lists → build product catalog
 *  :10 — Stage 2 Enrichment : search social platforms for each product
 *  :20 — Stage 3 Scoring    : compute trend scores → update Redis leaderboard
 *
 * Scoring uses two signals:
 *   • Popularity (cumulative) — how much total signal a product has accumulated
 *   • Velocity               — rate of change vs previous cycle (what is RISING)
 *
 * Both signals are price-weighted: a ₹50,000 phone appearing in bestsellers
 * contributes more than a ₹50 snack, using a log10 multiplier.
 */
@Component
@ConditionalOnProperty(name = "app.collectors.enabled", havingValue = "true", matchIfMissing = true)
public class CollectionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CollectionOrchestrator.class);
    private static final int MAX_PRODUCTS_TO_ENRICH = 50;

    /** Matches "Price: ₹1,499" or "Price: ₹1499.00" in SourcePost content */
    private static final Pattern PRICE_PATTERN =
        Pattern.compile("Price:\\s*₹([\\d,]+(?:\\.\\d+)?)");

    private final List<Collector> collectors;
    private final ProductRepository productRepository;
    private final SourcePostRepository sourcePostRepository;
    private final SourcePostSearchRepository searchRepository;
    private final TrendLeaderboardRepository leaderboard;
    private final TrendSnapshotRepository snapshotRepository;

    public CollectionOrchestrator(
        List<Collector> collectors,
        ProductRepository productRepository,
        SourcePostRepository sourcePostRepository,
        SourcePostSearchRepository searchRepository,
        TrendLeaderboardRepository leaderboard,
        TrendSnapshotRepository snapshotRepository
    ) {
        this.collectors = collectors;
        this.productRepository = productRepository;
        this.sourcePostRepository = sourcePostRepository;
        this.searchRepository = searchRepository;
        this.leaderboard = leaderboard;
        this.snapshotRepository = snapshotRepository;
    }

    // ── Stage 1: Discovery ────────────────────────────────────────────────

    @Scheduled(cron = "${app.collectors.discovery-cron:0 0 * * * *}")
    public void runDiscovery() {
        log.info("=== Stage 1: Discovery started ===");
        List<Collector> discoveryCollectors = collectors.stream()
            .filter(Collector::supportsDiscovery)
            .toList();

        if (discoveryCollectors.isEmpty()) {
            log.info("No discovery collectors registered — skipping");
            return;
        }

        for (Collector collector : discoveryCollectors) {
            try {
                log.info("Discovering: {}", collector.getPlatform());
                CollectionResult result = collector.discover();
                if (result.hasError()) {
                    log.warn("Discovery error [{}]: {}", collector.getPlatform(), result.getErrorMessage());
                    continue;
                }
                persistDiscoveryResult(result);
                log.info("Discovered [{}]: {} products, {} posts",
                    collector.getPlatform(), result.productCount(), result.postCount());
            } catch (Exception e) {
                log.error("Discovery failed [{}]: {}", collector.getPlatform(), e.getMessage(), e);
            }
        }
        log.info("=== Stage 1: Discovery complete ===");
    }

    // ── Stage 2: Enrichment ───────────────────────────────────────────────

    @Scheduled(cron = "${app.collectors.enrichment-cron:0 10 * * * *}")
    public void runEnrichment() {
        log.info("=== Stage 2: Enrichment started ===");
        List<Collector> enrichmentCollectors = collectors.stream()
            .filter(Collector::supportsEnrichment)
            .toList();

        if (enrichmentCollectors.isEmpty()) {
            log.info("No enrichment collectors registered — skipping");
            return;
        }

        List<Product> productsToEnrich = productRepository.findAll()
            .stream()
            .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
            .limit(MAX_PRODUCTS_TO_ENRICH)
            .toList();

        if (productsToEnrich.isEmpty()) {
            log.info("No products to enrich yet — run discovery first");
            return;
        }

        for (Product product : productsToEnrich) {
            for (Collector collector : enrichmentCollectors) {
                try {
                    CollectionResult result = collector.enrich(product.getName(), product.getId());
                    if (result.hasError()) {
                        log.warn("Enrichment error [{}] for '{}': {}",
                            collector.getPlatform(), product.getName(), result.getErrorMessage());
                        continue;
                    }
                    persistEnrichmentResult(result);
                } catch (Exception e) {
                    log.error("Enrichment failed [{}] for '{}': {}",
                        collector.getPlatform(), product.getName(), e.getMessage());
                }
            }
        }
        log.info("=== Stage 2: Enrichment complete — {} products enriched ===", productsToEnrich.size());
    }

    // ── Stage 3: Scoring ──────────────────────────────────────────────────

    @Scheduled(cron = "${app.collectors.scoring-cron:0 20 * * * *}")
    public void runScoring() {
        log.info("=== Stage 3: Scoring started ===");
        List<Product> products = productRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusHours(2);

        // Exclude snapshots written in the last 30 min so we don't diff against ourselves
        LocalDateTime snapshotCutoff = now.minusMinutes(30);

        int scored = 0;

        for (Product product : products) {
            List<SourcePost> recentPosts = sourcePostRepository.findByCollectedAtAfter(since)
                .stream()
                .filter(p -> product.getId().equals(p.getProductId()))
                .toList();

            if (recentPosts.isEmpty()) continue;

            // ── Price-weighted score ──────────────────────────────────────
            double baseScore  = computeBaseScore(recentPosts);
            double price      = extractAvgPrice(recentPosts);
            double weighted   = baseScore * priceMultiplier(price);

            // ── Popularity leaderboard (cumulative) ───────────────────────
            String state = firstNonNullState(recentPosts);
            String city  = firstNonNullCity(recentPosts);
            leaderboard.incrementScore(product.getId(), weighted, state, city);

            String categoryName = product.getCategory() != null ? product.getCategory().name() : null;
            if (categoryName != null) {
                leaderboard.incrementCategoryScore(product.getId(), categoryName, weighted);
            }

            // ── Velocity = this cycle's score − previous cycle's score ────
            Optional<TrendSnapshot> prev = snapshotRepository
                .findTopByProductIdAndRecordedAtBeforeOrderByRecordedAtDesc(
                    product.getId(), snapshotCutoff);

            double velocity = prev.map(s -> weighted - s.getScore())
                                  .orElse(weighted); // first appearance = full score as velocity

            leaderboard.setVelocityScore(product.getId(), velocity, categoryName);

            // ── Persist snapshot for next cycle ───────────────────────────
            snapshotRepository.save(new TrendSnapshot(product.getId(), weighted, now));

            scored++;
        }

        // Purge snapshots older than 48 h — keeps the table bounded
        snapshotRepository.deleteOlderThan(now.minusHours(48));

        log.info("=== Stage 3: Scoring complete — {} products scored ===", scored);
    }

    // ── Persistence helpers ───────────────────────────────────────────────

    private void persistDiscoveryResult(CollectionResult result) {
        for (CollectionResult.ProductEntry entry : result.getEntries()) {
            Product incoming = entry.getProduct();
            Product saved = productRepository
                .findByNameAndBrand(incoming.getName(), incoming.getBrand())
                .orElseGet(() -> productRepository.save(incoming));

            for (SourcePost post : entry.getPosts()) {
                post.setProductId(saved.getId());
                savePostIfNew(post);
            }
        }
        for (SourcePost post : result.getStandalonePosts()) {
            savePostIfNew(post);
        }
    }

    private void persistEnrichmentResult(CollectionResult result) {
        for (SourcePost post : result.getStandalonePosts()) {
            savePostIfNew(post);
        }
    }

    private void savePostIfNew(SourcePost post) {
        if (post.getUrl() == null || post.getUrl().isBlank()) return;
        if (!sourcePostRepository.existsByUrlAndPlatform(post.getUrl(), post.getPlatform())) {
            sourcePostRepository.save(post);
        }
    }

    // ── Scoring helpers ───────────────────────────────────────────────────

    /**
     * Base score (before price weighting):
     *   rankCount    × 0.35  — bestseller rank signal
     *   mentionCount × 0.30  — social discussions and mentions
     *   reviewCount  × 0.20  — review volume
     *   sentiment    × 0.15  — avg rating mapped to −1..1
     */
    private double computeBaseScore(List<SourcePost> posts) {
        long rankCount    = posts.stream().filter(p -> p.getType() == PostType.BESTSELLER_RANK).count();
        long mentionCount = posts.stream().filter(p -> p.getType() == PostType.MENTION
                                                    || p.getType() == PostType.DISCUSSION).count();
        long reviewCount  = posts.stream().filter(p -> p.getType() == PostType.REVIEW).count();

        double avgRating = posts.stream()
            .filter(p -> p.getRating() != null)
            .mapToDouble(SourcePost::getRating)
            .average()
            .orElse(3.0);

        double sentiment = (avgRating - 3.0) / 2.0; // 1–5 → −1..1
        return (rankCount * 0.35) + (mentionCount * 0.30) + (reviewCount * 0.20) + (sentiment * 0.15);
    }

    /**
     * Price multiplier using log10 so the curve is smooth and not explosive:
     *
     *   ₹0   (unknown) → 1.00×  (no boost)
     *   ₹100           → 1.30×
     *   ₹500           → 1.78×
     *   ₹1,000         → 2.04×
     *   ₹5,000         → 2.71×
     *   ₹10,000        → 3.00×
     *   ₹50,000        → 3.70×
     *   ₹80,000        → 3.90×
     *
     * Formula: 1 + log10(price / 100 + 1)
     */
    private double priceMultiplier(double price) {
        if (price <= 0) return 1.0;
        return 1.0 + Math.log10(price / 100.0 + 1.0);
    }

    /**
     * Parses "Price: ₹1,499" from BESTSELLER_RANK post content.
     * Returns 0 if price is unavailable ("N/A") or unparseable.
     */
    private double extractAvgPrice(List<SourcePost> posts) {
        return posts.stream()
            .filter(p -> p.getType() == PostType.BESTSELLER_RANK && p.getContent() != null)
            .mapToDouble(p -> {
                Matcher m = PRICE_PATTERN.matcher(p.getContent());
                if (!m.find()) return 0;
                try {
                    return Double.parseDouble(m.group(1).replace(",", ""));
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .filter(p -> p > 0)
            .average()
            .orElse(0);
    }

    private String firstNonNullState(List<SourcePost> posts) {
        return posts.stream()
            .filter(p -> p.getRegion() != null && p.getRegion().getState() != null)
            .map(p -> p.getRegion().getState())
            .findFirst().orElse(null);
    }

    private String firstNonNullCity(List<SourcePost> posts) {
        return posts.stream()
            .filter(p -> p.getRegion() != null && p.getRegion().getCity() != null)
            .map(p -> p.getRegion().getCity())
            .findFirst().orElse(null);
    }
}
