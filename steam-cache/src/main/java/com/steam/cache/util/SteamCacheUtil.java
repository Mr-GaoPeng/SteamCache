package com.steam.cache.util;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.steam.cache.annotation.SteamCache;
import com.steam.cache.dto.SteamCachePartitionConstant;
import com.steam.cache.dto.SteamCacheTimerTask;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存操作类，意在维护本地缓存；
 * 通过caffeine中间件维护；
 */
public class SteamCacheUtil {
    private static Logger logger = LoggerFactory.getLogger(SteamCacheUtil.class);
    private static final Map<SteamCacheIndex,Object> cacheIndexMap;//存放缓存引用的Map缓存

    private static final Cache<String, Object> DEFAULT_CACHE;//默认缓存
    private static final Cache<String, Object> BENCH_TENANT_SERVICES;//工作台缓存
    private static final Cache<String, Object> USERCENTER_TENANT_USER;//用户缓存
    private static final Cache<String,Object> AUTH_TENANT_ROLE;//角色缓存


    public static Map<String,Long> cacheKeyMap = new ConcurrentHashMap<>();//缓存key的集合

    private static final Timer scheduCleanKeyTimer = new Timer();

    public enum SteamCacheIndex {
        DEFAULT_CACHE("DEFAULT_CACHE","DEFAULT_CACHE"),
        BENCH_CACHE("BENCH_CACHE","BENCH_TENANT_SERVICES"),
        USER_CACHE("USER_CACHE","USERCENTER_TENANT_USER"),
        ROLE_CACHE("ROLE_CACHE","AUTH_TENANT_ROLE"),

        CACHE_CAHCE("CACHE_CAHCE","cacheIndexMap");//特殊使用，存放动态创建的缓存


        private String code;
        private String index;
        SteamCacheIndex(String indexCode, String index){
            this.code = indexCode;
            this.index = index;
        }

        public String getCode(){
            return code;
        }
    }


    static {
        cacheIndexMap = new HashMap<>();

        DEFAULT_CACHE = Caffeine.newBuilder()
                .expireAfterWrite(5*60, TimeUnit.SECONDS)
                .maximumSize(500)
                .build();

        BENCH_TENANT_SERVICES = Caffeine.newBuilder()
                .expireAfterWrite(5*60, TimeUnit.SECONDS)
                .maximumSize(500)
                .build();

        USERCENTER_TENANT_USER = Caffeine.newBuilder()
                .expireAfterWrite(5*60, TimeUnit.SECONDS)
                .maximumSize(500)
                .build();

        AUTH_TENANT_ROLE = Caffeine.newBuilder()
                .expireAfterWrite(5*60, TimeUnit.SECONDS)
                .maximumSize(500)
                .build();

        cacheIndexMap.put(SteamCacheIndex.DEFAULT_CACHE,DEFAULT_CACHE);
        cacheIndexMap.put(SteamCacheIndex.BENCH_CACHE,BENCH_TENANT_SERVICES);
        cacheIndexMap.put(SteamCacheIndex.USER_CACHE,USERCENTER_TENANT_USER);
        cacheIndexMap.put(SteamCacheIndex.ROLE_CACHE,AUTH_TENANT_ROLE);

        cacheIndexMap.put(SteamCacheIndex.CACHE_CAHCE,cacheIndexMap);
    }


    public static void put(SteamCacheIndex cacheIndexKey, String cacheKey, Object cacheValue){
        Assert.notNull(cacheIndexKey,"SteamCacheIndex can not be null");
        Assert.hasLength(cacheKey,"cacheKey can not be null");
        Assert.notNull(cacheValue,"cacheValue can not be null");

        Object cache = cacheIndexMap.get(cacheIndexKey);
        if(cache instanceof Cache){
            ((Cache)cache).put(cacheKey,cacheValue);
        }else if(cache instanceof Map){
            ((Map) cache).put(cacheKey,cacheValue);
        }else{
            Assert.isTrue(false,"type can not be favour");
        }
    }

    public static Object get(SteamCacheIndex cacheIndexKey, String cacheKey){
        Assert.notNull(cacheIndexKey,"SteamCacheIndex can not be null");
        Assert.hasLength(cacheKey,"cacheKey can not be null");

        Object cache = cacheIndexMap.get(cacheIndexKey);
        if(cache instanceof Cache){
            return ((Cache)cache).getIfPresent(cacheKey);
        }else if(cache instanceof Map){
            return ((Map) cache).get(cacheKey);
        }else{
            Assert.isTrue(false,"type can not be favour");
            return null;
        }
    }


    /**
     * 清除指定分类的缓存
     * @param cacheIndexKey
     */
    public static void clearUpCache(SteamCacheIndex cacheIndexKey){
        Assert.notNull(cacheIndexKey,"SteamCacheIndex can not be null");

        Object cache = cacheIndexMap.get(cacheIndexKey);
        if(cache instanceof Cache){
            ((Cache)cache).cleanUp();
        }else if(cache instanceof Map){
            ((Map) cache).clear();
        }else{
            Assert.isTrue(false,"type can not be favour");
        }

    }

    /**
     * todo 全部清空能力减弱，仅支持caffine类型的缓存集合清空，其他集合等考虑好或者有具体场景在弄
     */
    private static void clearUpAll(){
        for(Map.Entry<SteamCacheIndex,Object> entry : cacheIndexMap.entrySet()){
            Object cache = cacheIndexMap.get(entry.getKey());
            if(!(cache instanceof Cache)){
                continue;
            }
            ((Cache)cache).cleanUp();
        }
    }


    /**
     * 根据类名、方法、入参获取方法缓存键全名
     * @param cls   类名
     * @param method    方法
     * @param objectArgs    入参
     * @return  缓存键全名
     */
    public static String getMethodCacheKey(SteamCache steamCache, Class cls, Method method, Object... objectArgs) throws Exception{
        Assert.notNull(steamCache,"steamCache can not be null");
        Assert.notNull(cls,"cls can not be null");
        Assert.notNull(method,"method can not be null");

        return getMethodCacheKey(steamCache,cls.getName(),method.getName(),objectArgs);
    }

    public static String[] getMethodCacheModKeys(SteamCache steamCache, Class cls, Method method, Object... objectArgs) throws Exception{
        int modSplitCount = 0;//steamCache.modSplitCount();

        String beforModCacheKey = getMethodCacheKey(steamCache, cls, method, objectArgs);
        if(modSplitCount > 0){
            //需要分割
            String[] modCacheKeyArgs = new String[modSplitCount];
            for(int i=0;i<modSplitCount;i++){
                modCacheKeyArgs[i] = beforModCacheKey + ":-" + i;
            }
            return modCacheKeyArgs;
        }else{
            //不需要分割
            return new String[]{beforModCacheKey};
        }
    }


    public static String getMethodCacheKey(SteamCache steamCache, String clsName, String methodName, Object... methodArgs) throws Exception{
        Assert.notNull(steamCache,"steamCache can not be null");
        Assert.hasLength(clsName,"clsName can not be null");
        Assert.hasLength(methodName,"methodName can not be null");

        String cacheKeyPrefix = getMethodCacheKeyPrefix(clsName,methodName);
        return getAssembleMethodCacheKey(steamCache, cacheKeyPrefix,null,methodArgs);
    }

    public static String getMethodCacheKeyPrefix(String clsName,String methodName){
        Assert.hasLength(clsName,"clsName can not be null");
        Assert.hasLength(methodName,"methodName can not be null");
        //类名.方法名 作为前缀；eg：c.y.a.o.i.AdminOrgServerClientService:getOrgChildsByCodeAndType
        StringBuilder builder = new StringBuilder();
        String[] clsStrArgs = clsName.split("\\.");
        Arrays.stream(clsStrArgs).limit(clsStrArgs.length-1).forEach(data->builder.append(StringUtils.substring(data,0,1)).append("."));
        Arrays.stream(clsStrArgs).skip(clsStrArgs.length-1).forEach(data->builder.append(data));
        builder.append(":").append(methodName);
        return builder.toString();
    }

    public static String getAssembleMethodCacheKey(SteamCache steamCache, String keyPrefix, String keySuffix, Object... methodArgs) throws Exception {
        Assert.notNull(steamCache,"steamCache can not be null");

        String cacheKeyPrefix = StringUtils.isNotEmpty(steamCache.cacheKeyPrefix()) ? steamCache.cacheKeyPrefix() : keyPrefix;
        if(StringUtils.isEmpty(cacheKeyPrefix)){
            //prefix is null
            return null;
        }
        //#2023.08.09 生成键前缀，添加SteamCache:前缀，定义一个目录专门存放SteamCache缓存；
        //StringBuilder strBuilder = new StringBuilder("SteamCache:" + cacheKey);

        List<String> keyItemList = new ArrayList<>();
        keyItemList.add("SteamCache");
        keyItemList.add(cacheKeyPrefix);

        //List<Object> objectList = new ArrayList<>();
        boolean hashFlag = false;
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder(17,37);
        //隔离维度
        SteamCachePartitionConstant[] partitions = steamCache.partitions();
        if(ArrayUtils.isNotEmpty(partitions)){
            for(SteamCachePartitionConstant item : partitions){
                Class itemCls = item.getClassName();
                String itemMethName = item.getMethodName();
                Object itemMethodParam = item.getMethodParams();

                Object reflectVal;
                if(itemMethodParam == null){
                    reflectVal = itemCls.getMethod(itemMethName).invoke(null,new  Object[]{});
                }else{
                    reflectVal = itemCls.getMethod(itemMethName,String.class).invoke(null,new Object[]{itemMethodParam});
                }
                if(reflectVal == null){continue;}
                //objectList.add(reflectVal);
                hashCodeBuilder.append(reflectVal);
                hashFlag = true;
            }
        }

        //方法入参数
        if(ArrayUtils.isNotEmpty(methodArgs)){
            //objectList.addAll(Arrays.asList(methodArgs));
            hashCodeBuilder.append(methodArgs);
            hashFlag = true;
        }
        //strBuilder.append(":").append(hashCodeBuilder.hashCode());
        if(hashFlag) {
            keyItemList.add(String.valueOf(hashCodeBuilder.hashCode()));
        }

        if(StringUtils.isNotEmpty(keySuffix)){//拼后缀
            //strBuilder.append(":").append(keySuffix);
            keyItemList.add(keySuffix);
        }

        //todo 拼接版本信息
        //if(steamCache.cacheVersioned()){
            //带版本的缓存
            //String versionCatalogKey = getCacheVersionKey();//版本目录key
            //String outVerCacheKey = getCacheKeyWithOutVersion(cacheKey);//不带version的缓存Key
        //}

        return assembleCacheKey(keyItemList.toArray(new String[0]));
    }

    public static String assembleCacheKey(String... keyItems){
        if(ArrayUtils.isEmpty(keyItems)){
            return null;
        }
        String cacheKey = Arrays.stream(keyItems).collect(Collectors.joining(":"));
        return cacheKey;
    }

    public static String getCacheVersionKey(){
        return "SteamCache:catalog:version";
    }

    public static String getNewVersionCacheKey(String cacheKey,Long version){
        String newCachePrex = getCacheKeyWithOutVersion(cacheKey);
        return newCachePrex + ":" + version;
    }

    public static String getCacheKeyWithOutVersion(String versionCacheKey){
        String[] args = versionCacheKey.split(":");
        String newCachePrex = Arrays.stream(args).limit(args.length-1).collect(Collectors.joining(":"));
        return newCachePrex;
    }

    /**
     * 根据类名、方法获取方法缓存通知通道
     * @param cls   类名
     * @param method    方法
     * @return  方法通知通道
     * 默认值：SteamCacheIndex.DEFAULT_CACHE.getCode()
     */
    public static String getMethodNotifyChannel(Class cls,Method method){
        SteamCache steamCache = getMethodSteamCache(cls,method);
        return steamCache == null ? SteamCacheIndex.DEFAULT_CACHE.getCode() : steamCache.cacheIndex().getCode();
    }

    private static SteamCache getMethodSteamCache(Class cls, Method method){
        SteamCache steamCache = method.getAnnotation(SteamCache.class);
        if(steamCache == null){//根据cls 二次查找方法上的SteamCache注解
            Object clsBean = SteamCacheSpringUtil.getBeanFromContext(cls);
            if(clsBean == null){
                logger.error("notify only ===> can not getBean from Context by Class : " + cls);
                return null;
            }
            if(AopUtils.isAopProxy(clsBean)){
                clsBean = AopProxyUtils.getSingletonTarget(clsBean);
            }
            Method reFindMethod = null;
            try {
                reFindMethod = clsBean.getClass().getMethod(method.getName(),method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                return null;
            }
            steamCache = reFindMethod.getAnnotation(SteamCache.class);
            if(steamCache == null){
                return null;
            }
        }
        return steamCache;
    }

    public static String getMethodUsedCacheType(SteamCache steamCache, Method method){

        if(!steamCache.autoMethodCacheType()){//非自动按照方法返回值确定使用缓存基本类型，返回默认String类型；
            return "String";
        }


        if(Collection.class.isAssignableFrom(method.getReturnType())){
            //集合类型
            return "List";
        }

        if(Map.class.isAssignableFrom(method.getReturnType())){
            //Map类型
            return "Map";
        }

        return "String";
    }

    public static void localPutCacheKeyWithDelay(String cacheKey,long delay){
        if(!cacheKeyMap.keySet().contains(cacheKey)) {
            cacheKeyMap.put(cacheKey, delay);
            scheduCleanKeyTimer.schedule(new SteamCacheTimerTask(cacheKey), delay);
        }
    }

    public static final int EMPTY_LIST_HASHCODE = "steamCacheRedisListNullValue".hashCode();
    public static final int EMPTY_MAP_HASHCODE = "steamCacheRedisMapNullValue".hashCode();

    public static boolean isNullValue(Object obj){
        if (obj == ObjectUtils.NULL) {
            return true;
        }

        if (obj instanceof Collection) {
            if (obj.hashCode() == EMPTY_LIST_HASHCODE) {
                return true;
            }

            if (!((Collection) obj).isEmpty() && ((Collection) obj).stream().findFirst().get() == ObjectUtils.NULL) {
                return true;
            }
        }


        if (obj instanceof Map) {
            if (obj.hashCode() == EMPTY_MAP_HASHCODE) {
                return true;
            }

            if (((Map) obj) != null && ((Map) obj).containsKey("$null") && ((Map) obj).get("$null") == ObjectUtils.NULL) {
                return true;
            }
        }

        return false;
    }
}
