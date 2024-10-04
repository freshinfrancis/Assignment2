// File: GETClient.java
package com.weather.app;

import java.io.*;
import java.net.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GETClient {
    // Variables to store server address, port, station ID, and Lamport clock instance.
    private static String serverAddress;
    private static int serverPort;
    private static String stationId;
    private static final LamportClock lamportClock = new LamportClock(); // Create an instance of LamportClock for concurrency control.

    public static void main(String[] args) {

        // Check if the correct number of command-line arguments are provided.
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: java GETClient <server-address:port> [station-id]");
            return;
        }

        String serverInfo = args[0]; // Get the server address and port from command-line arguments.
        if (args.length == 2) {
            stationId = args[1]; // Optional: If a second argument is provided, use it as the station ID.
        }

        // Split the server address and port from the server info argument.
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
            // Send the HTTP GET request and get the server's JSON response.
            String jsonResponse = sendGetRequest();
            if (jsonResponse != null) {
                // If the response is not null, display the weather data.
                displayWeatherData(jsonResponse);
            } else {
                System.out.println("No response from server.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Sends an HTTP GET request to the server to fetch weather data and returns the JSON response.
    private static String sendGetRequest() throws IOException {
        lamportClock.tick(); // Increment Lamport clock before sending the request.

        // Create a socket connection to the server using the address and port.
        try (Socket socket = new Socket(serverAddress, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // To send the GET request.
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) { // To receive the server's response.

            // Prepare the HTTP GET request with headers.
            StringBuilder request = new StringBuilder();
            request.append("GET /weather.json HTTP/1.1\r\n");
            request.append("Host: ").append(serverAddress).append("\r\n");
            request.append("User-Agent: GETClient/1.0\r\n");
            request.append("Accept: application/json\r\n"); // Indicate that the client accepts JSON response.
            request.append("Lamport-Clock: ").append(lamportClock.getClock()).append("\r\n"); // Include the current Lamport clock value in the request header.
            request.append("\r\n"); // End of headers.

            // Send the GET request.
            out.print(request.toString());
            out.flush(); // Ensure the request is fully sent.

            // Read the response status line from the server.
            String statusLine = in.readLine();
            if (statusLine == null || !statusLine.contains("200")) { // Check if the status line indicates a successful response.
                System.out.println("Failed to get data from server.");
                return null;
            }

            // Read the response headers to extract Lamport clock and content length.
            String line;
            int serverLamportClock = 0;
            int contentLength = 0;
            boolean lamportClockReceived = false;

            // Loop through the headers until an empty line is reached (indicating the end of headers).
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Lamport-Clock:")) { // Check if the Lamport-Clock header is present.
                    String clockValueStr = line.substring("Lamport-Clock:".length()).trim();
                    serverLamportClock = Integer.parseInt(clockValueStr); // Parse the Lamport clock value.
                    lamportClockReceived = true; // Set flag to indicate that a Lamport clock value was received.
                } else if (line.startsWith("Content-Length:")) { // Check if the Content-Length header is present.
                    contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                }
            }

            // Update the local Lamport clock value if a valid clock value was received from the server.
            if (lamportClockReceived) {
                lamportClock.update(serverLamportClock);
                System.out.println("Lamport clock updated to: " + lamportClock.getClock());
            } else {
                System.out.println("No Lamport clock value received from server.");
            }

            // Read the response body, based on the content length.
            char[] bodyChars = new char[contentLength]; // Allocate an array to hold the body content.
            int totalRead = 0;
            while (totalRead < contentLength) {
                int read = in.read(bodyChars, totalRead, contentLength - totalRead); // Read the body content.
                if (read == -1) { // Check for end of stream.
                    break;
                }
                totalRead += read; // Update the total number of characters read.
            }
            String responseBody = new String(bodyChars); // Convert the character array to a String.

            return responseBody.trim(); // Return the response body, trimmed of leading/trailing whitespace.
        }
    }

    // Parses the JSON response and displays it in a user-friendly format.
    public static void displayWeatherData(String jsonData) {
        jsonData = jsonData.trim(); // Remove any leading or trailing whitespace.
        System.out.println("-----------------------------------");

        // Check if the response is empty.
        if (jsonData.isEmpty()) {
            System.out.println("No weather data available.");
            return;
        }

        // Handle JSON arrays (if the data is wrapped in an array).
        if (jsonData.startsWith("[") && jsonData.endsWith("]")) {
            jsonData = jsonData.substring(1, jsonData.length() - 1).trim(); // Remove the array brackets.
        }

        // Check if the JSON data is still empty after trimming the brackets.
        if (jsonData.isEmpty()) {
            System.out.println("No weather data available.");
            return;
        }

        // Split the JSON data into individual JSON objects if there are multiple entries.
        String[] dataObjects = jsonData.split("\\},\\{");

        // Loop through each weather data entry and display the attributes.
        for (String dataObject : dataObjects) {
            // Clean up any remaining curly braces.
            dataObject = dataObject.replaceAll("[\\{\\}]", "").trim();

            // Skip empty or malformed entries.
            if (dataObject.isEmpty()) {
                continue;
            }

            // Split the JSON attributes into key-value pairs.
            String[] attributes = dataObject.split(",");
            for (String attribute : attributes) {
                String[] keyValue = attribute.split(":", 2);

                // Ensure the key-value pair is valid (i.e., has both a key and a value).
                if (keyValue.length < 2) {
                    continue;
                }

                // Extract and clean up the key and value.
                String key = keyValue[0].replaceAll("\"", "").trim();
                String value = keyValue[1].replaceAll("\"", "").trim();
                System.out.println(key + ": " + value); // Display the key-value pair.
            }
        }
    }
}