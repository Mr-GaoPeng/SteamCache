package com.steam.cache.annotation;

import com.steam.cache.dto.PreHeatType;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SteamCachePreHeat {

    /**
     * 编码
     */
    String code() default "";


    /**
     * 模版数据
     */
    String tplJson() default "{\"tenantId\":\"${steamCache.preheat.tenantId}\"}";


    /**
     * 预热类型，默认容器启动后开始预热
     */
    PreHeatType preHeatType() default PreHeatType.CONTAINER_STARTED;
}
