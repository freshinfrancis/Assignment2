// File: ContentServer.java
// This file contains the ContentServer class, which acts as a server to send
// weather data from a local file to an AggregationServer.

package com.weather.app;

import java.net.*;
import java.io.*;
import java.util.*;

public class ContentServer {
    // Define variables for the server address, port, and file path to send data from.
    private static String serverAddress;
    private static int serverPort;
    private static String filePath;
    private static final LamportClock lamportClock = new LamportClock(); // Create an instance of LamportClock for concurrency control.

    public static void main(String[] args) {
        // Check if the correct number of command-line arguments are provided.
        if (args.length != 2) {
            System.out.println("Usage: java ContentServer <server-address:port> <file-path>");
            return;
        }

        // Parse the server address and port from the first argument.
        String serverInfo = args[0];
        filePath = args[1];

        // Split the server info into address and port parts.
        String[] serverParts = serverInfo.split(":");
        if (serverParts.length != 2) {
            System.out.println("Invalid server info format. Expected <server-address:port>");
            return;
        }

        serverAddress = serverParts[0]; // Assign the server address.
        try {
            serverPort = Integer.parseInt(serverParts[1]); // Parse and assign the server port.
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number: " + serverParts[1]);
            return;
        }

        try {
            // Convert the local file data to a JSON string format.
            String jsonData = convertFileToJson(filePath);
            if (jsonData != null) {
                // If the JSON data is valid, send it to the server.
                sendDataToServer(jsonData);
            } else {
                System.out.println("Invalid data, not sending to server.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Reads the content from the provided file path and converts it to a JSON string.
    public static String convertFileToJson(String filePath) throws IOException {
        Map<String, String> dataMap = new HashMap<>(); // Create a map to hold the file data.

        // Read the file line by line and parse key-value pairs.
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // Skip empty lines.

                String[] parts = line.split(":", 2); // Split each line into key and value.
                if (parts.length != 2) continue; // Skip lines that do not have exactly two parts.

                String key = parts[0].trim(); // Extract the key and remove surrounding whitespace.
                String value = parts[1].trim(); // Extract the value and remove surrounding whitespace.
                dataMap.put(key, value); // Store the key-value pair in the map.
            }
        }

        // Ensure that the data contains an 'id' field as a required attribute.
        if (!dataMap.containsKey("id")) {
            System.out.println("Missing 'id' field, entry rejected.");
            return null;
        }

        // Build a JSON string using the key-value pairs in the map.
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            jsonBuilder.append(String.format("    \"%s\": \"%s\",\n", entry.getKey(), entry.getValue())); // Format as a JSON attribute.
        }
        // Remove the last comma and close the JSON object.
        jsonBuilder.setLength(jsonBuilder.length() - 2);
        jsonBuilder.append("\n}");

        return jsonBuilder.toString(); // Return the constructed JSON string.
    }

    // Sends the constructed JSON data to the AggregationServer.
    public static void sendDataToServer(String jsonData) throws IOException {
        lamportClock.tick(); // Increment Lamport clock before sending to ensure concurrency control.

        // Create a socket connection to the server using the address and port.
        try (Socket socket = new Socket(serverAddress, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // To send data to the server.
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) { // To receive responses.

            // Prepare an HTTP PUT request with headers, including the Lamport-Clock header.
            StringBuilder request = new StringBuilder();
            request.append("PUT /weather.json HTTP/1.1\r\n");
            request.append("Host: ").append(serverAddress).append("\r\n");
            request.append("User-Agent: ContentServer/1.0\r\n");
            request.append("Content-Type: application/json\r\n");
            request.append("Content-Length: ").append(jsonData.length()).append("\r\n");
            request.append("Lamport-Clock: ").append(lamportClock.getClock()).append("\r\n"); // Include the current Lamport clock value.
            request.append("\r\n"); // End of headers.
            request.append(jsonData); // Append the JSON data to the request.

            // Send the entire HTTP request (headers and body).
            out.print(request.toString());
            out.flush(); // Ensure the data is sent to the server.

            // Read the response headers from the server.
            String responseLine;
            int lamportClockValue = lamportClock.getClock(); // Local copy of Lamport clock.
            boolean lamportClockReceived = false; // Flag to track if a Lamport clock value is received.

            while ((responseLine = in.readLine()) != null && !responseLine.isEmpty()) {
                System.out.println("Server Response Header: " + responseLine); // Print server response headers.

                // Check if the response contains a Lamport-Clock header.
                if (responseLine.startsWith("Lamport-Clock:")) {
                    String clockValueStr = responseLine.substring("Lamport-Clock:".length()).trim();
                    lamportClockValue = Integer.parseInt(clockValueStr); // Parse the Lamport clock value.
                    lamportClockReceived = true; // Set the flag to true.
                }
            }

            // Update the local Lamport clock value if a valid response was received.
            if (lamportClockReceived) {
                lamportClock.update(lamportClockValue);
                System.out.println("Lamport clock updated to: " + lamportClock.getClock());
            } else {
                System.out.println("No Lamport clock value received from server.");
            }

            // Optionally, read and print the response body (if present).
            while ((responseLine = in.readLine()) != null) {
                System.out.println("Server Response Body: " + responseLine); // Print server response body.
            }
        }
    }
}