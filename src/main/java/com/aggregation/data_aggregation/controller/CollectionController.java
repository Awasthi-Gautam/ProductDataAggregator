package com.aggregation.data_aggregation.controller;

import com.aggregation.data_aggregation.collector.CollectionOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Admin endpoints to trigger collection manually without waiting for the cron schedule.
 * All triggers run async and return 202 Accepted immediately.
 *
 * POST /api/admin/collect/discovery  — scrape bestseller lists → build product catalog
 * POST /api/admin/collect/enrichment — search social platforms for each product
 * POST /api/admin/collect/scoring    — compute trend scores → update Redis leaderboard
 * POST /api/admin/collect/all        — runs all three stages sequentially in background
 */
@RestController
@RequestMapping("/api/admin/collect")
public class CollectionController {

    private static final Logger log = LoggerFactory.getLogger(CollectionController.class);

    private final CollectionOrchestrator orchestrator;

    public CollectionController(CollectionOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/discovery")
    public ResponseEntity<Map<String, String>> triggerDiscovery() {
        CompletableFuture.runAsync(() -> {
            log.info("[manual] Discovery triggered via API");
            orchestrator.runDiscovery();
        });
        return ResponseEntity.accepted().body(Map.of("status", "Discovery started in background"));
    }

    @PostMapping("/enrichment")
    public ResponseEntity<Map<String, String>> triggerEnrichment() {
        CompletableFuture.runAsync(() -> {
            log.info("[manual] Enrichment triggered via API");
            orchestrator.runEnrichment();
        });
        return ResponseEntity.accepted().body(Map.of("status", "Enrichment started in background"));
    }

    @PostMapping("/scoring")
    public ResponseEntity<Map<String, String>> triggerScoring() {
        CompletableFuture.runAsync(() -> {
            log.info("[manual] Scoring triggered via API");
            orchestrator.runScoring();
        });
        return ResponseEntity.accepted().body(Map.of("status", "Scoring started in background"));
    }

    @PostMapping("/all")
    public ResponseEntity<Map<String, String>> triggerAll() {
        CompletableFuture.runAsync(() -> {
            log.info("[manual] Full pipeline triggered via API");
            orchestrator.runDiscovery();
            orchestrator.runEnrichment();
            orchestrator.runScoring();
            log.info("[manual] Full pipeline complete");
        });
        return ResponseEntity.accepted().body(Map.of("status", "Full pipeline (discovery → enrichment → scoring) started in background"));
    }
}
