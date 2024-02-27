package com.steam.cache.annotation;


import com.steam.cache.dto.SteamCacheAttributeConstant;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SteamCacheConfigAttribute {

    SteamCacheAttributeConstant code();
    String value() default "";
}
