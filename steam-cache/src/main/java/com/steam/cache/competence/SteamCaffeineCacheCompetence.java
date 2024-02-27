package com.steam.cache.competence;

import com.github.benmanes.caffeine.cache.Cache;
import com.steam.cache.itf.ISteamCacheMDCompetence;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


@Component
public class SteamCaffeineCacheCompetence extends AbstractCaffeineCacheStat implements ISteamCacheMDCompetence<Cache<String, Object>> {
    private Logger logger  = LoggerFactory.getLogger(this.getClass());

    @Override
    public void put(Cache<String, Object> cache, String cacheKey, Object cacheValue) {
        if(cacheValue == null){cacheValue = ObjectUtils.NULL;}
        cache.put(cacheKey,cacheValue);
    }

    @Override
    public Object get(Cache<String, Object> cache, String cacheKey) {
        try{
            return cache.getIfPresent(cacheKey);
        }catch (Exception e){//吞了异常，仅打印异常信息。出异常后就会走业务实现本身逻辑；
            logger.error(e.getMessage(),e);
            return null;
        }
    }

    @Override
    public Boolean containsKey(Cache<String, Object> cache, String cacheKey) {
        return cache.getIfPresent(cacheKey) != null;
    }

    @Override
    public void remove(Cache<String, Object> cache, String cacheKey) {
        cache.invalidate(cacheKey);
    }

    @Override
    public void removeByPrefix(Cache<String, Object> cache, String keyPrefix) {
        List<String> filterKeys = cache.asMap().keySet().stream()
                .filter(key->key.startsWith(keyPrefix))
                .collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(filterKeys)){
            cache.invalidateAll(filterKeys);
        }
    }
}
