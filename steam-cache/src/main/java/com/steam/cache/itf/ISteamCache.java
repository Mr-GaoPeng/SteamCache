package com.steam.cache.itf;


import com.steam.cache.util.SteamCacheUtil;

public interface ISteamCache {

    default SteamCacheUtil.SteamCacheIndex[] cacheIndexs(){
        return new SteamCacheUtil.SteamCacheIndex[]{SteamCacheUtil.SteamCacheIndex.DEFAULT_CACHE};
    }
}
