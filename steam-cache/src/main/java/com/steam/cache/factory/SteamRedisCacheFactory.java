package com.steam.cache.factory;

import com.steam.cache.annotation.SteamCache;
import com.steam.cache.dto.SteamCacheAttributeConstant;
import com.steam.cache.dto.SteamCacheRedisTemplate;
import com.steam.cache.itf.ISteamCacheMDFactory;
import com.steam.cache.util.SteamCacheUtil;
import com.steam.cache.util.AppMathUtil;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * redis构建工程
 *
 */
@Component("steamRedisCacheFactory")
public class SteamRedisCacheFactory implements ISteamCacheMDFactory<SteamCacheRedisTemplate> {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    @SteamCache(cacheIndex = SteamCacheUtil.SteamCacheIndex.CACHE_CAHCE,cacheKeyPrefix = "middleware:redis")
    public SteamCacheRedisTemplate build(Map<SteamCacheAttributeConstant, String> attributeMap) {
        logger.info("=====> SteamRedisCacheFactory build...========>");
        SteamCacheRedisTemplate.SteamCacheRedisTemplateBuilder builder = SteamCacheRedisTemplate.builder().redisTemplate(redisTemplate);

        if(MapUtils.isNotEmpty(attributeMap)){
            //存在配置，主要维护超时时间
            if(attributeMap.containsKey(SteamCacheAttributeConstant.duration)){
                String strDuration = attributeMap.get(SteamCacheAttributeConstant.duration);
                if(strDuration.contains("*")){
                    String[] splitArgs = strDuration.split("\\*");
                    strDuration = AppMathUtil.multiply(splitArgs);
                }
                TimeUnit timeUnit = TimeUnit.SECONDS;
                if(attributeMap.containsKey(SteamCacheAttributeConstant.unit)){
                    timeUnit = Enum.valueOf(TimeUnit.class,attributeMap.get(SteamCacheAttributeConstant.unit));
                }
                builder.duration(Long.parseLong(strDuration)).timeUnit(timeUnit);
            }
        }
        logger.info("=====> SteamRedisCacheFactory build... End ========>");
        return builder.build();
    }
}
