package com.steam.cache.util;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;

public class SteamCacheArgumentsResolver {
    private static final ObjectMapper defaultOm = new ObjectMapper();
    private static final ObjectMapper typedOm = new ObjectMapper();

    private SteamCacheArgumentsResolver() {
    }

    @SneakyThrows
    public static Object[] resolve(String argsJson, Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return new Object[0];
        }
        JsonNode rootNode = defaultOm.readTree(argsJson);
        List<String> clz = rootNode.findValuesAsText("@class");
        ObjectMapper om = defaultOm;
        if (!clz.isEmpty()) {
            // 处理用父类型接收子类型参数的场景
            om = typedOm;
            BasicPolymorphicTypeValidator bptv = BasicPolymorphicTypeValidator
                    .builder()
                    .allowIfSubType(new BasicPolymorphicTypeValidator.TypeMatcher() {
                        @Override
                        public boolean match(MapperConfig<?> config, Class<?> clazz) {
                            return clz.contains(clazz.getName());
                        }
                    })
                    .build();
            om.activateDefaultTyping(bptv, ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS,
                    JsonTypeInfo.As.PROPERTY);
            rootNode = om.readTree(argsJson);
        }
        Type[] parameterTypes = method.getGenericParameterTypes();
        Object[] args = new Object[parameters.length];

        if (parameters.length == 1) {
            Type pType = parameterTypes[0];
            // 如果只有一个参数，先判断是否有跟参数名称相同的属性
            JsonNode paraNode = rootNode.get(parameters[0].getName());
            if (null != paraNode) {
                // 如果有，则将这个属性转换为参数
                try {
                    args[0] = om.readValue(om.treeAsTokens(paraNode), om.constructType(pType));
                } catch (Exception e) {
                    // 转换失败时，也尝试将整个json作为参数再转换一次，防止参数名和参数的属性名一致的情况引发的错误
                    args[0] = om.readValue(om.treeAsTokens(rootNode), om.constructType(pType));
                }
            } else {
                // 否则将整个json转换为参数
                args[0] = om.readValue(om.treeAsTokens(rootNode), om.constructType(pType));
            }
            return args;
        }

        for (int i = 0; i < parameters.length; i++) {
            JsonNode paraNode = rootNode.get(parameters[i].getName());
            if (null != paraNode) {
                args[i] = om.readValue(om.treeAsTokens(paraNode), om.constructType(parameterTypes[i]));
            }
        }
        return args;
    }
}
