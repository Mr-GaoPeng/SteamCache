package com.steam.cache.itf;


/**
 * 缓存中间件的能力抽象接口
 */
public interface ISteamCacheMDCompetence<T> {

    void put(T t,String cacheKey,Object cacheValue);

    Object get(T t,String cacheKey);

    Boolean containsKey(T t,String cacheKey);

    void remove(T t,String cacheKey);

    void removeByPrefix(T t,String keyPrefix);

}
