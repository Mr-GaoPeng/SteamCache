package com.steam.cache.dto;

public enum SteamCacheExecutor {
    syncExecutor("syncExecutor"),//同步执行器 默认
    AsyncExecutor("asyncExecutor"),//异步执行器
    AsyncScheduleExecutor("asyncScheduleExecutor")//异步调度任务执行器
    //StatAsyncExecutor(""),//带监控异步执行器

    ;

    private String beanName;

    SteamCacheExecutor(String beanName){
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }
}
