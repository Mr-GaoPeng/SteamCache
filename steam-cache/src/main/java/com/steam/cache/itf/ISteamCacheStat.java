package com.steam.cache.itf;

public interface ISteamCacheStat<T> {
    long totalCount(T t);

    String totalSize(T t);

    String humanSizeOf(T t , String cacheKey);

    boolean isBigKey(T t,String cacheKey,Object cacheVal);
}
