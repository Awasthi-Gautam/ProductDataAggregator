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
 * Two leaderboard endpoints:
 *
 *  /api/trending  — sorted by VELOCITY (rate of change).
 *                   Shows what is rising RIGHT NOW. A product with a high
 *                   velocity jumped up the charts this cycle.
 *
 *  /api/popular   — sorted by cumulative POPULARITY score.
 *                   Shows what has been consistently strong over time.
 *
 * Both support optional ?limit= and can be filtered by category.
 * /api/popular also supports state/city filters (location data is only
 * available from discovery sources, not social enrichment).
 */
@RestController
@RequestMapping("/api")
public class TrendController {

    private final TrendLeaderboardRepository leaderboard;
    private final ProductRepository productRepository;

    public TrendController(TrendLeaderboardRepository leaderboard, ProductRepository productRepository) {
        this.leaderboard = leaderboard;
        this.productRepository = productRepository;
    }

    // ── Trending (velocity) ───────────────────────────────────────────────

    @GetMapping("/trending")
    public List<TrendingProductDto> getTrending(@RequestParam(defaultValue = "20") int limit) {
        return toDto(leaderboard.getTopVelocityNational(limit), true);
    }

    @GetMapping("/trending/category/{category}")
    public List<TrendingProductDto> getTrendingByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "20") int limit) {
        return toDto(leaderboard.getTopVelocityByCategory(category.toUpperCase(), limit), true);
    }

    // ── Popular (cumulative score) ────────────────────────────────────────

    @GetMapping("/popular")
    public List<TrendingProductDto> getPopular(@RequestParam(defaultValue = "20") int limit) {
        return toDto(leaderboard.getTopNational(limit), false);
    }

    @GetMapping("/popular/category/{category}")
    public List<TrendingProductDto> getPopularByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "20") int limit) {
        return toDto(leaderboard.getTopByCategory(category.toUpperCase(), limit), false);
    }

    @GetMapping("/popular/state/{state}")
    public List<TrendingProductDto> getPopularByState(
            @PathVariable String state,
            @RequestParam(defaultValue = "20") int limit) {
        return toDto(leaderboard.getTopByState(state, limit), false);
    }

    @GetMapping("/popular/city/{city}")
    public List<TrendingProductDto> getPopularByCity(
            @PathVariable String city,
            @RequestParam(defaultValue = "20") int limit) {
        return toDto(leaderboard.getTopByCity(city, limit), false);
    }

    // ── Mapper ────────────────────────────────────────────────────────────

    /**
     * Converts a Redis ZSet result to DTOs, looking up the complementary score
     * (velocity or popularity) for each product from the other ZSet.
     *
     * @param velocityFirst  true → primary score is velocity, fetch popularity as secondary
     *                       false → primary score is popularity, fetch velocity as secondary
     */
    private List<TrendingProductDto> toDto(
            Set<ZSetOperations.TypedTuple<String>> tuples, boolean velocityFirst) {

        if (tuples == null || tuples.isEmpty()) return List.of();

        List<TrendingProductDto> result = new ArrayList<>();
        long rank = 1;

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String productId = tuple.getValue();
            double primaryScore = tuple.getScore() != null ? tuple.getScore() : 0.0;
            if (productId == null) continue;

            Optional<Product> opt = productRepository.findById(productId);
            if (opt.isEmpty()) continue;
            Product p = opt.get();

            double popularityScore;
            Double velocityScore;

            if (velocityFirst) {
                velocityScore   = primaryScore;
                popularityScore = orZero(leaderboard.getScore(productId));
            } else {
                popularityScore = primaryScore;
                velocityScore   = leaderboard.getVelocityScore(productId);
            }

            result.add(new TrendingProductDto(
                rank++,
                p.getId(),
                p.getName(),
                p.getBrand(),
                p.getCategory() != null ? p.getCategory().name() : null,
                p.getImageUrl(),
                popularityScore,
                velocityScore
            ));
        }
        return result;
    }

    private double orZero(Double d) { return d != null ? d : 0.0; }
}
