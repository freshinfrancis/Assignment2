package com.weather.app;

import java.io.*;
import java.net.*;
import java.util.*;

public class GETClient {
 private static LamportClock lamportClock = new LamportClock();

 public static void main(String[] args) {
     if (args.length < 1) {
         System.out.println("Usage: java GETClient <server_host>:<port> [station_id]");
         return;
     }

     String serverAddress = args[0];
     String stationId = args.length >= 2 ? args[1] : null;

     String[] serverParts = serverAddress.split(":");
     String host = serverParts[0];
     int port = Integer.parseInt(serverParts[1]);

     try {
         // Send HTTP GET request
         Socket socket = new Socket(host, port);
         BufferedWriter out = new BufferedWriter(
                 new OutputStreamWriter(socket.getOutputStream()));
         BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream()));

         String path = "/weather";
         if (stationId != null) {
             path += "?id=" + URLEncoder.encode(stationId, "UTF-8");
         }

         synchronized (lamportClock) {
             lamportClock.increment();
         }

         out.write("GET " + path + " HTTP/1.1\r\n");
         out.write("Host: " + host + "\r\n");
         out.write("Lamport-Timestamp: " + lamportClock.getTime() + "\r\n");
         out.write("\r\n");
         out.flush();

         // Read response status line
         String statusLine = in.readLine();
         if (statusLine == null || !statusLine.contains("200")) {
             System.out.println("Failed to get data from server.");
             socket.close();
             return;
         }

         // Read headers
         String line;
         int contentLength = 0;
         while (!(line = in.readLine()).isEmpty()) {
             if (line.startsWith("Content-Length:")) {
                 contentLength = Integer.parseInt(line.split(": ")[1]);
             }
         }

         // Read body
         char[] bodyChars = new char[contentLength];
         in.read(bodyChars);
         String responseBody = new String(bodyChars);

         // Parse and display weather data
         displayWeatherData(responseBody);

         socket.close();

     } catch (IOException e) {
         e.printStackTrace();
     }
 }

 private static void displayWeatherData(String jsonData) {
     // Remove JSON formatting and display attributes
     jsonData = jsonData.trim();
     if (jsonData.startsWith("[")) {
         jsonData = jsonData.substring(1, jsonData.length() - 1);
     }
     String[] dataObjects = jsonData.split("\\},\\{");
     for (String dataObject : dataObjects) {
         dataObject = dataObject.replaceAll("[\\{\\}]", "");
         String[] attributes = dataObject.split(",");
         for (String attribute : attributes) {
             String[] keyValue = attribute.split(":", 2);
             String key = keyValue[0].replaceAll("\"", "").trim();
             String value = keyValue[1].replaceAll("\"", "").trim();
             System.out.println(key + ": " + value);
         }
         System.out.println("-----------------------------------");
     }
 }
}

