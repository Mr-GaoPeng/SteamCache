package com.steam.cache.executor;

import com.steam.cache.annotation.SteamCache;
import com.steam.cache.itf.ISteamCacheMDCompetence;
import com.steam.cache.util.SteamCacheUtil;
import org.apache.commons.collections4.MapUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class PutExecutorActionHandler extends AbstractExecutorActionHandler<Object> {

    public PutExecutorActionHandler(AbstractExecutor abstractExecutor) {
        super(abstractExecutor);
    }

    @Override
    public Object execute(SteamCache steamCache, Class targetCls, Method method, Object... methodArgs) throws Exception{
        LinkedHashMap<Object, ISteamCacheMDCompetence> competenceBeanMap = abstractExecutor.tl_handlerInfo.get().getMethodCompetenceBeanMap().get(method.hashCode());

        Object methodObj = abstractExecutor.tl_handlerInfo.get().methodInvokeValue;
        if(MapUtils.isNotEmpty(competenceBeanMap)){
            if(!checkObjectIsNull(steamCache,method,methodObj) || steamCache.cacheNullable()) {
                //值不为空 或者 配置'可缓存空值'
                String cacheKey = SteamCacheUtil.getMethodCacheKey(steamCache, targetCls, method, methodArgs);
                for (Map.Entry<Object, ISteamCacheMDCompetence> entry : competenceBeanMap.entrySet()) {
                    entry.getValue().put(entry.getKey(), cacheKey, methodObj);
                }
            }
        }
        return null;
    }


    private boolean checkObjectIsNull(SteamCache steamCache, Method method, Object methodObj){
        if(methodObj == null){return true;}

        String cacheType = SteamCacheUtil.getMethodUsedCacheType(steamCache,method);

        switch (cacheType){
            case "List":
                return ((Collection)methodObj) == null || ((Collection)methodObj).isEmpty();
            case "Map":
                return ((Map)methodObj) == null || ((Map)methodObj).isEmpty();
            default:
                break;
        }

        return false;
    }
}
