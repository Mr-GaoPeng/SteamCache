package com.steam.cache.util;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import java.util.concurrent.ConcurrentHashMap;


/**
 * 参照 YmsPropertyUtil 类复制而来，减少yms依赖
 */
@Configuration
public class CacheYmsPropertyUtil implements EnvironmentAware {

    private static final String NULL_OBJECT = new String();

    private static ConcurrentHashMap<String, String> PROPERTY_CACHE = new ConcurrentHashMap<>();

    public static void removePropertyCache(String key) {
        PROPERTY_CACHE.remove(key);
    }

    public static String getProperty(String key, boolean system) {
        Assert.notNull(environment, "请在environment对象初始化后调用此方法");
        String val = PROPERTY_CACHE.computeIfAbsent(key, k -> {
            String ret = system ? System.getProperty(k) : environment.getProperty(k);
            return null == ret ? NULL_OBJECT : ret;
        });
        if (NULL_OBJECT == val) {
            return null;
        }
        return val;
    }

    private static Environment environment;

    @Override
    public  void setEnvironment(Environment environment) {
        if (null == CacheYmsPropertyUtil.environment) {
            CacheYmsPropertyUtil.environment = environment;
        }
    }
    public static void main(String[] args) {
        PROPERTY_CACHE.put("a",NULL_OBJECT);
        System.out.println(PROPERTY_CACHE.get("a"));
        System.out.println(PROPERTY_CACHE.get("a")==NULL_OBJECT);
    }
}
