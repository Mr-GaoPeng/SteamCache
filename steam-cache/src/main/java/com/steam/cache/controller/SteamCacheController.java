package com.steam.cache.controller;


import com.carrotsearch.sizeof.RamUsageEstimator;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.google.gson.Gson;
import com.steam.cache.event.util.SteamCacheEventUtil;
import com.steam.cache.util.SteamCacheUtil;
import org.apache.commons.collections4.MapUtils;
import org.openjdk.jol.info.GraphLayout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/common/cache")
public class SteamCacheController {

    private final Gson gson = new Gson();

    @Value("${spring.application.name:#{null}}")
    private String localDomain;
    @Value("${spring.profiles.active:#{null}}")
    private String profile;


    @RequestMapping(value = "/cleanMethodCache",method = RequestMethod.GET)
    @ResponseBody
    public String cleanMethodCache(@RequestParam String className, @RequestParam String methodName,@RequestParam String cacheKey){
        Assert.hasLength(className,"className can not be null");
        Assert.hasLength(methodName,"methodName can not be null");

        Class cls = null;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            Assert.notNull(cls,"can not get " + className + " from classloader");
        }

        SteamCacheEventUtil.publishCleanUpSteamCacheEvent(localDomain, SteamCacheUtil.SteamCacheIndex.DEFAULT_CACHE,cls,methodName,cacheKey);

        return "success";
    }

    @RequestMapping(value = "/statReport",method = RequestMethod.GET,produces = {"application/json"})
    @ResponseBody
    public String cacheStatReport(HttpServletResponse response) throws Exception {

        Field field = SteamCacheUtil.class.getDeclaredField("cacheIndexMap");
        ReflectionUtils.makeAccessible(field);
        Map<SteamCacheUtil.SteamCacheIndex,Object> cacheIndexMap = (Map<SteamCacheUtil.SteamCacheIndex,Object>)field.get(SteamCacheUtil.class);
        if(MapUtils.isNotEmpty(cacheIndexMap)){
            Map<String,Object> statMap = new HashMap<>();

            for(Map.Entry entry : cacheIndexMap.entrySet()){
                Object key = entry.getKey();
                if(key instanceof String){
                    Object value = entry.getValue();
                    if(value instanceof Cache){
                        Cache cache = (Cache)value;
                        CacheStats cacheStats = cache.stats();

                        Map<String,Object> itemStatMap = new HashMap<>();
                        itemStatMap.put("hitCount",cacheStats.hitCount());
                        itemStatMap.put("hitRate",cacheStats.hitRate());
                        //itemStatMap.put("evictionCount",cacheStats.evictionCount());
                        itemStatMap.put("totalCount",cache.asMap().size());
                        itemStatMap.put("totalSizeOf",RamUsageEstimator.humanReadableUnits(GraphLayout.parseInstance(cache.asMap()).totalSize()));

                        statMap.put(key.toString(),itemStatMap);
                    }
                }
            }

            return gson.toJson(statMap);
        }
        return null;
    }
}
