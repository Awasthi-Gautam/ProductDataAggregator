package com.aggregation.data_aggregation.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class TrendLeaderboardRepository {

    private static final String NATIONAL_KEY   = "trends:IN";
    private static final String STATE_PREFIX   = "trends:IN:state:";
    private static final String CITY_PREFIX    = "trends:IN:city:";
    private static final String CATEGORY_PREFIX = "trends:IN:cat:";

    private final RedisTemplate<String, String> redisTemplate;

    public TrendLeaderboardRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void incrementScore(String productId, double increment) {
        redisTemplate.opsForZSet().incrementScore(NATIONAL_KEY, productId, increment);
    }

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
}
