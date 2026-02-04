package com.smarttask.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class GsonUtil {

    public static Gson createLocalDateTimeGsonAdapter() {
        return new GsonBuilder()
            .registerTypeAdapter(
                LocalDateTime.class,
                (com.google.gson.JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                    src == null ? null :
                    new com.google.gson.JsonPrimitive(
                        src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    )
            )
            .registerTypeAdapter(
                LocalDateTime.class,
                (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                    json == null || json.isJsonNull()
                        ? null
                        : LocalDateTime.parse(
                            json.getAsString(),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        )
            )
            .create();
    }

    private GsonUtil() {}
}

