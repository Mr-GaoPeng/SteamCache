package com.steam.cache.event.subscribe;

import com.steam.cache.event.dto.SteamCacheEventCleanUp;
import com.steam.cache.itf.ISteamCache;
import com.steam.cache.util.SteamCacheCleanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 基于Spring事件通知机制；
 * 容器级别的消息接收方；
 */
@Component
public class SteamCacheEventListener implements ApplicationListener<SteamCacheEventCleanUp> {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Override
    public void onApplicationEvent(SteamCacheEventCleanUp event) {
        Assert.notNull(event,"event can not be null");
        Assert.notNull(event.getMethod(),"event.getMethod can not be null");

        logger.info("======>receive SteamCacheEventCleanUp======>");
        Object source = event.getSource();
        if(source != null && source instanceof ISteamCache){
            ISteamCache steamCacheBean = (ISteamCache)source;
            SteamCacheCleanUtil.cleanUp(steamCacheBean,event.getMethod(),event.getCacheKey());
        }else{
            logger.warn("====>can not be favour=====>");
        }
        logger.info("======>receive SteamCacheEventCleanUp end======>");
    }
}
