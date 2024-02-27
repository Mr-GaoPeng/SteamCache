package com.steam.cache.dto;

public enum PreHeatType {
    //容器启动后（spring容器启动后出发）
    CONTAINER_STARTED,
    //事件驱动（手动发事件、程序发送事件等等，这里是事件型响应方式）
    EVENT_DRIVED;



    //***********************
    //后续设想的预热类型，可能会有
    //规则型，根据参数设置动态决定是否预热
    //
    //***********************
}
