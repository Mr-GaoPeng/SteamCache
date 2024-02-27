package com.steam.cache.dto;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ClassTypeSerialiableAdapter implements JsonSerializer<Class>, JsonDeserializer<Class> {

    @Override
    public Class deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String clsStr = json.getAsString();
        try {
            return Class.forName(clsStr);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonElement serialize(Class src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getName());
    }
}
