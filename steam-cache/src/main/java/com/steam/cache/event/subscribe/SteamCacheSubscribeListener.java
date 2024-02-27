package com.steam.cache.event.subscribe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.steam.cache.dto.ClassTypeSerialiableAdapter;
import com.steam.cache.event.dto.SteamCacheNotify;
import com.steam.cache.event.dto.SteamCacheNotifyType;
import com.steam.cache.util.SteamCacheHutoolUtil;
import com.steam.cache.util.SteamCacheSpringUtil;
import com.steam.cache.util.CacheYmsPropertyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * 基于redis publish/subscribe 模式实现消息订阅接收方
 */
@Component
@ConditionalOnExpression("#{'RedisPubSub'.equals('${steamCache.notify.type:}')}")
public class SteamCacheSubscribeListener {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String YONDIF_DOMAIN = "yondif";
    private static final String YONDIF_AMS_COMMON_DOMAIN = "yondif-ams-common";
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(Class.class,new ClassTypeSerialiableAdapter()).create();

    @Value("${spring.application.name:''}")
    private String localDomain;

    public void onMessage(String message, String pattern) {
        logger.info("=======>SteamCacheSubscribeListener==========>");
        SteamCacheNotify cacheNotify = gson.fromJson(message, SteamCacheNotify.class);
        //判断领域不符合的跳过
        if(cacheNotify == null){return;}

        if(YONDIF_DOMAIN.equals(cacheNotify.getToDomain())){
            // YONDIF_DOMAIN 领域，验证fromDomain和accesskey
            if(!YONDIF_AMS_COMMON_DOMAIN.equals(cacheNotify.getFromDomain())){
                return;
            }
            String rsaContent = CacheYmsPropertyUtil.getProperty("commonCache.ciphertext",false);//密文
            if(StringUtils.isEmpty(rsaContent) || !SteamCacheHutoolUtil.verifyRSAContent(rsaContent)){
                return;
            }
        }else if(localDomain.equals(cacheNotify.getToDomain())){
            //触达消息响应服务域
        }else{
            //非以上两种情况，当前服务域不继续响应事件通知；
            return;
        }


        SteamCacheNotifyType notifyType = cacheNotify.getNotifyType();
        if(notifyType == null){
            logger.warn("SteamCacheNotifyType is be null, skip!!!");
            return;
        }

        switch (notifyType){
            case DELETE:
                SteamCacheSpringUtil.publishCleanUpSteamCacheEvent(cacheNotify.getCls(),cacheNotify.getMethodName(),cacheNotify.getCacheKey());
                break;
            default:
                logger.info("notifyType can not be favour");
                break;
        }

        logger.info("=======>SteamCacheSubscribeListener End==========>");
    }
}
