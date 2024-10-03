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

    private static void displayWeatherData(String jsonData) {
    	Gson gson = new Gson();
        
        // Parse JSON data
    	jsonData = jsonData.replace("{", "").replace("}", "").replace("\"", "");

        System.out.println("Received Weather Data:");
        
        System.out.println(jsonData);
    }
}