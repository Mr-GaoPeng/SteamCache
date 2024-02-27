package com.steam.cache.competence;

import com.steam.cache.config.SteamRedisCacheConfiguration;
import com.steam.cache.dto.SteamCacheRedisTemplate;
import com.steam.cache.itf.ISteamCacheMDCompetence;
import com.steam.cache.itf.ISteamCacheMDExtCompetence;
import com.steam.cache.util.SteamCacheUtil;
import com.steam.cache.util.CompletableFutureExpandUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class SteamRedisMapCacheCompetence extends BaseRedisCacheCompetence implements ISteamCacheMDCompetence<SteamCacheRedisTemplate>, ISteamCacheMDExtCompetence<SteamCacheRedisTemplate> {

    @Override
    public void put(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey, Object cacheValue) {
        if(cacheValue == null){cacheValue = getNullValue();}
        Map collection = (Map) cacheValue;

        cacheRedisTemplate.getRedisTemplate().executePipelined(new SessionCallback<Object>() {
            @Override
            public  Object execute(RedisOperations operations) throws DataAccessException {
                operations.delete(cacheKey);
                operations.opsForHash().putAll(cacheKey, collection);
                if( cacheRedisTemplate.getDuration() != null){
                    operations.expire(cacheKey, cacheRedisTemplate.getDuration() + randomDuration(),cacheRedisTemplate.getTimeUnit());
                }
                return null;
            }
        });

        super.put(cacheRedisTemplate,cacheKey,cacheValue);
    }

    @Override
    public Object get(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey) {
        try{
            if(containsKey(cacheRedisTemplate,cacheKey)) {
                if (taskExecutor != null) {
                    CompletableFuture future = CompletableFutureExpandUtils.orTimeout(
                            CompletableFuture.supplyAsync(() -> cacheRedisTemplate.getRedisTemplate().opsForHash().entries(cacheKey), taskExecutor),
                            SteamRedisCacheConfiguration.getTimeOut, SteamRedisCacheConfiguration.getTimeOutUnit);
                    return future.get();
                } else {
                    return cacheRedisTemplate.getRedisTemplate().opsForHash().entries(cacheKey);
                }
            }else{
                return null;
            }
        }catch (Exception e){//吞了异常，仅打印异常信息。出异常后就会走业务实现本身逻辑；
            logger.error(e.getMessage(),e);
            
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public Boolean containsKey(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey) {
        return cacheRedisTemplate.getRedisTemplate().opsForHash().size(cacheKey) > 0;
    }



    /*****扩展能力*****/

    @Override
    public void incrementPut(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey, Object cacheValue) {
        if(containsKey(cacheRedisTemplate,cacheKey)) {
            Map collection = (Map) cacheValue;
            cacheRedisTemplate.getRedisTemplate().opsForHash().putAll(cacheKey,collection);
        }
    }

    @Override
    public void decrementRemove(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey, Object cacheValue) {
        if(containsKey(cacheRedisTemplate,cacheKey)) {
            Map collection = (Map) cacheValue;
            cacheRedisTemplate.getRedisTemplate().opsForHash().delete(cacheKey,collection.keySet().toArray());
        }
    }


    private Map getNullValue(){
        return new HashMap<String,Object>(){

            {
                put("$null",ObjectUtils.NULL);
            }

            @Override
            public int hashCode() {
                return SteamCacheUtil.EMPTY_MAP_HASHCODE;
            }
            

			@Override
			public boolean equals(Object o) {
				// TODO Auto-generated method stub
				return super.equals(o);
			}
        };
    }

    private Map[] splitObjects(Object cacheValue, int splitCount) {
        Map map = (Map) cacheValue;
        if(MapUtils.isNotEmpty(map) && splitCount > 1) {
            int splitSize = map.size() / splitCount + (map.size() % splitCount > 0 ? 1 : 0);
            Map[] mapArgs = new HashMap[splitCount];

            Map.Entry[] mapEntryArgs = (Map.Entry[]) map.entrySet().toArray(new Map.Entry[0]);
            for(int i=0;i<splitCount;i++){
                Map tmpMap = new HashMap();
                int tmp_i = splitSize * i;
                int tmp_len = splitSize * (i+1);
                for(int j=tmp_i;j<tmp_len;j++){
                    tmpMap.put(mapEntryArgs[i].getKey(),mapEntryArgs[i].getValue());
                }

                mapArgs[i] = tmpMap;
            }


            return mapArgs;
        }else{
            return new Map[]{map};
        }
    }
}
