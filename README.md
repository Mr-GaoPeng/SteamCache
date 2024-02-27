# SteamCache

## 简介
~~~
缓存层组件,是抽象出缓存能力，不关乎缓存实现方式的一种使用方式，目的是给服务层提供缓存能力，加快服务层响应速度；
当调用业务接口时，会优先从缓存中查找，未查找到缓存时才进行调用方法，调用方法结束后，会将结果放入缓存中，下次调用接口时就直接走缓存了。
缓存层可以多个间进行组合，例如本地缓存+分布式缓存方式
~~~

![image/SteamCache.png](image/SteamCache.png)

## 使用方式
在方法上使用@SteamCache注解即可;
```
@SteamCache
public T methodA(){
    //method impl
}
```

