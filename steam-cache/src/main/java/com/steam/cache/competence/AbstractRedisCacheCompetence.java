package com.steam.cache.competence;

import com.steam.cache.dto.SteamCacheRedisTemplate;
import com.steam.cache.itf.ISteamCacheMDCompetence;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public abstract class AbstractRedisCacheCompetence extends AbstractRedisCacheStat implements ISteamCacheMDCompetence<SteamCacheRedisTemplate> {

    protected Logger logger  = LoggerFactory.getLogger(this.getClass());

    private static final Random random = new SecureRandom();

    protected int randomDuration(){
        return random.nextInt(3);
    }


    @Override
    public void put(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey, Object cacheValue) {
        //采用scan方式扫描删除键值的前提下，不需要在本地维护维护键列表了；
        //redis缓存存放时，将键也本地存储一份（timer来负责key失效），在模糊删除时使用（采用遍历剩余key 替代 scan方式）
        /*Long duration = cacheRedisTemplate.getDuration();
        if(duration != null) {
            long delay = getTimeUnitRatio(duration,cacheRedisTemplate.getTimeUnit());
            SteamCacheUtil.localPutCacheKeyWithDelay(cacheKey, delay);
        }else{
            //未设置失效时间的键
            SteamCacheUtil.localPutCacheKeyWithDelay(cacheKey, Long.MAX_VALUE);
        }*/
    }

    private Long getTimeUnitRatio(Long duration,TimeUnit timeUnit){
        long unitRatio = 1;
        switch (timeUnit) {//转成毫秒
            case NANOSECONDS:
                unitRatio = 1 / 1000000;
                break;
            case MICROSECONDS:
                unitRatio = 1 / 1000;
                break;
            case MILLISECONDS:
                unitRatio = 1;
                break;
            case SECONDS:
                unitRatio = 1000;
                break;
            case MINUTES:
                unitRatio = 60 * 1000;
                break;
            case HOURS:
                unitRatio = 60 * 60 * 1000;
                break;
            case DAYS:
                unitRatio = 24 * 60 * 60 * 1000;
                break;
        }

        return duration * unitRatio;
    }

    @Override
    public void remove(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey) {
        //redisTemplate.delete(cahceKey);
        // 考虑大键删除影响性能，采用惰性删除删除键值
        cacheRedisTemplate.getRedisTemplate().expire(cacheKey,1, TimeUnit.MILLISECONDS);//1ms失效
    }

    @Override
    public void removeByPrefix(SteamCacheRedisTemplate cacheRedisTemplate, String keyPrefix) {
        //方式一：采用scan方式遍历redis下的键，在逐个设置失效时间来淘汰键值；（redis 存储的键值过多时，会耗性能）
        List<String> cacheKeys = new ArrayList<>();
        String machKey = keyPrefix + "*";
        ScanOptions scanOptions = ScanOptions.scanOptions().count(100L).match(machKey).build();
        Cursor cursor = cacheRedisTemplate.getRedisTemplate().scan(scanOptions);
        while (cursor.hasNext()){
            String key = (String)cursor.next();
            cacheKeys.add(key);
        }
        if(CollectionUtils.isNotEmpty(cacheKeys)){
            cacheKeys.stream().forEach(key->cacheRedisTemplate.getRedisTemplate().expire(key,1, TimeUnit.MILLISECONDS));
        }
        cursor.close();

        //todo 防止方式一在键值过多情况下占用cpu资源，后续完善方式二的删除逻辑，在服务启动时加载一波
        //方式二：通过本地维护的存储键的Map ,遍历键Map逐个设置失效时间来淘汰键值；（缓存Key的Map，综合考虑数据量不会特别大，采用此方式）
        /*tips:
            1)此方式当服务重启后，缓存Key的Map集合数据清空，无法清除已有的redis缓存；
            2)基于情况（1），依赖redis自有的到期淘汰机制；
            3)基于情况（2），若存在未设置失效时间的redis键值，如何清除？（cache层暂未有此情况，都是定义了失效时间的）//todo 服务启动时，从redis加载一遍？
        */
        /*if(MapUtils.isNotEmpty(SteamCacheUtil.cacheKeyMap)) {
            SteamCacheUtil.cacheKeyMap.keySet().stream().filter(key -> key.startsWith(keyPrefix)).forEach(key -> {
                cacheRedisTemplate.getRedisTemplate().expire(key, 1, TimeUnit.MILLISECONDS);
            });
        }*/
    }
}
