package com.steam.cache.executor;

import com.steam.cache.annotation.SteamCache;
import com.steam.cache.itf.ISteamCacheMDCompetence;
import com.steam.cache.util.SteamCacheUtil;
import org.apache.commons.collections4.MapUtils;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class PropagateExecutorActionHandler extends AbstractExecutorActionHandler<Object>{
    public PropagateExecutorActionHandler(AbstractExecutor abstractExecutor) {
        super(abstractExecutor);
    }

    @Override
    Object execute(SteamCache steamCache, Class targetCls, Method method, Object... methodArgs) throws Exception {
        LinkedHashMap<Object, ISteamCacheMDCompetence> competenceBeanMap = abstractExecutor.tl_handlerInfo.get().getMethodCompetenceBeanMap().get(method.hashCode());
        if(MapUtils.isNotEmpty(competenceBeanMap) && competenceBeanMap.size() > 1){
            String cacheKey = SteamCacheUtil.getMethodCacheKey(steamCache,targetCls,method,methodArgs);
            //todo 考虑传播层级的限制，防止层与层传播中发生数据污染
            for (Map.Entry<Object, ISteamCacheMDCompetence> entry : competenceBeanMap.entrySet()) {
                if(!entry.getValue().containsKey(entry.getKey(),cacheKey)){
                    entry.getValue().put(entry.getKey(),cacheKey,abstractExecutor.tl_handlerInfo.get().getCacheValue());
                }
            }
        }
        return null;
    }
}
