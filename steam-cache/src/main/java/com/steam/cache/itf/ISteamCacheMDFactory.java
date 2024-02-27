package com.steam.cache.itf;

import com.steam.cache.dto.SteamCacheAttributeConstant;

import java.util.Map;

public interface ISteamCacheMDFactory<T> {
    T build(Map<SteamCacheAttributeConstant,String> attributeMap);
}
