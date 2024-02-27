package com.steam.cache.annotation;


import com.steam.cache.dto.SteamCacheCompose;
import com.steam.cache.dto.SteamCachePartitionConstant;
import com.steam.cache.util.SteamCacheUtil;

import java.lang.annotation.*;


@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SteamCache {

    SteamCacheUtil.SteamCacheIndex cacheIndex() default SteamCacheUtil.SteamCacheIndex.DEFAULT_CACHE;//缓存索引

    /**
     * 缓存键的前缀
     * @return
     */
    String cacheKeyPrefix() default "";

    /**
     * 可缓存空值，默认false(不缓存空值)
     * *** 对于方法返回值为空或者空集合的情况，通过这个参数判定是否放入缓存中，默认是不放入的，如果有场景需要可以设定可缓存空值（缓存穿透）;
     * @return
     */
    boolean cacheNullable() default false;

    /**
     * 带版本的缓存；（适用并发写缓存的场景）
     *
     * 1、额外生成版本号记录缓存；
     * 2、会在生成cacheKey的时候，额外拼加版本号；查不到版本号的默认拼加 :-1 版本表示；
     *
     * @return
     */
    //boolean cacheVersioned() default false;

    /**
     * 隔离维度,会参与缓存键的生成；
     * tips:业务接口使用是按需设置；
     * @return
     */
    SteamCachePartitionConstant[] partitions() default {};

    /**
     * 缓存层枚举
     * @return  返回预先定义好的缓存层
     */
    SteamCacheCompose cacheCompose() default SteamCacheCompose.Level_1;

    /**
     * 执行器；参见 SteamCacheExecutor 枚举定义
     * @return 执行器
     */
    SteamExecutor executor() default @SteamExecutor();


    /**
     * 自动根据方法类型采用缓存类型
     * @return
     */
    boolean autoMethodCacheType() default true;
}
