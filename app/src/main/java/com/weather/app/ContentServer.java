package com.weather.app;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class ContentServer {
 private static LamportClock lamportClock = new LamportClock();

 public static void main(String[] args) {
     if (args.length < 2) {
         System.out.println("Usage: java ContentServer <server_host>:<port> <data_file>");
         return;
     }

     String serverAddress = args[0];
     String dataFilePath = args[1];

     String[] serverParts = serverAddress.split(":");
     String host = serverParts[0];
     int port = Integer.parseInt(serverParts[1]);

     try {
         // Read data from file
         String data = new String(Files.readAllBytes(Paths.get(dataFilePath)));
         Map<String, String> dataMap = parseDataFile(data);

         // Create WeatherData object
         WeatherData weatherData = new WeatherData(dataMap);
         synchronized (lamportClock) {
             lamportClock.increment();
             weatherData.setLamportTimestamp(lamportClock.getTime());
         }

         // Convert to JSON
         String jsonData = weatherData.toJson();

         // Send HTTP PUT request
         Socket socket = new Socket(host, port);
         BufferedWriter out = new BufferedWriter(
                 new OutputStreamWriter(socket.getOutputStream()));
         BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream()));

         out.write("PUT /weather HTTP/1.1\r\n");
         out.write("Host: " + host + "\r\n");
         out.write("Content-Type: application/json\r\n");
         out.write("Content-Length: " + jsonData.length() + "\r\n");
         out.write("Lamport-Timestamp: " + lamportClock.getTime() + "\r\n");
         out.write("\r\n");
         out.write(jsonData);
         out.flush();

         // Read response
         String responseLine;
         while (!(responseLine = in.readLine()).isEmpty()) {
             // Process response headers if needed
         }

         socket.close();
         System.out.println("Data sent to Aggregation Server.");

     } catch (IOException e) {
         e.printStackTrace();
     }
 }

 private static Map<String, String> parseDataFile(String data) {
     Map<String, String> dataMap = new HashMap<>();
     String[] lines = data.split("\n");
     for (String line : lines) {
         String[] keyValue = line.trim().split(":", 2);
         if (keyValue.length == 2) {
             dataMap.put(keyValue[0], keyValue[1]);
         }
     }
     return dataMap;
 }
}
