package com.steam.cache.competence;

import com.steam.cache.dto.SteamCacheRedisTemplate;
import com.steam.cache.itf.ISteamCacheStat;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRedisCacheStat implements ISteamCacheStat<SteamCacheRedisTemplate> {

    @Override
    public long totalCount(SteamCacheRedisTemplate cacheRedisTemplate) {
        return 0;
    }

    @Override
    public String totalSize(SteamCacheRedisTemplate cacheRedisTemplate) {
        return null;
    }

    @Override
    public String humanSizeOf(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey) {
        List<String> keyList = new ArrayList<>();
        if(cacheKey.endsWith("*")){
            //范围搜索
            ScanOptions scanOptions = ScanOptions.scanOptions().count(100L).match(cacheKey).build();
            Cursor cursor = cacheRedisTemplate.getRedisTemplate().scan(scanOptions);
            while (cursor.hasNext()){
                String key = (String)cursor.next();
                keyList.add(key);
            }
        }else{
            keyList.add(cacheKey);
        }

        if(CollectionUtils.isNotEmpty(keyList)) {
            String script = "return redis.pcall('MEMORY', 'USAGE', KEYS[1])";
            Long keySiseResult = (Long)cacheRedisTemplate.getRedisTemplate().execute((RedisCallback<Long>) connection -> {
                Long keySizeSum = 0L;
                for (String key : keyList) {
                    Long keySise = connection.eval(script.getBytes(StandardCharsets.UTF_8), ReturnType.INTEGER, 1, key.getBytes(StandardCharsets.UTF_8));
                    if (keySise != null) {
                        keySizeSum += keySise;
                    }
                }
                return keySizeSum;
            });
            if (keySiseResult != null) {
                return keySiseResult / 1024 + " KB";
            }
        }
        return null;
    }


    private static final long str_Length = 1024 * 1024;
    private static final long coll_length = 2000L;

    @Override
    public boolean isBigKey(SteamCacheRedisTemplate cacheRedisTemplate, String cacheKey, Object cacheVal) {
        DataType dataType = cacheRedisTemplate.getRedisTemplate().type(cacheKey);
        switch (dataType){
            case STRING:
                return StringUtils.length(cacheKey) > str_Length;//大于1M
            case LIST:
                Long listSize = cacheRedisTemplate.getRedisTemplate().opsForList().size(cacheKey);//集合长度超2000
                return (listSize == null ? 0 : listSize) > coll_length;
            case HASH:
                Long hashSize = cacheRedisTemplate.getRedisTemplate().opsForHash().size(cacheKey);//集合长度超2000
                return  (hashSize == null ? 0 : hashSize) > coll_length;
            default:
                break;
        }

        return false;
    }
}
