package com.steam.cache.aop;


import com.steam.cache.itf.ISteamCacheStat;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnExpression("#{'true'.equals('${steamCache.stat:false}')}")
public class SteamCacheStatAop {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${steamCache.stat.sizeOf:false}")
    private boolean sizeOf_switch;
    @Value("${steamCache.stat.bigKey:false}")
    private boolean bigKey_switch;
    @Value("${steamCache.stat.costTime:false}")
    private boolean costTime_switch;




    @Pointcut("execution(* com.steam.cache.competence..remove*(..))")
    public void removeCacheApi() {
        //ISteamCacheMDCompetence接口中remove清除缓存的方法
    }


    /**
     *  缓存层删除缓存前，对键值大小的计算输出；
     */
    @Around("removeCacheApi()")
    public Object sizeOfRemoveKey(ProceedingJoinPoint jp) throws Throwable {
        Object targetBean = jp.getTarget();
        String methodName = jp.getSignature().getName();//method name
        Object cacheObj = jp.getArgs()[0];// cache object
        String cacheKey = (String) jp.getArgs()[1];//cache key

        if(sizeOf_switch){
            if (targetBean instanceof ISteamCacheStat) {
                ISteamCacheStat steamCacheStat = (ISteamCacheStat) targetBean;
                if (methodName.endsWith("ByPrefix")) {
                    cacheKey += "*";
                }
                String sizeOf = steamCacheStat.humanSizeOf(cacheObj, cacheKey);
                if (StringUtils.isNotEmpty(sizeOf)) {
                    logger.error("===[仅统计]=== " + targetBean.getClass().getSimpleName() + " clean up cache of key " + cacheKey + " , size of cacheKey approx [" + sizeOf + "]");
                }
            }
        }


        long startTime = System.currentTimeMillis();
        Object returnObj =  jp.proceed(jp.getArgs());
        long endTime = System.currentTimeMillis();

        if(costTime_switch && endTime > startTime){
            logger.error("===[仅统计]=== " + targetBean.getClass().getSimpleName() + " [remove] cache of key " + cacheKey + " take [" + (endTime - startTime) + " ms]");
        }

        return returnObj;
    }



    @Pointcut("execution(* com.steam.cache.competence..put(..))")
    public void putCacheApi() {
        //ISteamCacheMDCompetence接口中put存放缓存
    }


    /**
     *  缓存层存放缓存前，对键值是否"大键"的判定；
     *  仅做判定，后续对判定大键是否继续存放入缓存层中，在考虑是否通过参数限制；
     */
    @Around("putCacheApi()")
    public Object isBigKey(ProceedingJoinPoint jp) throws Throwable {
        Object targetBean = jp.getTarget();
        //String methodName = jp.getSignature().getName();//method name
        Object cacheObj = jp.getArgs()[0];// cache object
        String cacheKey = (String) jp.getArgs()[1];//cache key
        Object cacheVal = jp.getArgs()[2];//cache value


        long startTime = System.currentTimeMillis();
        Object returnObj =  jp.proceed(jp.getArgs());
        long endTime = System.currentTimeMillis();
        if(costTime_switch && endTime > startTime){
            logger.error("===[仅统计]=== " + targetBean.getClass().getSimpleName() + " [put] cache of key " + cacheKey + " take [" + (endTime - startTime) + " ms]");
        }

        if(bigKey_switch) {
            if (targetBean instanceof ISteamCacheStat) {
                ISteamCacheStat steamCacheStat = (ISteamCacheStat) targetBean;

                boolean isBigKey = steamCacheStat.isBigKey(cacheObj,cacheKey,cacheVal);
                if (isBigKey) {
                    logger.error("===[仅统计]=== " + targetBean.getClass().getSimpleName() + " judge cache of key " + cacheKey + " is a big key");
                }
            }
        }

        return returnObj;
    }



    @Pointcut("execution(* com.steam.cache.competence..get(..))")
    public void getCacheApi() {
        //ISteamCacheMDCompetence接口中put存放缓存
    }

    @Around("getCacheApi()")
    public Object getTakeTimeStat(ProceedingJoinPoint jp) throws Throwable {
        Object targetBean = jp.getTarget();
        //String methodName = jp.getSignature().getName();//method name
        //Object cacheObj = jp.getArgs()[0];// cache object
        String cacheKey = (String) jp.getArgs()[1];//cache key


        long startTime = System.currentTimeMillis();
        Object returnObj =  jp.proceed(jp.getArgs());
        long endTime = System.currentTimeMillis();
        if(costTime_switch && endTime > startTime){
            logger.error("===[仅统计]=== " + targetBean.getClass().getSimpleName() + " [get] cache of key " + cacheKey + " take [" + (endTime - startTime) + " ms]");
        }

        return returnObj;
    }
}
