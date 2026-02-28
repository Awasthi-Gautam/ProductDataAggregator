package com.aggregation.data_aggregation.controller;

import com.aggregation.data_aggregation.dto.TrendingProductDto;
import com.aggregation.data_aggregation.model.entity.Product;
import com.aggregation.data_aggregation.repository.ProductRepository;
import com.aggregation.data_aggregation.repository.TrendLeaderboardRepository;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Returns trending products from the Redis leaderboard, enriched with product metadata.
 *
 * GET /api/trending                       — national top N
 * GET /api/trending/category/{category}   — top N in a ProductCategory (e.g. ELECTRONICS_GADGETS)
 * GET /api/trending/state/{state}         — top N in an Indian state (e.g. Maharashtra)
 * GET /api/trending/city/{city}           — top N in a city (e.g. Mumbai)
 */
@RestController
@RequestMapping("/api/trending")
public class TrendController {

    private final TrendLeaderboardRepository leaderboard;
    private final ProductRepository productRepository;

    public TrendController(TrendLeaderboardRepository leaderboard, ProductRepository productRepository) {
        this.leaderboard = leaderboard;
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<TrendingProductDto> getTopNational(@RequestParam(defaultValue = "20") int limit) {
        return toDto(leaderboard.getTopNational(limit));
    }

    @GetMapping("/category/{category}")
    public List<TrendingProductDto> getTopByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "20") int limit) {
        return toDto(leaderboard.getTopByCategory(category.toUpperCase(), limit));
    }

    @GetMapping("/state/{state}")
    public List<TrendingProductDto> getTopByState(
            @PathVariable String state,
            @RequestParam(defaultValue = "20") int limit) {
        return toDto(leaderboard.getTopByState(state, limit));
    }

    @GetMapping("/city/{city}")
    public List<TrendingProductDto> getTopByCity(
            @PathVariable String city,
            @RequestParam(defaultValue = "20") int limit) {
        return toDto(leaderboard.getTopByCity(city, limit));
    }

    private List<TrendingProductDto> toDto(Set<ZSetOperations.TypedTuple<String>> tuples) {
        if (tuples == null || tuples.isEmpty()) return List.of();

        List<TrendingProductDto> result = new ArrayList<>();
        long rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String productId = tuple.getValue();
            double score = tuple.getScore() != null ? tuple.getScore() : 0.0;
            if (productId == null) continue;

            Optional<Product> opt = productRepository.findById(productId);
            if (opt.isEmpty()) continue;

            Product p = opt.get();
            result.add(new TrendingProductDto(
                rank++,
                p.getId(),
                p.getName(),
                p.getBrand(),
                p.getCategory() != null ? p.getCategory().name() : null,
                p.getImageUrl(),
                score
            ));
        }
        return result;
    }
}
