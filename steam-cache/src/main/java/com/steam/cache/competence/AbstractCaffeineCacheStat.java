package com.steam.cache.competence;

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.github.benmanes.caffeine.cache.Cache;
import com.steam.cache.itf.ISteamCacheStat;
import org.apache.commons.collections4.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractCaffeineCacheStat implements ISteamCacheStat<Cache<String, Object>> {
    @Override
    public long totalCount(Cache<String, Object> cache) {
        return cache.asMap().keySet().size();
    }

    @Override
    public String totalSize(Cache<String, Object> cache) {
        Object value = cache.asMap();
        return RamUsageEstimator.humanSizeOf(value);
    }

    @Override
    public String humanSizeOf(Cache<String, Object> cache, String cacheKey) {
        Object value;
        if(cacheKey.endsWith("*")){
            String cacheKeyPrefix = cacheKey.replace("*","");//移除末尾的*
            List<String> filterKeys = cache .asMap().keySet().stream()
                    .filter(key->key.startsWith(cacheKeyPrefix))
                    .collect(Collectors.toList());
            Map<String,Object> filterCache = cache.getAllPresent(filterKeys);
            value = MapUtils.isNotEmpty(filterCache) ? filterCache.values() : null;
        }else{
            value = cache.getIfPresent(cacheKey);
        }

        if(value != null){
            return RamUsageEstimator.humanSizeOf(value);
        }
        return null;
    }

    private static final long obj_length = 10 * 1024 * 1024;//10M
    private static final long coll_length = 2000L;

    @Override
    public boolean isBigKey(Cache<String, Object> cache, String cacheKey, Object cacheVal) {
        if(cacheVal instanceof List){
            return ((List)cacheVal).size() > coll_length;
        }else if(cacheVal instanceof Map){
            return ((Map)cacheVal).size()> coll_length;
        }else{
            return RamUsageEstimator.sizeOf(cacheVal) > obj_length;
        }
    }
}
