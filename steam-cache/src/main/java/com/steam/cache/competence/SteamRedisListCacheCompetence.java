package com.steam.cache.competence;

import cn.hutool.core.collection.CollectionUtil;
import com.steam.cache.config.SteamRedisCacheConfiguration;
import com.steam.cache.dto.SteamCacheRedisTemplate;
import com.steam.cache.itf.ISteamCacheMDCompetence;
import com.steam.cache.itf.ISteamCacheMDExtCompetence;
import com.steam.cache.util.SteamCacheUtil;
import com.steam.cache.util.CompletableFutureExpandUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class SteamRedisListCacheCompetence extends BaseRedisCacheCompetence implements ISteamCacheMDCompetence<SteamCacheRedisTemplate>, ISteamCacheMDExtCompetence<SteamCacheRedisTemplate> {

    @Override
    public void put(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey, Object cacheValue) {
        if(cacheValue == null){cacheValue = getNullValue();}

        Collection collection = (Collection) cacheValue;
        cacheRedisTemplate.getRedisTemplate().executePipelined(new SessionCallback<Object>() {
            @Override
            public  Object execute(RedisOperations operations) throws DataAccessException {
                operations.delete(cacheKey);
                operations.opsForList().rightPushAll(cacheKey, collection);
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
                            CompletableFuture.supplyAsync(() -> cacheRedisTemplate.getRedisTemplate().opsForList().range(cacheKey, 0, -1), taskExecutor),
                            SteamRedisCacheConfiguration.getTimeOut, SteamRedisCacheConfiguration.getTimeOutUnit);
                    return future.get();
                } else {
                    return cacheRedisTemplate.getRedisTemplate().opsForList().range(cacheKey, 0, -1);
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
        return cacheRedisTemplate.getRedisTemplate().opsForList().size(cacheKey) > 0;
    }



    /*****扩展能力*****/

    @Override
    public void incrementPut(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey, Object cacheValue) {
        //todo 需考虑多个缓存匹配更新的问题；
        if(containsKey(cacheRedisTemplate,cacheKey)) {
            String outVerCacheKey = SteamCacheUtil.getCacheKeyWithOutVersion(cacheKey);//不带version的缓存Key
            String versionCatalogKey = SteamCacheUtil.getCacheVersionKey();//版本目录key

            cacheRedisTemplate.getRedisTemplate().executePipelined(new RedisCallback<String>() {
                @Override
                public String doInRedis(RedisConnection connection) throws DataAccessException {
                    cacheRedisTemplate.getRedisTemplate().opsForList().rightPush(cacheKey,cacheValue);
                    Long incrementVal = cacheRedisTemplate.getRedisTemplate().opsForHash().increment(versionCatalogKey,outVerCacheKey,1);//根据初始缓存key找到键值自增；
                    String newCacheKey = SteamCacheUtil.getNewVersionCacheKey(cacheKey,incrementVal);
                    cacheRedisTemplate.getRedisTemplate().rename(cacheKey,newCacheKey);
                    return newCacheKey;
                }
            });


        }
    }

    @Override
    public void decrementRemove(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey, Object cacheValue) {
        if(containsKey(cacheRedisTemplate,cacheKey)) {
            String outVerCacheKey = SteamCacheUtil.getCacheKeyWithOutVersion(cacheKey);//不带version的缓存Key
            String versionCatalogKey = SteamCacheUtil.getCacheVersionKey();//版本目录key

            cacheRedisTemplate.getRedisTemplate().executePipelined(new RedisCallback<String>() {
                @Override
                public String doInRedis(RedisConnection connection) throws DataAccessException {
                    cacheRedisTemplate.getRedisTemplate().opsForList().remove(cacheKey,0,cacheValue);// 移除所有值为cacheValue的元素
                    Long incrementVal = cacheRedisTemplate.getRedisTemplate().opsForHash().increment(versionCatalogKey,outVerCacheKey,1);//根据初始缓存key找到键值自增；
                    String newCacheKey = SteamCacheUtil.getNewVersionCacheKey(cacheKey,incrementVal);
                    cacheRedisTemplate.getRedisTemplate().rename(cacheKey,newCacheKey);
                    return newCacheKey;
                }
            });
        }
    }


    private Collection getNullValue(){
        return new ArrayList<Object>(){

            {
                add(ObjectUtils.NULL);
            }

            @Override
            public int hashCode() {
                return SteamCacheUtil.EMPTY_LIST_HASHCODE;
            }

			@Override
			public boolean equals(Object o) {
				// TODO Auto-generated method stub
				return super.equals(o);
			}

			
            
            
        };
    }


    private Collection[] splitObjects(Object cacheValue, int splitCount) {
        Collection collection = (Collection) cacheValue;
        if(CollectionUtils.isNotEmpty(collection) && splitCount > 1) {
            int splitSize = collection.size() / splitCount + (collection.size() % splitCount > 0 ? 1 : 0);
            List<List> splitList = CollectionUtil.split(collection, splitSize);
            return splitList.toArray(new Collection[0]);
        }else{
            return new Collection[]{collection};
        }
    }

    private Collection unionObject(List targetObject, List cacheValue) {
        if (targetObject != null){
            Collection collection = (Collection) targetObject;
            collection.addAll((Collection) cacheValue);
            return collection;
        }else{
            return (Collection) cacheValue;
        }
    }
}
