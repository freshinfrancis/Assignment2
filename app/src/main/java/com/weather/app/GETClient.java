// File: GETClient.java
package com.weather.app;

import java.io.*;
import java.net.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GETClient {
    private static String serverAddress;
    private static int serverPort;
    private static String stationId;
    private static final LamportClock lamportClock = new LamportClock();

    public static void main(String[] args) {

        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: java GETClient <server-address:port> [station-id]");
            return;
        }

        String serverInfo = args[0];
        if (args.length == 2) {
            stationId = args[1];
        }

        // Split the server info into address and port
        String[] serverParts = serverInfo.split(":");
        if (serverParts.length != 2) {
            System.out.println("Invalid server info format. Expected <server-address:port>");
            return;
        }

        serverAddress = serverParts[0];
        try {
            serverPort = Integer.parseInt(serverParts[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number: " + serverParts[1]);
            return;
        }

        try {
            String jsonResponse = sendGetRequest();
            if (jsonResponse != null) {
                displayWeatherData(jsonResponse);
            } else {
                System.out.println("No response from server.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sendGetRequest() throws IOException {
        lamportClock.tick(); // Increment Lamport clock before sending

        try (Socket socket = new Socket(serverAddress, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Prepare HTTP GET request with Lamport-Clock header
            StringBuilder request = new StringBuilder();
            request.append("GET /weather.json HTTP/1.1\r\n");
            request.append("Host: ").append(serverAddress).append("\r\n");
            request.append("User-Agent: GETClient/1.0\r\n");
            request.append("Accept: application/json\r\n");
            request.append("Lamport-Clock: ").append(lamportClock.getClock()).append("\r\n"); // Include Lamport clock
            request.append("\r\n"); // End of headers

            // Send request
            out.print(request.toString());
            out.flush();

            // Read response status line
            String statusLine = in.readLine();
            if (statusLine == null || !statusLine.contains("200")) {
                System.out.println("Failed to get data from server.");
                return null;
            }

            // Read headers and extract Lamport-Clock
            String line;
            int serverLamportClock = 0;
            int contentLength = 0;
            boolean lamportClockReceived = false;

            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Lamport-Clock:")) {
                    String clockValueStr = line.substring("Lamport-Clock:".length()).trim();
                    serverLamportClock = Integer.parseInt(clockValueStr);
                    lamportClockReceived = true;
                } else if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                }
            }

            // Update Lamport clock if value received
            if (lamportClockReceived) {
                lamportClock.update(serverLamportClock);
                System.out.println("Lamport clock updated to: " + lamportClock.getClock());
            } else {
                System.out.println("No Lamport clock value received from server.");
            }

            // Read body
            char[] bodyChars = new char[contentLength];
            int totalRead = 0;
            while (totalRead < contentLength) {
                int read = in.read(bodyChars, totalRead, contentLength - totalRead);
                if (read == -1) {
                    break;
                }
                totalRead += read;
            }
            String responseBody = new String(bodyChars);

            return responseBody.trim();
        }
    }

    public static void displayWeatherData(String jsonData) {
    	jsonData = jsonData.trim();
    	System.out.println("-----------------------------------");
    	
        // Check if the response is empty
        if (jsonData.isEmpty()) {
            System.out.println("No weather data available.");
            return;
        }

        // Handle JSON arrays (if the data is wrapped in an array)
        if (jsonData.startsWith("[") && jsonData.endsWith("]")) {
            jsonData = jsonData.substring(1, jsonData.length() - 1).trim();  // Remove the array brackets
        }

        // Check if the JSON data is still empty after trimming
        if (jsonData.isEmpty()) {
            System.out.println("No weather data available.");
            return;
        }

        // Split the JSON objects if there are multiple weather data entries
        String[] dataObjects = jsonData.split("\\},\\{");

        // Loop through each weather data object
        for (String dataObject : dataObjects) {
            // Clean up any remaining curly braces
            dataObject = dataObject.replaceAll("[\\{\\}]", "").trim();

            // Check if the data object is empty or malformed
            if (dataObject.isEmpty()) {
                continue;  // Skip empty entries
            }

            // Split the attributes of the weather data
            String[] attributes = dataObject.split(",");
            for (String attribute : attributes) {
                String[] keyValue = attribute.split(":", 2);

                // Ensure the key-value pair is valid (i.e., has both a key and a value)
                if (keyValue.length < 2) {
                    continue;  // Skip invalid key-value pairs
                }

                String key = keyValue[0].replaceAll("\"", "").trim();
                String value = keyValue[1].replaceAll("\"", "").trim();
                System.out.println(key + ": " + value);
            }
        }
    }
}