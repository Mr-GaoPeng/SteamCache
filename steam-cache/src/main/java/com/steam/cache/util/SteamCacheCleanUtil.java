package com.steam.cache.util;

import com.steam.cache.annotation.SteamCache;
import com.steam.cache.annotation.SteamCacheConfig;
import com.steam.cache.annotation.SteamCacheConfigAttribute;
import com.steam.cache.annotation.SteamCacheConfigGroup;
import com.steam.cache.dto.SteamCacheAttributeConstant;
import com.steam.cache.dto.SteamCacheCompose;
import com.steam.cache.itf.ISteamCacheMDCompetence;
import com.steam.cache.itf.ISteamCacheMDFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SteamCacheCleanUtil {
    /**
     * 清全部缓存
     */
    public static <T>  void cleanUp(T t){
        Assert.notNull(t,"impl bean can not be null");
        if(AopUtils.isAopProxy(t)){
            t = (T)AopProxyUtils.getSingletonTarget(t);
        }
        Class cls = t.getClass();
        Method[] methods = cls.getMethods();
        List<Method> cacheMethods = Stream.of(methods).filter(data-> null != data.getAnnotation(SteamCache.class)).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(cacheMethods)){return;}

        //todo 暂时先用逐个方法清缓存的实现
        T finalT = t;
        cacheMethods.stream().forEach(data->cleanUp(finalT,data));
    }

    /**
     * 按方法名称查找，清除缓存
     * @param methodName
     */
    public static <T> void cleanUp(T t,String methodName){
        Assert.hasLength(methodName,"methodName can not be null");
        if(AopUtils.isAopProxy(t)){
            t = (T)AopProxyUtils.getSingletonTarget(t);
        }
        Class cls = t.getClass();
        Method[] methods = cls.getMethods();
        List<Method> filterMethods = Stream.of(methods)
                .filter(data-> null != data.getAnnotation(SteamCache.class))
                .filter(data->methodName.equals(data.getName()))
                .collect(Collectors.toList());
        if(CollectionUtils.isEmpty(filterMethods)){return;}

        //todo 暂时先用逐个方法清缓存的实现
        filterMethods.stream().forEach(data->cleanUp(data));
    }

    /**
     * 按方法查找，清除缓存
     * 清方法级全部缓存
     * @param method
     */
    public static <T> void cleanUp(T t,Method method){
        cleanUp(t,method,null);
    }

    /**
     * 按指定方法清除缓存
     * @param method
     */
    public static <T> void cleanUp(T t,Method method,String cacheKey){
        Assert.notNull(method,"method can not be null");
        if(AopUtils.isAopProxy(t)){
            t = (T)AopProxyUtils.getSingletonTarget(t);
        }
        Method targetMethod = null;
        try {
            targetMethod = t.getClass().getMethod(method.getName(),method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return;
        }
        SteamCache steamCache = targetMethod.getAnnotation(SteamCache.class);
        if(steamCache == null){return;}

        SteamCacheCompose cacheCompose = steamCache.cacheCompose();
        Annotation[] annotations = new Annotation[0];
        try {
            annotations = SteamCacheCompose.class.getField(cacheCompose.toString()).getAnnotations();
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        Assert.notEmpty(annotations,"SteamCacheConfig of SteamCacheCompose Enum can not be null");
        String cacheKeyPrefix = StringUtils.isNotEmpty(steamCache.cacheKeyPrefix()) ? steamCache.cacheKeyPrefix() : SteamCacheUtil.getMethodCacheKeyPrefix(t.getClass().getName(),method.getName());
        String cacheKeyType = SteamCacheUtil.getMethodUsedCacheType(steamCache,method);
        try {
            cacheKeyPrefix = SteamCacheUtil.getAssembleMethodCacheKey(steamCache,cacheKeyPrefix,null,null);
        } catch (Exception e) {
            throw new RuntimeException("SteamCacheUtil.getAssembleMethodCacheKey has error,can not get cacheKey");
        }
        Annotation annotation = annotations[0];
        if(annotation instanceof SteamCacheConfig){
            //一级缓存
            Object cache = getCacheByConfig((SteamCacheConfig)annotation);
            if(cache != null) {
                ISteamCacheMDCompetence competenceBean = SteamCacheSpringUtil.getMDCompetenceBean(((SteamCacheConfig) annotation).cacheType().getCompetenceClass(cacheKeyType));
                if(StringUtils.isNotEmpty(cacheKey)){
                    competenceBean.remove(cache,cacheKey);
                }else{
                    competenceBean.removeByPrefix(cache,cacheKeyPrefix);
                }
            }
        }else if (annotation instanceof SteamCacheConfigGroup){
            //多级缓存，顺序取值
            SteamCacheConfig[] configArgs = ((SteamCacheConfigGroup)annotation).configGroup();
            for(int i = 0; i < configArgs.length; i++){
                Object cacheObj = getCacheByConfig(configArgs[i]);
                ISteamCacheMDCompetence cacheMDCompetence = SteamCacheSpringUtil.getMDCompetenceBean(configArgs[i].cacheType().getCompetenceClass(cacheKeyType));
                if(StringUtils.isNotEmpty(cacheKey)){
                    cacheMDCompetence.remove(cacheObj,cacheKey);
                }else{
                    cacheMDCompetence.removeByPrefix(cacheObj,cacheKeyPrefix);
                }
            }
        }else{
            Assert.isTrue(false,annotation.toString() + " can not be favour");
        }
    }

    public static Object getCacheByConfig(SteamCacheConfig cacheConfig){
        ISteamCacheMDFactory factoryBean = SteamCacheSpringUtil.getMDFactoryBean(cacheConfig.cacheType().getFactoryClass());
        SteamCacheConfigAttribute[] configAttributes = cacheConfig.attributes();
        Map<SteamCacheAttributeConstant,String> attributeMap = null;
        if(ArrayUtils.isNotEmpty(configAttributes)){
            attributeMap = new HashMap<>();
            for(SteamCacheConfigAttribute attr : configAttributes){
                attributeMap.put(attr.code(), StringUtils.isEmpty(attr.value()) ? attr.code().getDefaultValue() : attr.value());
            }
        }

        Object cache = factoryBean.build(attributeMap);
        return cache;
    }
}
