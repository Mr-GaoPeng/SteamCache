package com.steam.cache.executor;

import com.steam.cache.annotation.SteamCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 动作执行处理器
 * @param <T>
 */
public abstract class AbstractExecutorActionHandler<T> {
   Logger logger = LoggerFactory.getLogger(AbstractExecutorActionHandler.class);

   AbstractExecutor abstractExecutor;

   AbstractExecutorActionHandler(AbstractExecutor abstractExecutor){
      this.abstractExecutor = abstractExecutor;
   }


   abstract T execute(SteamCache steamCache, Class targetCls, Method method, Object... methodArgs) throws Exception;
}
