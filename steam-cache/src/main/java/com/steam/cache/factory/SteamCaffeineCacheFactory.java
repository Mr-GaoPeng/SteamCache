package com.steam.cache.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.steam.cache.annotation.SteamCache;
import com.steam.cache.dto.SteamCacheAttributeConstant;
import com.steam.cache.itf.ISteamCacheMDFactory;
import com.steam.cache.util.SteamCacheUtil;
import com.steam.cache.util.AppMathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * caffeine构建工厂，通过传递不通的属性设置，构建不同的缓存集合；
 * 工厂初始化时，不默认类型，在具体执行动作（放缓存、取缓存）操作时，由不同类型的能力接口实现
 *
 */
@Component("steamCaffeineCacheFactory")
public class SteamCaffeineCacheFactory implements ISteamCacheMDFactory<Cache> {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    @SteamCache(cacheIndex = SteamCacheUtil.SteamCacheIndex.CACHE_CAHCE,cacheKeyPrefix = "middleware:caffeine")
    public Cache build(Map<SteamCacheAttributeConstant,String> attributeMap) {
        logger.info("=====> SteamCaffeineCacheFactory build...========>");
        if(attributeMap == null || CollectionUtils.isEmpty(attributeMap.keySet())){
            //未指定设置属性时，默认构造一个：数量500，5分钟的缓存集合；
            Cache cache = Caffeine.newBuilder()
                    .expireAfterWrite(5*60, TimeUnit.SECONDS)
                    .maximumSize(500)
                    .recordStats()
                    .build();
            return cache;
        }


        Caffeine caffeine = Caffeine.newBuilder();
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
            caffeine.expireAfterWrite(Long.parseLong(strDuration),timeUnit);
        }

        if(attributeMap.containsKey(SteamCacheAttributeConstant.maximum)){
            String strMaxinum = attributeMap.get(SteamCacheAttributeConstant.maximum);
            caffeine.maximumSize(Integer.parseInt(strMaxinum));
        }
        caffeine.recordStats();
        logger.info("=====> SteamCaffeineCacheFactory build...   End ========>");
        return caffeine.build();
    }
}
