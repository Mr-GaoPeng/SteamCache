package com.steam.cache.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * cache redis componse configuration
 * author : gaopengj
 */

@Configuration
public class SteamRedisCacheConfiguration {

    // config set "get" operate time out when cache is redis componse
    // config set value come from properties file ,default time out value is 5000 ms
    public static long getTimeOut;

    public static TimeUnit getTimeOutUnit = TimeUnit.MILLISECONDS;


    @Value(value = "${steamCache.get.timeOut:5000}")
    public void setGetTimeOut(long getTimeOut) {
        SteamRedisCacheConfiguration.getTimeOut = getTimeOut;
    }
}
