package com.galdiuz.manga.classes;

import com.galdiuz.manga.classes.Chapter;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class ChapterDeserializer implements JsonDeserializer<Chapter> {
    @Override
    public Chapter deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonArray jsonArray = json.getAsJsonArray();
        Double number = jsonArray.get(0).getAsDouble();
        Double date = jsonArray.get(1).getAsDouble();
        String title;
        if(!jsonArray.get(2).isJsonNull()) {
            title = jsonArray.get(2).getAsString();
        }
        else {
            title = "";
        }
        String id = jsonArray.get(3).getAsString();
        return new Chapter(number, date, title, id);
    }
}