package com.steam.cache.event.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.steam.cache.dto.ClassTypeSerialiableAdapter;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SteamCacheNotify<T> {
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(Class.class,new ClassTypeSerialiableAdapter()).create();
    private SteamCacheNotifyType notifyType;
    private String fromDomain;
    private String toDomain;
    private Class<T> cls;
    private String methodName;
    private String cacheKey;

    public SteamCacheNotify(){}

    public SteamCacheNotify(SteamCacheNotifyType notifyType, String fromDomain, String toDomain, Class cls, String methodName, String cacheKey){
        this.notifyType = notifyType;
        this.fromDomain = fromDomain;
        this.toDomain = toDomain;
        this.cls = cls;
        this.methodName = methodName;
        this.cacheKey = cacheKey;
    }

    public String toJsonString() {
        //return JSONObject.toJSONString(this);
        return gson.toJson(this);
    }
}
