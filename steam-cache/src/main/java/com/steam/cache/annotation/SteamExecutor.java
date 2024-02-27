package com.steam.cache.annotation;

import com.steam.cache.executor.AbstractExecutor;
import com.steam.cache.executor.SyncExecutor;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SteamExecutor {
    Class<? extends AbstractExecutor> executorCls() default SyncExecutor.class;

    //周期,时间单位是 TimeUnit.MINUTES
    long period() default 10L;

    //次数
    int count() default 0;
}
