package com.steam.cache.event.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

import java.lang.reflect.Method;

@Getter
@Setter
@ToString
public class SteamCacheEvent<T> extends ApplicationEvent{
    private Class<T> cls;
    private Method method;

    public SteamCacheEvent(Object source, Class<T> cls, Method method){
        super(source);
        this.cls = cls;
        this.method = method;
    }
}
