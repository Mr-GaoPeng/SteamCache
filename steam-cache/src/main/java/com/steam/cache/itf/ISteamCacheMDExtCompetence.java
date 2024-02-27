package com.steam.cache.itf;

/**
 * 缓存中间件的扩展能力抽象接口
 */
public interface ISteamCacheMDExtCompetence<T> {
    /**
     * 增量往key缓存中加值（不新增key）
     *
     * tips:需要考虑多入口更新缓存的更新问题；（建议通过额外增加版本号缓存来解决）
     */
    void incrementPut(T t,String cacheKey,Object cacheValue);

    /**
     * 增量删除key缓存中的值
     *
     * tips:需要考虑多入口更新缓存的更新问题；（建议通过额外增加版本号缓存来解决）
     */
    void decrementRemove(T t,String cacheKey,Object cacheValue);
}
