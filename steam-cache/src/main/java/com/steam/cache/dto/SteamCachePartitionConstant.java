package com.steam.cache.dto;

/**
 * 隔离维护枚举，运行期，通过反射调用class下的方法获取值；
 * 暂时仅有租户、用户两个维度，后面需要其他可扩展（维度扩展、获取值方式扩展都可以）；
 */
public enum SteamCachePartitionConstant {
    //根据类名和方法名通过反射方法获取值
    //token(InvocationInfoProxy.class,"getToken",null),

    token(String.class,"getToken",null),
    tenant_id(String.class,"getTenantid",null),
    user_id(String.class,"getUserid",null),
    ext_a00(String.class,"getExtendAttribute","a00");

    private Class className;
    private String methodName;
    private Object methodParam;

    SteamCachePartitionConstant(Class className, String methodName, String methodParam){
        this.className = className;
        this.methodName = methodName;
        this.methodParam = methodParam;
    }

    public Class getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object getMethodParams() {
        return methodParam;
    }
}
