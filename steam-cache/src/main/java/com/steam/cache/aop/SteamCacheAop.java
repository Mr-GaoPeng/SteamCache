package com.steam.cache.aop;

import com.steam.cache.annotation.*;
import com.steam.cache.dto.SteamCacheAttributeConstant;
import com.steam.cache.dto.SteamCacheCompose;
import com.steam.cache.dto.SteamCacheType;
import com.steam.cache.executor.AbstractExecutor;
import com.steam.cache.itf.ISteamCacheMDFactory;
import com.steam.cache.util.SteamCacheSpringUtil;
import com.steam.cache.util.SteamCacheUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class SteamCacheAop {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public static ThreadLocal<String> threadLocal =  new ThreadLocal<>();//解决factory循环


    @Value("${steamCache.developer.enableCache:true}")
    private Boolean developerEnableCache;

    @Pointcut("@annotation(steamCache)")
    public void annotationCacheApi(SteamCache steamCache){
        //@SteamCache注解下的接口实现
    }

    @Pointcut("execution(* com.steam.cache.factory..*(..))")
    public void cacheFactoryApi(){
        //特意为cacheFactory新建的匹配规则（不可移除）
    }


    //@Around("serviceServerCacheApi() || serviceClientCacheApi() || commonSdkBaseCacheApi()")
    @Around("annotationCacheApi(steamCache) && !cacheFactoryApi()")
    public Object getCache(ProceedingJoinPoint jp, SteamCache steamCache) throws Throwable {
        threadLocal.remove();//清除线程变量
        Object[] paramVals = jp.getArgs();//方法入口参数

        if(!developerEnableCache){
            //开发者禁用缓存，直接跳过；
            logger.info("====================================================================");
            logger.info("==== Developer Enable SteamCache configuration has be disable !!! ====");
            logger.info("====================================================================");
            return jp.proceed(paramVals);
        }

        if(steamCache == null){
            //第二次判断，未设置@SteamCache注解的不处理
            return jp.proceed(paramVals);
        }

        Class<?> classTarget = jp.getTarget().getClass();
        String methodName = jp.getSignature().getName();
        Class<?>[] par = ((MethodSignature) jp.getSignature()).getParameterTypes();
        Method objMethod = classTarget.getMethod(methodName, par);

        SteamExecutor steamExecutor = steamCache.executor();
        AbstractExecutor executor = SteamCacheSpringUtil.getBeanFromContext(steamExecutor.executorCls());
        executor.initHandlerInfo();
        try {
            Object cacheValue = executor.execute(AbstractExecutor.ExecutorAction.GET, steamCache, classTarget, objMethod, paramVals);//取
            if (cacheValue != null) {

                if(SteamCacheUtil.isNullValue(cacheValue)){//配置可缓存null值时会出现此判断
                    return null;
                }

                executor.tl_handlerInfo.get().setCacheValue(cacheValue);

                Boolean propagateFlag = executor.tl_handlerInfo.get().getMethodPropagateFlag().get(objMethod.hashCode());
                if (propagateFlag != null && propagateFlag) {
                    executor.execute(AbstractExecutor.ExecutorAction.PROPAGATE, steamCache, classTarget, objMethod, paramVals);//传播
                }

                return cacheValue;
            }

            Object methodObj = jp.proceed(paramVals);//方法执行

            executor.tl_handlerInfo.get().setMethodInvokeValue(methodObj);
            executor.execute(AbstractExecutor.ExecutorAction.PUT, steamCache, classTarget, objMethod, paramVals);//放

            return methodObj;
        }finally {
            executor.reset();
        }
    }

    //公共代码抽离
    private SteamCache getSteamCacheFromMethod(ProceedingJoinPoint jp){
        Class<?> classTarget = jp.getTarget().getClass();
        String methodName = jp.getSignature().getName();
        Class<?>[] par = ((MethodSignature) jp.getSignature()).getParameterTypes();
        SteamCache steamCache;
        try {
            Method objMethod = classTarget.getMethod(methodName, par);

            steamCache = objMethod.getAnnotation(SteamCache.class);//通过@SteamCache注解设置的方法进行拦截
        } catch (Exception e) {
            steamCache = null;
        }
        return steamCache;
    }

    @Around("cacheFactoryApi()")
    public Object getCacheFromFactory(ProceedingJoinPoint jp) throws Throwable{
        //返回工厂build()后的缓存层
        Class<?> classTarget = jp.getTarget().getClass();
        String methodName = jp.getSignature().getName();
        Object[] paramVals = jp.getArgs();//方法入口参数

        SteamCache steamCache = getSteamCacheFromMethod(jp);
        if(steamCache == null){
            //第二次判断，未设置@SteamCache注解的不处理
            return jp.proceed(paramVals);
        }

        SteamCacheUtil.SteamCacheIndex cacheIndex = steamCache.cacheIndex() != null ? steamCache.cacheIndex() : SteamCacheUtil.SteamCacheIndex.CACHE_CAHCE;
        String cacheKey = SteamCacheUtil.getMethodCacheKey(steamCache,classTarget.getName(),methodName,paramVals);
        SteamCacheCompose cacheCompose = steamCache.cacheCompose();

        Object cache = null;
        if(cacheCompose != null){
            Annotation[] annotations = SteamCacheCompose.class.getField(cacheCompose.toString()).getAnnotations();
            Assert.notEmpty(annotations,"SteamCacheConfig of SteamCacheCompose Enum can not be null");
            Annotation annotation = annotations[0];
            if(annotation instanceof SteamCacheConfig){
                String key = ((SteamCacheConfig)annotation).hashCode() + ":" + cacheIndex + ":" +cacheKey;
                threadLocal.set(key);
                //一级缓存
                cache = getCacheByConfig((SteamCacheConfig)annotation,cacheIndex,cacheKey);
            }else if (annotation instanceof SteamCacheConfigGroup){
                Assert.isTrue(false,annotation.toString() + " can not be favour，because of multi factory");
            }else{
                Assert.isTrue(false,annotation.toString() + " can not be favour");
            }
        }

        if(cache != null){
            threadLocal.remove();//清除线程变量
            return cache;
        }

        cache = jp.proceed(paramVals);//方法执行

        if(cache != null){
            SteamCacheUtil.put(cacheIndex,cacheKey,cache);
        }

        threadLocal.remove();//清除线程变量
        return cache;
    }

    /**
     * 根据cacheConfig获取缓存对象
     * @param cacheConfig
     * @param cacheIndex
     * @param cacheKey
     * @return
     */
    private Object getCacheByConfig(SteamCacheConfig cacheConfig, SteamCacheUtil.SteamCacheIndex cacheIndex, String cacheKey){
        //拿线程变量进行判断，防止factory.builder()构建的时候死循环；
        Object cache = null;
        SteamCacheType cacheType = cacheConfig.cacheType();
        String threadStr = threadLocal.get();

        if(StringUtils.isNotEmpty(threadStr)){
            cache = SteamCacheUtil.get(cacheIndex,cacheKey);
        }else{
            ISteamCacheMDFactory factoryBean = SteamCacheSpringUtil.getMDFactoryBean(cacheType.getFactoryClass());
            SteamCacheConfigAttribute[] configAttributes = cacheConfig.attributes();
            Map<SteamCacheAttributeConstant,String> attributeMap = null;
            if(ArrayUtils.isNotEmpty(configAttributes)){
                attributeMap = new HashMap<>();
                for(SteamCacheConfigAttribute attr : configAttributes){
                    attributeMap.put(attr.code(), StringUtils.isEmpty(attr.value()) ? attr.code().getDefaultValue() : attr.value());
                }
            }

            cache = factoryBean.build(attributeMap);
        }

        return cache;
    }

}
