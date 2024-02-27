package com.steam.cache.executor;

import com.steam.cache.annotation.SteamCache;
import com.steam.cache.itf.ISteamCacheMDCompetence;
import lombok.Data;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractExecutor {
    public enum ExecutorAction{
        PUT,//放
        GET,//取
        PROPAGATE//传播
    }

    @Data
    public class ExecutorHandlerInfo{
        Map<Integer,LinkedHashMap<Object, ISteamCacheMDCompetence>>  methodCompetenceBeanMap;//缓存层对象
        Object methodInvokeValue;//方法返回值
        Map<Integer,Boolean> methodPropagateFlag;//是否传播
        Object cacheValue;//缓存值
    }

    @Data
    public class MethodInvokeInfo{
        SteamCache steamCache;
        Class targetCls;
        Method method;
        Object[] methodParams;

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof MethodInvokeInfo)){return false;}
            MethodInvokeInfo objInfo = (MethodInvokeInfo)obj;
            boolean clsCompare = this.targetCls.getName().equals(objInfo.targetCls.getName());
            boolean methodCompare = this.method.equals(objInfo.method);
            boolean paramCompare = Arrays.equals(this.methodParams,objInfo.methodParams);
            return clsCompare && methodCompare && paramCompare;
        }

        @Override
        public int hashCode() {
            return this.targetCls.getName().hashCode();
        }
    }


    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    public static ThreadLocal<ExecutorHandlerInfo> tl_handlerInfo = new ThreadLocal<>();
    private static ThreadLocal<Integer> tl_counter = new ThreadLocal<Integer>(){
        @Override
        protected Integer initialValue() {
            return Integer.valueOf(0);
        }
    };

    AbstractExecutor(){
        initActionHandlers();
    }

    private void initActionHandlers() {
        //初始化放入默认执行处理器
        //todo 指定操作handler的方式仍需优化；
        actionHandlerMap = new HashMap<>();
        actionHandlerMap.put(ExecutorAction.PUT, new PutExecutorActionHandler(this));
        actionHandlerMap.put(ExecutorAction.GET,new GetExecutorActionHandler(this));
        actionHandlerMap.put(ExecutorAction.PROPAGATE,new PropagateExecutorActionHandler(this));

        Map<ExecutorAction, AbstractExecutorActionHandler> extActionHandlerMap = configExtActionHandlerMap();//executorActionHandler扩展
        if(MapUtils.isNotEmpty(extActionHandlerMap)){
            extActionHandlerMap.keySet().stream().forEach(extAction->{
                actionHandlerMap.put(extAction,extActionHandlerMap.get(extAction));//扩展实现覆盖初始化默认的
            });
        }
    }

    private Map<ExecutorAction, AbstractExecutorActionHandler> actionHandlerMap;////操作缓存执行处理器

    protected Map<ExecutorAction, AbstractExecutorActionHandler> configExtActionHandlerMap(){return null;}

    public Object execute(ExecutorAction action, SteamCache steamCache, Class targetCls, Method method, Object... methodArgs){

        AbstractExecutorActionHandler actionHandler = actionHandlerMap.get(action);
        Assert.notNull(actionHandler,"can not init ExecutorAction Impl Handler : " + action);
        try {
            return actionHandler.execute(steamCache,targetCls,method,methodArgs);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            //throw new RuntimeException(e.getMessage(),e);//吞异常，不影响下游执行
            return null;
        }
    }

    public void initHandlerInfo(){
        if(tl_handlerInfo.get() == null) {
            ExecutorHandlerInfo handlerInfo = new ExecutorHandlerInfo();
            handlerInfo.setMethodCompetenceBeanMap(new HashMap<>());
            handlerInfo.setMethodPropagateFlag(new HashMap<>());
            handlerInfo.setCacheValue(null);
            handlerInfo.setMethodInvokeValue(null);
            tl_handlerInfo.set(handlerInfo);
        }else {
            tl_counter.set(tl_counter.get()+1);
        }
    }

    public void reset(){
        if(tl_counter.get() == 0) {
            tl_handlerInfo.remove();
        }else{
            tl_counter.set(tl_counter.get()-1);
        }
    }
}
