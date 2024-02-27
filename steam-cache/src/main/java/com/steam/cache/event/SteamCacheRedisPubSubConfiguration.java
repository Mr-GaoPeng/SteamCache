package com.steam.cache.event;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.steam.cache.event.subscribe.SteamCacheSubscribeListener;
import com.steam.cache.util.SteamCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

@Configuration
@ConditionalOnExpression("#{'RedisPubSub'.equals('${steamCache.notify.type:RedisPubSub}')}")
public class SteamCacheRedisPubSubConfiguration {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    @Qualifier("steamCacheMessageListener")
    public MessageListenerAdapter getMessageListenerAdapter(SteamCacheSubscribeListener steamCacheMessageListener){
        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(steamCacheMessageListener,"onMessage");
        messageListenerAdapter.setSerializer(getJacksonRedisSerializer());
        messageListenerAdapter.afterPropertiesSet();
        return messageListenerAdapter;
    }

    @Bean
    public RedisMessageListenerContainer getRedisMessageListenerContainer(@Qualifier("cacheTaskExecutor") Executor executor,
                                                                          @Qualifier("steamCacheMessageListener") MessageListenerAdapter messageListenerAdapter){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.setTaskExecutor(executor);
        container.addMessageListener(messageListenerAdapter,getChannelTopicByEnum());
        return container;
    }

    private RedisSerializer getJacksonRedisSerializer(){
        Jackson2JsonRedisSerializer serializer = new Jackson2JsonRedisSerializer(Object.class);
        serializer.setObjectMapper(objectMapper);
        return serializer;
    }

    private List<ChannelTopic> getChannelTopicByEnum(){
        return Arrays.asList(new ChannelTopic(SteamCacheUtil.SteamCacheIndex.DEFAULT_CACHE.getCode()),
                new ChannelTopic(SteamCacheUtil.SteamCacheIndex.USER_CACHE.getCode()),
                new ChannelTopic(SteamCacheUtil.SteamCacheIndex.ROLE_CACHE.getCode()),
                new ChannelTopic(SteamCacheUtil.SteamCacheIndex.BENCH_CACHE.getCode()));
    }
}
