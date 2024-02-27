package com.steam.cache.dto;

import com.steam.cache.annotation.SteamCacheConfig;
import com.steam.cache.annotation.SteamCacheConfigAttribute;
import com.steam.cache.annotation.SteamCacheConfigGroup;

/**
 * 缓存组成
 *
 * 定义缓存配置：
 * 可通过@SteamCacheConfig注解定义单个组件的缓存层，
 * 也可通过@SteamCacheConfigGroup注解定义多个组件的缓存层。
 *      注意数组内元素的顺序；按照元素下标进行缓存能力的顺序调用；
 */
public enum SteamCacheCompose {
    @SteamCacheConfig(cacheType = SteamCacheType.Caffeine ,attributes = {
            @SteamCacheConfigAttribute(code = SteamCacheAttributeConstant.maximum,value = "10000"),
            @SteamCacheConfigAttribute(code = SteamCacheAttributeConstant.duration,value = "5*60"),
    },valueCopyFlag = true,startedInit = true)
    Level_1,

    @SteamCacheConfigGroup(configGroup = {
            @SteamCacheConfig(cacheType = SteamCacheType.Caffeine ,attributes = {
                    @SteamCacheConfigAttribute(code = SteamCacheAttributeConstant.maximum,value = "5000"),
                    @SteamCacheConfigAttribute(code = SteamCacheAttributeConstant.duration,value = "15*60"),
            },valueCopyFlag = true,startedInit = true),
            @SteamCacheConfig(cacheType = SteamCacheType.Redis ,attributes = {
                    @SteamCacheConfigAttribute(code = SteamCacheAttributeConstant.duration,value = "30*60")
            },startedInit = true)
    },propagation = true)
    Level_2,

    @SteamCacheConfig(cacheType = SteamCacheType.Redis ,attributes = {
            @SteamCacheConfigAttribute(code = SteamCacheAttributeConstant.duration,value = "2*30*60")
    })
    Level_1_Redis,

    @SteamCacheConfig(cacheType = SteamCacheType.Redis ,attributes = {
            @SteamCacheConfigAttribute(code = SteamCacheAttributeConstant.duration,value = "12*60*60")
    },startedInit = true)
    Level_1_Long_Redis;
}
