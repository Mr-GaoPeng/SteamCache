package com.steam.cache.executor;

import com.steam.cache.annotation.SteamCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


/**
 * 异步执行器；
 * 针对缓存存储、缓存传播 行为采用异步方式执行；
 */
@Component
public class AsyncExecutor  extends AbstractExecutor{

    @Autowired
    @Qualifier("cacheTaskExecutor")
    private Executor taskExecutor;


    @Override
    public Object execute(ExecutorAction action, SteamCache steamCache, Class targetCls, Method method, Object... methodArgs) {
        try {
            if (taskExecutor != null && (action == ExecutorAction.PUT || action == ExecutorAction.PROPAGATE)) {
                //线程上下文传递
                ExecutorHandlerInfo parentHandlerInfo = tl_handlerInfo.get();
                CompletableFuture future = CompletableFuture.supplyAsync(() -> {
                        try {
                            tl_handlerInfo.set(parentHandlerInfo);
                            return super.execute(action, steamCache, targetCls, method, methodArgs);
                        }finally {
                            tl_handlerInfo.remove();
                        }
                    }, taskExecutor);
                return future.get();
            } else {
                return super.execute(action, steamCache, targetCls, method, methodArgs);
            }
        }catch (Exception e){
            logger.error(e.getMessage(),e);
            //异常时，重新同步调用一次；
            return super.execute(action, steamCache, targetCls, method, methodArgs);
        }
    }
}
