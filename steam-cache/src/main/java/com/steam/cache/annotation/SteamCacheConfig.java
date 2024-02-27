package com.steam.cache.annotation;


import com.steam.cache.dto.SteamCacheType;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SteamCacheConfig {
    SteamCacheType cacheType();
    SteamCacheConfigAttribute[] attributes() default {};
    boolean valueCopyFlag() default false;
    boolean startedInit() default false;
}
