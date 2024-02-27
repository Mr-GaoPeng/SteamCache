package com.steam.cache.competence;

import com.steam.cache.config.SteamRedisCacheConfiguration;
import com.steam.cache.dto.SteamCacheRedisTemplate;
import com.steam.cache.itf.ISteamCacheMDCompetence;
import com.steam.cache.util.CompletableFutureExpandUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;


@Component
public class SteamRedisCacheCompetence extends BaseRedisCacheCompetence implements ISteamCacheMDCompetence<SteamCacheRedisTemplate> {

    @Override
    public void put(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey, Object cacheValue) {
        if(cacheValue == null){cacheValue = ObjectUtils.NULL;}
        if( cacheRedisTemplate.getDuration() != null){
            cacheRedisTemplate.getRedisTemplate().opsForValue().set(cacheKey,cacheValue,cacheRedisTemplate.getDuration() + randomDuration(),cacheRedisTemplate.getTimeUnit());
        }else {
            cacheRedisTemplate.getRedisTemplate().opsForValue().set(cacheKey, cacheValue);
        }

        super.put(cacheRedisTemplate,cacheKey,cacheValue);
    }

    @Override
    public Object get(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey) {
        try{
            if(taskExecutor != null){
                CompletableFuture future = CompletableFutureExpandUtils.orTimeout(
                        CompletableFuture.supplyAsync(()-> cacheRedisTemplate.getRedisTemplate().opsForValue().get(cacheKey),taskExecutor),
                        SteamRedisCacheConfiguration.getTimeOut, SteamRedisCacheConfiguration.getTimeOutUnit);
                return future.get();
            }else{
                return cacheRedisTemplate.getRedisTemplate().opsForValue().get(cacheKey);
            }
        }catch (Exception e){//吞了异常，仅打印异常信息。出异常后就会走业务实现本身逻辑；
            logger.error(e.getMessage(),e);
            return null;
        }
    }

    @Override
    public Boolean containsKey(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey) {
        return  cacheRedisTemplate.getRedisTemplate().opsForValue().get(cacheKey) != null;
    }

}
