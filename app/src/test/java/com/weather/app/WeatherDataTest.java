package com.weather.app;

//File: WeatherDataTest.java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

public class WeatherDataTest {

 @Test
 public void testToJson() {
     Map<String, String> dataMap = new HashMap<>();
     dataMap.put("id", "IDS60901");
     dataMap.put("name", "Adelaide");
     dataMap.put("state", "SA");

     WeatherData weatherData = new WeatherData(dataMap);
     String json = weatherData.toJson();

     assertTrue(json.contains("\"id\":\"IDS60901\""));
     assertTrue(json.contains("\"name\":\"Adelaide\""));
     assertTrue(json.contains("\"state\":\"SA\""));
 }

 @Test
 public void testFromJson() {
     String jsonData = "{\"id\":\"IDS60901\",\"name\":\"Adelaide\",\"state\":\"SA\"}";
     WeatherData weatherData = WeatherData.fromJson(jsonData);

     assertEquals("IDS60901", weatherData.getId());
     assertEquals("Adelaide", weatherData.getDataMap().get("name"));
     assertEquals("SA", weatherData.getDataMap().get("state"));
 }

 @Test
 public void testLamportTimestamp() {
     Map<String, String> dataMap = new HashMap<>();
     dataMap.put("id", "IDS60901");
     WeatherData weatherData = new WeatherData(dataMap);
     weatherData.setLamportTimestamp(10);

     assertEquals(10, weatherData.getLamportTimestamp());
 }
}
