package com.steam.cache.executor;

import com.steam.cache.annotation.SteamCache;
import com.steam.cache.annotation.SteamExecutor;
import com.steam.cache.itf.ISteamCacheMDCompetence;
import com.steam.cache.util.SteamCacheSpringUtil;
import com.steam.cache.util.SteamCacheUtil;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;


/**
 * 本地调度任务异步执行器；
 * 继承AsyncExecutor异步执行期，增加了本地调度功能；
 *
 * 以下情况不适用此执行器：
 * 1、方法SteamCache注解使用中带隔离维度的；（暂不支持，隔离维度需要上下文信息，调度任务执行时，需要额外缓存用户信息，在恢复上下文，开销比较大，后面考虑好在支持）
 * 2、方法内实现带上下文相关的；（理由同上）
 * 3、方法需被调用过；未调用过的方法不支持；（调用过的方法相关参数被缓存，以供调度使用；）
 * 4、建议：适用缓存时间大于缓存层定义，且不怎么频繁调用的方法上适用；
 *
 */
@Component
public class AsyncScheduleExecutor extends AsyncExecutor{

    private static final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();//单核心线程的调度异步线程执行器
    private static Map<MethodInvokeInfo,ScheduleExecutorHandlerInfo> methodInvokeCacheMap = new ConcurrentHashMap<>();

    @Data
    public class ScheduleExecutorHandlerInfo{
        private ExecutorHandlerInfo executorHandlerInfo;
        private int count;
        private ScheduledFuture future;
    }

    @Override
    public Object execute(ExecutorAction action, SteamCache steamCache, Class targetCls, Method method, Object... methodArgs) {

        if(steamCache.partitions() != null && steamCache.partitions().length > 0){//有隔离维度的设置，不支持此调度执行器
            return super.execute(action, steamCache, targetCls, method, methodArgs);
        }

        SteamExecutor steamExecutor = steamCache.executor();
        if(steamExecutor.period() == 0L || steamExecutor.count() <= 0){//周期为0 或者 次数设置小于等于0 的，不走此调度执行器
            return super.execute(action, steamCache, targetCls, method, methodArgs);
        }


        Object result = super.execute(action, steamCache, targetCls, method, methodArgs);

        if(action == ExecutorAction.GET) {
            //针对get取缓存操作，进行触发调度执行操作
            MethodInvokeInfo invokeInfo =  new MethodInvokeInfo();
            invokeInfo.setSteamCache(steamCache);
            invokeInfo.setTargetCls(targetCls);
            invokeInfo.setMethod(method);
            invokeInfo.setMethodParams(methodArgs);
            if (!methodInvokeCacheMap.keySet().contains(invokeInfo)) {
                ExecutorHandlerInfo handlerInfo = tl_handlerInfo.get();
                ScheduleExecutorHandlerInfo scheduleExecutorHandlerInfo = new ScheduleExecutorHandlerInfo();
                scheduleExecutorHandlerInfo.setExecutorHandlerInfo(handlerInfo);
                scheduleExecutorHandlerInfo.setCount(steamExecutor.count());
                methodInvokeCacheMap.put(invokeInfo, scheduleExecutorHandlerInfo);

                //延迟period时间触发，间隔period时间；

                ScheduledFuture scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {

                    if(scheduleExecutorHandlerInfo.getCount() == 0 && scheduleExecutorHandlerInfo.getFuture() != null){
                        //取消任务
                        scheduleExecutorHandlerInfo.getFuture().cancel(true);
                        return;
                    }

                    try {
                        Object methodResult = invokeInfo.method.invoke(SteamCacheSpringUtil.getBeanFromContext(invokeInfo.targetCls),invokeInfo.methodParams);
                        if(methodResult != null){
                            LinkedHashMap<Object, ISteamCacheMDCompetence> competenceLinkedHashMap = scheduleExecutorHandlerInfo.getExecutorHandlerInfo().getMethodCompetenceBeanMap().get(method.hashCode());
                            String cacheKey = SteamCacheUtil.getMethodCacheKey(invokeInfo.steamCache,
                                    invokeInfo.targetCls,
                                    invokeInfo.method,
                                    invokeInfo.methodParams);
                            for (Map.Entry<Object, ISteamCacheMDCompetence> entry : competenceLinkedHashMap.entrySet()) {
                                entry.getValue().put(entry.getKey(),cacheKey,methodResult);
                            }
                        }
                    } catch (Exception e) {//吞了异常，防止影响下面任务的执行；
                        logger.error(e.getMessage(),e);
                        //throw new RuntimeException(e);
                    }finally {
                        //执行次数减1
                        scheduleExecutorHandlerInfo.setCount(scheduleExecutorHandlerInfo.getCount() - 1);
                    }

                }, steamExecutor.period(), steamExecutor.period(), TimeUnit.MINUTES);

                scheduleExecutorHandlerInfo.setFuture(scheduledFuture);

            }
        }

        return result;
    }
}
