package com.steam.cache.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Data
@Builder
public class SteamCacheRedisTemplate {
    private RedisTemplate redisTemplate;
    private Long duration;
    private TimeUnit timeUnit;
}
