package com.steam.cache.dto;

import com.steam.cache.util.SteamCacheUtil;

import java.util.TimerTask;

public class SteamCacheTimerTask extends TimerTask {

    private String cacheKey;


    public SteamCacheTimerTask(String cacheKey){
        this.cacheKey = cacheKey;
    }

    @Override
    public void run() {
        if(SteamCacheUtil.cacheKeyMap.keySet().contains(cacheKey)){
            //System.out.println("SteamCacheTimerTask : local clean cachekey " + cacheKey);
            SteamCacheUtil.cacheKeyMap.remove(cacheKey);
        }
    }
}
