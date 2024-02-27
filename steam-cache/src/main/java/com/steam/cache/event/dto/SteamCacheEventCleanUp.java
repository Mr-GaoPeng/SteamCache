package com.steam.cache.event.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.lang.reflect.Method;

@Setter
@Getter
@ToString
public class SteamCacheEventCleanUp extends SteamCacheEvent {
    private String cacheKey;

    public SteamCacheEventCleanUp(Object source, Class cls, Method method, String cacheKey){
        super(source,cls,method);
        this.cacheKey = cacheKey;
    }
}
