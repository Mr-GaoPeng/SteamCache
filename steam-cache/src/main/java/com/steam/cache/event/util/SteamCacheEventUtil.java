package com.steam.cache.event.util;

import com.steam.cache.event.dto.SteamCacheNotify;
import com.steam.cache.event.dto.SteamCacheNotifyType;
import com.steam.cache.util.SteamCacheSpringUtil;
import com.steam.cache.util.SteamCacheUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SteamCacheEventUtil {
    private static String localDomain;
    private static RedisTemplate pubRedisTemplate;

    @Value("${spring.application.name}")
    public void setLocalDomain(String domain){
        SteamCacheEventUtil.localDomain = domain;
    }

    @Autowired(required = false)
    @Qualifier("pubRedisTemplate")
    public void setPubRedisTemplate(RedisTemplate pubRedisTemplate){
        SteamCacheEventUtil.pubRedisTemplate = pubRedisTemplate;
    }


    /**
     * 清除方法缓存
     * @param toDomain  目标服务域；目标域为空时，降级为容器服务级别清缓存；
     * @param cacheIndex    缓存索引、通道
     * @param cls       类名
     * @param methodName    方法名
     * @param cacheKey  方法缓存键全名
     */
    public static void publishCleanUpSteamCacheEvent(String toDomain, SteamCacheUtil.SteamCacheIndex cacheIndex, Class cls, String methodName, String cacheKey){
        if(StringUtils.isNotEmpty(toDomain) && pubRedisTemplate != null){
            //走微服务级别的消息发送
            SteamCacheNotify cacheNotify = SteamCacheNotify.builder()
                    .notifyType(SteamCacheNotifyType.DELETE)
                    .fromDomain(localDomain)
                    .toDomain(toDomain)
                    .cls(cls)
                    .methodName(methodName)
                    .cacheKey(cacheKey).build();
            pubRedisTemplate.convertAndSend(cacheIndex.getCode(), cacheNotify.toJsonString());
        }else {
            //容器内的消息发送
            SteamCacheSpringUtil.publishCleanUpSteamCacheEvent(cls, methodName, cacheKey);
        }
    }
}
