package com.steam.cache.dto;


import java.util.concurrent.TimeUnit;

public enum SteamCacheAttributeConstant {

    /* 暂时归属于Caffeine的配置 */
    maximum(Integer.TYPE,"500","the maximum size of the cache"),
    duration(Long.class,"5*60","the length of time after an entry is created that it should be automatically removed"),
    unit(TimeUnit.class,"SECONDS"," the unit that duration is expressed in");


    private Class type;
    private String defaultValue;
    private String describe;

    SteamCacheAttributeConstant(Class type, String defaultValue, String describe){
        this.type = type;
        this.defaultValue = defaultValue;
        this.describe = describe;
    }

    public Class getType(){
        return type;
    }

    public String getDefaultValue(){
        return defaultValue;
    }

    public String getDescribe(){
        return describe;
    }
}
