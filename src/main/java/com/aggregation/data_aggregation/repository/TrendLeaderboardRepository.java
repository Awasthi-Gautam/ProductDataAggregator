package com.aggregation.data_aggregation.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * Two leaderboards in Redis:
 *
 *  Popularity (cumulative, incrementing ZSets):
 *    trends:IN              — national all-time weighted score
 *    trends:IN:state:{s}    — by Indian state
 *    trends:IN:city:{c}     — by city
 *    trends:IN:cat:{cat}    — by product category
 *
 *  Velocity (absolute, overwritten each cycle):
 *    trends:IN:velocity          — national rate-of-change
 *    trends:IN:velocity:cat:{c}  — category rate-of-change
 *
 * Popularity answers "what consistently sells well".
 * Velocity answers "what is rising RIGHT NOW".
 */
@Repository
public class TrendLeaderboardRepository {

    // ── Popularity keys (cumulative) ──────────────────────────────────────
    private static final String NATIONAL_KEY      = "trends:IN";
    private static final String STATE_PREFIX      = "trends:IN:state:";
    private static final String CITY_PREFIX       = "trends:IN:city:";
    private static final String CATEGORY_PREFIX   = "trends:IN:cat:";

    // ── Velocity keys (absolute snapshot, overwritten each cycle) ─────────
    private static final String VELOCITY_KEY      = "trends:IN:velocity";
    private static final String VELOCITY_CAT_PREFIX = "trends:IN:velocity:cat:";

    private final RedisTemplate<String, String> redisTemplate;

    public TrendLeaderboardRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ── Popularity writes ─────────────────────────────────────────────────

    public void incrementScore(String productId, double increment, String state, String city) {
        redisTemplate.opsForZSet().incrementScore(NATIONAL_KEY, productId, increment);
        if (state != null) {
            redisTemplate.opsForZSet().incrementScore(STATE_PREFIX + state, productId, increment);
        }
        if (city != null) {
            redisTemplate.opsForZSet().incrementScore(CITY_PREFIX + city, productId, increment);
        }
    }

    public void incrementCategoryScore(String productId, String category, double increment) {
        redisTemplate.opsForZSet().incrementScore(CATEGORY_PREFIX + category, productId, increment);
    }

    // ── Velocity writes (ZSet.add overwrites the previous value) ──────────

    public void setVelocityScore(String productId, double velocity, String category) {
        redisTemplate.opsForZSet().add(VELOCITY_KEY, productId, velocity);
        if (category != null) {
            redisTemplate.opsForZSet().add(VELOCITY_CAT_PREFIX + category, productId, velocity);
        }
    }

    // ── Popularity reads ──────────────────────────────────────────────────

    public Set<ZSetOperations.TypedTuple<String>> getTopNational(int count) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(NATIONAL_KEY, 0, count - 1);
    }

    public Set<ZSetOperations.TypedTuple<String>> getTopByState(String state, int count) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(STATE_PREFIX + state, 0, count - 1);
    }

    public Set<ZSetOperations.TypedTuple<String>> getTopByCity(String city, int count) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(CITY_PREFIX + city, 0, count - 1);
    }

    public Set<ZSetOperations.TypedTuple<String>> getTopByCategory(String category, int count) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(CATEGORY_PREFIX + category, 0, count - 1);
    }

    public Double getScore(String productId) {
        return redisTemplate.opsForZSet().score(NATIONAL_KEY, productId);
    }

    // ── Velocity reads ────────────────────────────────────────────────────

    public Set<ZSetOperations.TypedTuple<String>> getTopVelocityNational(int count) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(VELOCITY_KEY, 0, count - 1);
    }

    public Set<ZSetOperations.TypedTuple<String>> getTopVelocityByCategory(String category, int count) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(VELOCITY_CAT_PREFIX + category, 0, count - 1);
    }

    public Double getVelocityScore(String productId) {
        return redisTemplate.opsForZSet().score(VELOCITY_KEY, productId);
    }
}
