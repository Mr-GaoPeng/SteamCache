package com.steam.cache.config;

import com.steam.cache.annotation.SteamCacheConfig;
import com.steam.cache.annotation.SteamCacheConfigAttribute;
import com.steam.cache.annotation.SteamCacheConfigGroup;
import com.steam.cache.dto.SteamCacheAttributeConstant;
import com.steam.cache.dto.SteamCacheCompose;
import com.steam.cache.dto.SteamCacheRedisTemplate;
import com.steam.cache.dto.SteamCacheType;
import com.steam.cache.itf.ISteamCacheMDFactory;
import com.steam.cache.util.SteamCacheSpringUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Init SteamCacheComponse when container started
 *
 * author : gaopengj
 */
@Component
public class SteamCacheComponseInitConfiguration implements ApplicationRunner {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private void initCacheComposes() throws Exception{
        SteamCacheCompose[] composes = SteamCacheCompose.values();
        List<SteamCacheConfig> configList = new ArrayList<>();
        for(SteamCacheCompose compose : composes) {
            Annotation[] annotations = SteamCacheCompose.class.getField(compose.toString()).getAnnotations();
            Annotation annotation = annotations[0];
            if(annotation instanceof SteamCacheConfig){
                if(((SteamCacheConfig) annotation).startedInit()) {
                    configList.add((SteamCacheConfig) annotation);
                }
            }else if(annotation instanceof SteamCacheConfigGroup){
                SteamCacheConfig[] configArgs = ((SteamCacheConfigGroup)annotation).configGroup();
                for(SteamCacheConfig config : configArgs){
                    if(config.startedInit()){
                        configList.add(config);
                    }
                }
            }
        }

        if(CollectionUtils.isEmpty(configList)){//do not have startedInit cacheCompose
            return;
        }

        logger.info("==> SteamCacheComponseInitConfiguration init SteamCacheComponse begin ");

        long startTime = System.currentTimeMillis();

        for(SteamCacheConfig cacheConfig : configList){
            ISteamCacheMDFactory factoryBean = SteamCacheSpringUtil.getMDFactoryBean(cacheConfig.cacheType().getFactoryClass());
            SteamCacheConfigAttribute[] configAttributes = cacheConfig.attributes();
            Map<SteamCacheAttributeConstant,String> attributeMap = null;
            if(ArrayUtils.isNotEmpty(configAttributes)){
                attributeMap = new HashMap<>();
                for(SteamCacheConfigAttribute attr : configAttributes){
                    attributeMap.put(attr.code(), StringUtils.isEmpty(attr.value()) ? attr.code().getDefaultValue() : attr.value());
                }
            }

            Object cacheObj = factoryBean.build(attributeMap);

            if(cacheConfig.cacheType() == SteamCacheType.Redis){
                //初始化客户端管道
                SteamCacheRedisTemplate steamCacheRedisTemplate = (SteamCacheRedisTemplate)cacheObj;
                steamCacheRedisTemplate.getRedisTemplate().executePipelined(new SessionCallback<Object>() {
                    @Override
                    public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                        operations.hasKey((K)"");
                        return null;
                    }
                });
            }
        }

        long endTime = System.currentTimeMillis();

        logger.info("==> SteamCacheComponseInitConfiguration init SteamCacheComponse end , cost {} ms",(endTime - startTime));
    }

    @Override
    public void run(ApplicationArguments args){
        try {
            initCacheComposes();
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
    }
}
