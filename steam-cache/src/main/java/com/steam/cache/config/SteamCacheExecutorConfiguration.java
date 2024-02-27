package com.steam.cache.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class SteamCacheExecutorConfiguration {

    @Bean("cacheTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(16);
        taskExecutor.setMaxPoolSize(100);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix("commonCacheTaskExecutor-");
        taskExecutor.setKeepAliveSeconds(120);
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //tips:注意有上下文传递的场景
        //taskExecutor.setTaskDecorator(YmsContextWrappers::wrapRunnable);
        taskExecutor.initialize();
        return taskExecutor;
    }
}
