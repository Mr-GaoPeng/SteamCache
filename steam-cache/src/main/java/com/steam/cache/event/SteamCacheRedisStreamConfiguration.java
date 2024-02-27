package com.steam.cache.event;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.util.ErrorHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@ConditionalOnExpression("#{'RedisStream'.equals('${steamCache.notify.type:}')}")
public class SteamCacheRedisStreamConfiguration {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${spring.application.name}")
    private String streamGroupName;
    
    private static final String STREAM_KEY = "SteamCacheStream";



    @Bean
    public Object initSteamCacheStreamKey(){
        boolean containStream = containStream(redisTemplate,STREAM_KEY);

        if(!containStream) {
            //不存在steamKey则创建

            Map<String,String> sourceMap = new HashMap<>();
            sourceMap.put("",null);


            MapRecord mapRecord = StreamRecords.newRecord().ofMap(sourceMap).withStreamKey(STREAM_KEY);

            redisTemplate.opsForStream().add(mapRecord);

        }

        return new Object();
    }

    private boolean containStream(RedisTemplate redisTemplate,String streamKey){
        try{
            StreamInfo.XInfoStream stream = redisTemplate.opsForStream().info(streamKey);
            return stream != null;
        }catch (Exception e){
            try {
                Long sizeOf = redisTemplate.opsForStream().size(streamKey);
                return sizeOf > 0;
            }catch (Exception ex){
                return false;
            }
        }
    }

    @Bean
    public Object initSteamCacheClientGroup(){
        StreamInfo.XInfoGroups infoGroups = redisTemplate.opsForStream().groups(STREAM_KEY);

        if(infoGroups.isEmpty()){
            //按微服务名称创建消费者组
            redisTemplate.opsForStream().createGroup(STREAM_KEY, streamGroupName);
        }

        return new Object();
    }
    

    @Bean
    @Qualifier("steamCacheGroupConsumerName")
    public String initSteamCacheClientGroupConsumerName() throws UnknownHostException {
        String ipAddress = InetAddress.getLocalHost().getHostAddress();

        //String groupConsumerName = applicationName + "_" + ipAddress.substring(ipAddress.lastIndexOf(".")+1);

        return ipAddress;
    }


    private RedisSerializer getJacksonRedisSerializer(){
        Jackson2JsonRedisSerializer serializer = new Jackson2JsonRedisSerializer(Object.class);
        serializer.setObjectMapper(objectMapper);
        return serializer;
    }


    /**
     *
     * 后注册的消费者，不能读取之前生产者发送的数据；（考虑清除缓存场景，不需要在搞继续读取历史数据的逻辑）
     *
     */
    @Bean
    public StreamMessageListenerContainer streamMessageListenerContainer(@Qualifier("steamCacheRedisStreamListener") StreamListener streamListener,
                                                                         @Qualifier("steamCacheGroupConsumerName") String groupConsumerName) {
        AtomicInteger index = new AtomicInteger(1);
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(processors, processors, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), r -> {
            Thread thread = new Thread(r);
            thread.setName("async-stream-comsumer-" + index.getAndDecrement());
            thread.setDaemon(true);
            return thread;
        });

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions streamMessageListenerContainerOptions = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder().batchSize(10).executor(executor).errorHandler(new ErrorHandler() {
            @Override
            public void handleError(Throwable t) {
                t.printStackTrace();
            }
        }).pollTimeout(Duration.ZERO).serializer(new StringRedisSerializer()).build();

        StreamOffset<String> StreamOffset = org.springframework.data.redis.connection.stream.StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed());
        StreamMessageListenerContainer streamMessageListenerContainer = StreamMessageListenerContainer.create(redisConnectionFactory, streamMessageListenerContainerOptions);
        streamMessageListenerContainer.receive(Consumer.from(streamGroupName, groupConsumerName), StreamOffset, streamListener);
        streamMessageListenerContainer.start();
        return streamMessageListenerContainer;
    }
}