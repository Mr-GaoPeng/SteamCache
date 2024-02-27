package com.steam.cache.event.dto;

public enum SteamCacheNotifyType {
    CREATE(SteamCacheEvent.class),
    DELETE(SteamCacheEventCleanUp.class),
    UPDATE(SteamCacheEvent.class);

    private Class eventClass;

    SteamCacheNotifyType(Class eventClass){
        this.eventClass = eventClass;
    }

    public Class getEventClass() {
        return eventClass;
    }
}
