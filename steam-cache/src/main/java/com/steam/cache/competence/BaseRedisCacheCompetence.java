package com.steam.cache.competence;

import com.steam.cache.dto.SteamCacheRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class BaseRedisCacheCompetence extends AbstractRedisCacheCompetence{
    @Autowired
    @Qualifier("cacheTaskExecutor")
    protected Executor taskExecutor;

    @Override
    public Object get(SteamCacheRedisTemplate steamCacheRedisTemplate, String cacheKey) {
        return null;
    }

    @Override
    public Boolean containsKey(SteamCacheRedisTemplate steamCacheRedisTemplate, String cacheKey) {
        return null;
    }
}
