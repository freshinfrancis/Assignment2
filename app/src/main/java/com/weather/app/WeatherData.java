package com.weather.app;

import java.util.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WeatherData {
    private Map<String, String> dataMap;
    private long lamportTimestamp;

    // Gson instance (can be reused)
    private static final Gson gson = new Gson();

    public WeatherData(Map<String, String> dataMap) {
        this.dataMap = dataMap;
    }

    // Create WeatherData from JSON string using Gson
    public static WeatherData fromJson(String jsonData) {
        // Parse the JSON string into a JsonObject
        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
        
        // Create a dataMap from the JsonObject
        Map<String, String> dataMap = new HashMap<>();
        for (String key : jsonObject.keySet()) {
            dataMap.put(key, jsonObject.get(key).getAsString());
        }
        return new WeatherData(dataMap);
    }

    // Convert WeatherData to JSON string using Gson
    public String toJson() {
        // Convert the dataMap into a JsonObject and then to a JSON string
        return gson.toJson(dataMap);
    }

    // Getters and Setters
    public String getId() {
        return dataMap.get("id");
    }

    public void setLamportTimestamp(long timestamp) {
        this.lamportTimestamp = timestamp;
    }

    public long getLamportTimestamp() {
        return lamportTimestamp;
    }
    
    public Map<String, String> getDataMap() {
        return dataMap;
    }
}
