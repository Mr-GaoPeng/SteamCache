package com.steam.cache.config;

import com.steam.cache.annotation.SteamCachePreHeat;
import com.steam.cache.dto.PreHeatType;
import com.steam.cache.itf.ISteamCachePreHeat;
import com.steam.cache.util.SteamCacheArgumentsResolver;
import com.steam.cache.util.SteamCacheSpringUtil;
import com.steam.cache.util.CacheYmsPropertyUtil;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 启动时缓存预热处理
 */
@Component
public class SteamCachePreHeatConfiguration implements ApplicationRunner {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(ApplicationArguments args) {
        try{
            preHeatInvokeMethodCache();
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
    }


    public void preHeatInvokeMethodCache() throws Exception{
        Map<String, ISteamCachePreHeat> beanMap = SteamCacheSpringUtil.getApplicationContext().getBeansOfType(ISteamCachePreHeat.class);
        if(MapUtils.isEmpty(beanMap)){return;}//没有IApp  CachePreHeat接口实现，返回

        Map<Method, ISteamCachePreHeat> preHeatMethodMap = new HashMap<>();
        Map<Method,Object[]> preHeatMethodParamMap = new HashMap<>();
        for(Map.Entry<String, ISteamCachePreHeat> entry : beanMap.entrySet()){
            ISteamCachePreHeat preHeat = entry.getValue();
            if(AopUtils.isAopProxy(preHeat)){
                preHeat = (ISteamCachePreHeat)AopProxyUtils.getSingletonTarget(preHeat);
            }
            ISteamCachePreHeat finalPreHeat = preHeat;
            Arrays.stream(finalPreHeat.getClass().getMethods()).filter(method -> method.getAnnotation(SteamCachePreHeat.class) != null && PreHeatType.CONTAINER_STARTED == method.getAnnotation(SteamCachePreHeat.class).preHeatType()).forEach(method->{
                SteamCachePreHeat preHeat_at = method.getAnnotation(SteamCachePreHeat.class);
                //String code = preHeat_at.code();//集中式预热数据时用到，可以根据code查找执行预热入参数；
                String tplJson = preHeat_at.tplJson();//方法上定义的模版入参数（这里）
                Set<String> propertySet = regexTplJsonProperty(tplJson);
                Map<String,String> propertyValMap = propertySet.stream().collect(Collectors.toMap(key->key,key->{
                    String propertyVal = CacheYmsPropertyUtil.getProperty(key, false);
                    if(propertyVal == null){
                        propertyVal = "";
                    }
                    return propertyVal;
                }));

                for(Map.Entry<String,String> property : propertyValMap.entrySet()){
                    tplJson = tplJson.replace("${"+property.getKey()+"}",property.getValue());
                }

                Object[] paramArgs = SteamCacheArgumentsResolver.resolve(tplJson,method);

                preHeatMethodMap.put(method, finalPreHeat);
                preHeatMethodParamMap.put(method,paramArgs);
            });
        }

        if(MapUtils.isEmpty(preHeatMethodMap)){return;}//没有@SteamCachePreHeat注解的方法，返回

        logger.info("==> SteamCachePreHeatConfiguration preheat method cache begin ");
        long startTime = System.currentTimeMillis();

        for(Map.Entry<Method, ISteamCachePreHeat> entry : preHeatMethodMap.entrySet()){
            Method method = entry.getKey();
            method.invoke(entry.getValue(),preHeatMethodParamMap.get(method));
        }


        long endTime = System.currentTimeMillis();
        logger.info("==> SteamCachePreHeatConfiguration preheat method cache end ,cost {} ms",(endTime - startTime));
    }

    private static Set<String> regexTplJsonProperty(String jsonTpl){
        Set<String> variables = new HashSet<>();
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
        Matcher matcher = pattern.matcher(jsonTpl);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables;
    }

}
