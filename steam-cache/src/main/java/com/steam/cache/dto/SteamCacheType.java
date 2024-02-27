package com.steam.cache.dto;


import com.steam.cache.competence.SteamCaffeineCacheCompetence;
import com.steam.cache.competence.SteamRedisCacheCompetence;
import com.steam.cache.competence.SteamRedisListCacheCompetence;
import com.steam.cache.competence.SteamRedisMapCacheCompetence;
import com.steam.cache.factory.SteamCaffeineCacheFactory;
import com.steam.cache.factory.SteamRedisCacheFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存类型，描述一些中间件和工厂bean的关系；
 *
 * 后续会扩大支持类型：
 *
 * Mongodb
 * YmsSession
 * Mongodb
 * ElasticSearch
 *
 */
public enum SteamCacheType {
    Caffeine(SteamCaffeineCacheFactory.class,new HashMap<String,Class>(){
        {
            put("String", SteamCaffeineCacheCompetence.class);
            //put("List", AppCaffeineCacheCompetence.class);
            //put("Map", AppCaffeineCacheCompetence.class);
        }
    }),
    Redis(SteamRedisCacheFactory.class,new HashMap<String,Class>(){
        {
            put("String", SteamRedisCacheCompetence.class);
            put("List", SteamRedisListCacheCompetence.class);
            put("Map", SteamRedisMapCacheCompetence.class);
        }
    }),
    Custom(null,null);

    private Class factoryCls;//工厂bean
    private Map<String,Class> competenceClsMap;//能力bean
    SteamCacheType(Class factoryCls, Map<String,Class> competenceClsMap){
        this.factoryCls = factoryCls;
        this.competenceClsMap = competenceClsMap;
    }

    public Class getFactoryClass() {
        return factoryCls;
    }

    public Map<String,Class> getCompetenceClassMap() {
        return competenceClsMap;
    }

    public Class getCompetenceClass(String competenceClsKey){
        if(competenceClsMap.containsKey(competenceClsKey)) {
            return competenceClsMap.get(competenceClsKey);
        }else{
            return competenceClsMap.get("String");
        }
    }
}
