package com.steam.cache.event.subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("#{'RedisStream'.equals('${steamCache.notify.type:}')}")
public class SteamCacheRedisStreamListener implements StreamListener<String, MapRecord<String,String,String>> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        logger.warn("redis stream consumer get message: streamId:{}, id:{}, value:{}",message.getStream(),message.getId().toString(),message.getValue());
    }
}
