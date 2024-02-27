package com.steam.cache.executor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.steam.cache.annotation.SteamCache;
import com.steam.cache.annotation.SteamCacheConfig;
import com.steam.cache.annotation.SteamCacheConfigAttribute;
import com.steam.cache.annotation.SteamCacheConfigGroup;
import com.steam.cache.dto.SteamCacheAttributeConstant;
import com.steam.cache.dto.SteamCacheCompose;
import com.steam.cache.dto.SteamCacheType;
import com.steam.cache.itf.ISteamCacheMDCompetence;
import com.steam.cache.itf.ISteamCacheMDFactory;
import com.steam.cache.util.SteamCacheSpringUtil;
import com.steam.cache.util.SteamCacheUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.*;

public class GetExecutorActionHandler extends AbstractExecutorActionHandler<Object> {
    private static final Gson gson = new GsonBuilder().create();

    public GetExecutorActionHandler(AbstractExecutor abstractExecutor) {
        super(abstractExecutor);
    }

    @Override
    public Object execute(SteamCache steamCache, Class targetCls, Method method, Object... methodArgs) throws Exception {
        String cacheKey = SteamCacheUtil.getMethodCacheKey(steamCache,targetCls,method,methodArgs);
        String cacheKeyType = SteamCacheUtil.getMethodUsedCacheType(steamCache,method);
        SteamCacheCompose cacheCompose = steamCache.cacheCompose();//缓存层注解（指定提供缓存能力的分层枚举）

        Object cacheValue = null;
        boolean cfgValueCopyFlag = false;
        if(cacheCompose != null){
            Annotation[] annotations = SteamCacheCompose.class.getField(cacheCompose.toString()).getAnnotations();
            Assert.notEmpty(annotations,"SteamCacheConfig of SteamCacheCompose Enum can not be null");
            Annotation annotation = annotations[0];
            if(annotation instanceof SteamCacheConfig){
                //一级缓存
                abstractExecutor.tl_handlerInfo.get().getMethodPropagateFlag().put(method.hashCode(),false);//是否传播
                cfgValueCopyFlag = ((SteamCacheConfig)annotation).valueCopyFlag();
                Object cache = getCacheByConfig((SteamCacheConfig)annotation);
                if(cache != null) {
                    ISteamCacheMDCompetence competenceBean = SteamCacheSpringUtil.getMDCompetenceBean(((SteamCacheConfig) annotation).cacheType().getCompetenceClass(cacheKeyType));
                    cacheValue = competenceBean.get(cache, cacheKey);
                    abstractExecutor.tl_handlerInfo.get().getMethodCompetenceBeanMap().put(method.hashCode(),new LinkedHashMap<Object, ISteamCacheMDCompetence>(){
                        {put(cache,competenceBean);}
                    });
                }
            }else if (annotation instanceof SteamCacheConfigGroup){
                //多级缓存，顺序取值
                abstractExecutor.tl_handlerInfo.get().getMethodPropagateFlag().put(method.hashCode(),((SteamCacheConfigGroup)annotation).propagation());//是否传播
                LinkedHashMap<Object, ISteamCacheMDCompetence> cacheArgs = getCacheByConfigGroup((SteamCacheConfigGroup)annotation,cacheKeyType);
                List<Boolean> valueCopyFlagList = getValueCopyFlagByConfigGroup((SteamCacheConfigGroup)annotation);
                if(MapUtils.isNotEmpty(cacheArgs)) {
                    abstractExecutor.tl_handlerInfo.get().getMethodCompetenceBeanMap().put(method.hashCode(), new LinkedHashMap<>());

                    int i = 0;
                    for (Map.Entry<Object, ISteamCacheMDCompetence> entry : cacheArgs.entrySet()) {
                        cacheValue = entry.getValue().get(entry.getKey(), cacheKey);
                        abstractExecutor.tl_handlerInfo.get().getMethodCompetenceBeanMap().get(method.hashCode()).put(entry.getKey(),entry.getValue());
                        if (cacheValue != null) {//找到缓存值，退出循环
                            cfgValueCopyFlag = valueCopyFlagList.get(i);
                            break;
                        }
                        i++;
                    }
                }
            }else{
                Assert.isTrue(false,annotation.toString() + " can not be favour");
            }
        }

        if(cacheValue != null){
            //1、先走配置文件中valueCopyFlag判断逻辑；（已完成）
            //2、在走缓存层的valueCopyFlag判断逻辑；（已完成）

            //配置文件中的valueCopyFlag设置；（根据配置文件及场景上下文传递来综合判断）
            boolean valueCopyFlag = SteamCacheSpringUtil.getValueCopyCfgProperty();
            if(!valueCopyFlag){//不进行值拷贝操作，返回缓存原始值
                return cacheValue;
            }

            //走缓存层的valueCopyFlag判断逻辑
            if(!cfgValueCopyFlag){
                return cacheValue;
            }

            //深拷贝对象，防止下游操作对象污染缓存（针对业务对象数据，其中一些工厂类、代理类是不需要深拷贝的）
            if(cacheValue instanceof Proxy){
                //代理类型的时候，返回原值（factory工厂创建的实例）
                return cacheValue;
            }else if(SteamCacheUtil.isNullValue(cacheValue)){
                //允许缓存空值的时候
                logger.warn("===> SteamCacheAop : get value from Cache : (NULL VALUE)");
                return cacheValue;
            }else {
                //深拷贝缓存值对象（防止下游使用中污染数据）
                String jsonVal = "";
                Object copyCacheValue = null;
                try {
                    Type returnType = method.getGenericReturnType();
                    if((returnType instanceof ParameterizedType) && isGenericRuntimeType(returnType)){
                        //泛型 返回
                        returnType = cacheValue.getClass().getGenericSuperclass();//通过值拿到泛型方法的返回类型，若仍拿不到，则通过抛异常方式，返回原始缓存值；
                        if(isGenericRuntimeType(returnType)){
                            throw new RuntimeException("Generics Result can not be favour");
                        }
                    }
                    jsonVal = gson.toJson(cacheValue, returnType);
                    copyCacheValue = gson.fromJson(jsonVal, returnType);
                }catch (Exception ex){
                    //gson方式深拷贝对象发生异常，返回缓存原始值
                    logger.error(ex.getMessage(),ex);
                    logger.warn("can not identify method return type,return original cache value !");
                    logger.warn("===> SteamCacheAop : get value from Cache : {}", cacheValue);
                    return cacheValue;
                }

                long printLengthOff = SteamCacheSpringUtil.getApplicationContext().getEnvironment().getProperty("steamCache.console.maxPrintLength",Long.class,1024L);

                if(jsonVal.length() >= printLengthOff){
                    logger.warn("===> SteamCacheAop : get value from Cache : {}", "value too large ,skip output !!!");
                }else{
                    logger.warn("===> SteamCacheAop : get value from Cache : {}", jsonVal);
                }

                return copyCacheValue;
            }
        }

        return null;
    }

    public static boolean isGenericRuntimeType(Type type){
        Type[] types = ((ParameterizedType) type).getActualTypeArguments();
        String typeName = types[0].getTypeName();
        return "T".equals(typeName) || "E".equals(typeName);
    }



    /**
     * 根据cacheConfig获取缓存对象
     * @param cacheConfig
     * @return
     */
    private Object getCacheByConfig(SteamCacheConfig cacheConfig){
        //拿线程变量进行判断，防止factory.builder()构建的时候死循环；
        Object cache = null;
        SteamCacheType cacheType = cacheConfig.cacheType();
        /*String threadStr = SteamCacheAop.threadLocal.get();

        if(StringUtils.isNotEmpty(threadStr)){
            cache = SteamCacheUtil.get(cacheIndex,cacheKey);
        }else{*/
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
        //}

        return cache;
    }


    /**
     * 根据cacheConfigGroup获取缓存对象
     * @param cacheConfigGroup
     * @return
     */
    private LinkedHashMap<Object, ISteamCacheMDCompetence> getCacheByConfigGroup(SteamCacheConfigGroup cacheConfigGroup, String cacheKeyType){
        SteamCacheConfig[] configArgs = cacheConfigGroup.configGroup();
        LinkedHashMap<Object, ISteamCacheMDCompetence> map = new LinkedHashMap<>();

        for(int i = 0; i < configArgs.length; i++){
            Object cacheObj = getCacheByConfig(configArgs[i]);
            ISteamCacheMDCompetence cacheMDCompetence = SteamCacheSpringUtil.getMDCompetenceBean(configArgs[i].cacheType().getCompetenceClass(cacheKeyType));
            map.put(cacheObj,cacheMDCompetence);
        }
        return map;
    }

    private List<Boolean> getValueCopyFlagByConfigGroup(SteamCacheConfigGroup cacheConfigGroup){
        SteamCacheConfig[] configArgs = cacheConfigGroup.configGroup();
        List<Boolean> flagList = new ArrayList<>(configArgs.length);
        for(int i = 0; i < configArgs.length; i++){
            flagList.add(configArgs[i].valueCopyFlag());
        }
        return flagList;
    }
}
