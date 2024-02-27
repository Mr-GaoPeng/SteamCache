package com.steam.cache.annotation;


import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SteamCacheConfigGroup {
    SteamCacheConfig[] configGroup() default {};

    /**
     * 传播标志
     * todo 后面在针对传播设立枚举，按枚举类型进行缓存的传播；
     * @return  true:缓存值会传播到未建立缓存的缓存层
     */
    boolean propagation() default false;
}
