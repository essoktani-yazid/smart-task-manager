package com.smarttask.server.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;

public final class GsonUtils {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final Gson GSON = createGson();

    private GsonUtils() {}

    public static Gson getGson() {
        return GSON;
    }

    private static Gson createGson() {
        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            @Override
            public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src.format(DATE_TIME_FORMATTER));
            }
        });
        builder.registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return LocalDateTime.parse(json.getAsString(), DATE_TIME_FORMATTER);
            }
        });

        builder.registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
            @Override
            public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src.format(DATE_FORMATTER));
            }
        });
        builder.registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
            @Override
            public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return LocalDate.parse(json.getAsString(), DATE_FORMATTER);
            }
        });

        builder.registerTypeAdapter(LocalTime.class, new JsonSerializer<LocalTime>() {
            @Override
            public JsonElement serialize(LocalTime src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_TIME));
            }
        });
        builder.registerTypeAdapter(LocalTime.class, new JsonDeserializer<LocalTime>() {
            @Override
            public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return LocalTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_TIME);
            }
        });

        builder.serializeNulls();
        return builder.create();
    }
}
