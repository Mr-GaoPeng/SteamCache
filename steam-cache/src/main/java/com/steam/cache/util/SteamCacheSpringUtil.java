package com.steam.cache.util;

import com.steam.cache.annotation.SteamCache;
import com.steam.cache.event.dto.SteamCacheEvent;
import com.steam.cache.event.dto.SteamCacheEventCleanUp;
import com.steam.cache.itf.ISteamCacheMDCompetence;
import com.steam.cache.itf.ISteamCacheMDFactory;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Component
public class SteamCacheSpringUtil implements ApplicationContextAware {
    private static Logger logger = LoggerFactory.getLogger(SteamCacheSpringUtil.class);

    private static ApplicationContext applicationContext;


    private static Executor taskExecutor;

    @Autowired
    public void setTaskExecutor(@Qualifier("cacheTaskExecutor")Executor taskExecutor){
        SteamCacheSpringUtil.taskExecutor = taskExecutor;
    }

    private static ThreadLocal<Boolean> tl_valueCopy = new ThreadLocal<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SteamCacheSpringUtil.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static ISteamCacheMDFactory getMDFactoryBean(Class cls){
        return (ISteamCacheMDFactory)getBeanFromContext(cls);
    }

    public static ISteamCacheMDCompetence getMDCompetenceBean(Class cls){
        return (ISteamCacheMDCompetence)getBeanFromContext(cls);
    }


    public static <T> T getBeanFromContext(Class<T> cls){

        Map<String, T> beanMap = applicationContext.getBeansOfType(cls);
        if(MapUtils.isEmpty(beanMap)){
            return null;
        }

        if(cls.isInterface() || Modifier.isAbstract(cls.getModifiers())){
            //如果是接口或者抽象类,返回第一个
            return beanMap.get(beanMap.keySet().stream().findFirst().get());
        }
        String beanId = null;
        Component component = cls.getAnnotation(Component.class);
        if(component != null){
            beanId = component.value();
        }
        Service service = cls.getAnnotation(Service.class);
        if(service != null){
            beanId = service.value();
        }

        if(StringUtils.isEmpty(beanId)){
            beanId = SteamCacheHutoolUtil.lowerFirst(cls.getSimpleName());
        }
        String finalBeanId = beanId;
        String findKey = beanMap.keySet().stream().filter(key->key.equals(finalBeanId)).findFirst().get();
        return beanMap.get(findKey);
    }

    /**
     * 容器内发送事件通知
     * @param cls           class
     * @param methodName    方法名称
     * @param cacheKey      方法缓存key(缓存键全名)
     */
    public static void publishCleanUpSteamCacheEvent(Class cls, String methodName, String cacheKey){
        Assert.notNull(cls,"paramter cls can not be null");
        //methodName 为空时，清除接口下所有方法的缓存；
        //cacheKey 为空时，清除接口方法下所有的缓存；
        Object clsBean = applicationContext.getBean(cls);
        if(clsBean == null){
            logger.error("notify only ===> can not getBean from Context by Class : " + cls);
            return;
        }
        if(AopUtils.isAopProxy(clsBean)){
            clsBean = AopProxyUtils.getSingletonTarget(clsBean);
        }
        List<Method> methodLists = Arrays.stream(clsBean.getClass().getMethods())
                .filter(data-> null != data.getAnnotation(SteamCache.class))
                .filter(data->{
                    if(StringUtils.isEmpty(methodName)){
                        return true;
                    }
                    return methodName.equals(data.getName());
                }).collect(Collectors.toList());

        Object finalClsBean = clsBean;
        if(taskExecutor != null){//异步调用
            CompletableFuture.runAsync(() -> {
                methodLists.stream().forEach(method->{
                    SteamCacheEvent cacheEvent = new SteamCacheEventCleanUp(finalClsBean,cls,method,cacheKey);
                    publishSteamCacheEvent(cacheEvent);
                });
            }, taskExecutor);
        }else{//同步调用
            methodLists.stream().forEach(method->{
                SteamCacheEvent cacheEvent = new SteamCacheEventCleanUp(finalClsBean,cls,method,cacheKey);
                publishSteamCacheEvent(cacheEvent);
            });
        }
    }

    public static <T extends SteamCacheEvent> void publishSteamCacheEvent(T steamCacheEvent){
        applicationContext.publishEvent(steamCacheEvent);
    }

    public static void setThreadValueCopyVal(Boolean flag){
        tl_valueCopy.set(flag);
    }

    public static void restThreadValueCopy(){
        tl_valueCopy.remove();
    }

    public static boolean getValueCopyCfgProperty(){
        Boolean valueCopy = tl_valueCopy.get();
        if(valueCopy == null){
            valueCopy = applicationContext.getEnvironment().getProperty("steamCache.value.copy",Boolean.class,true);
        }
        return valueCopy;
    }
}
