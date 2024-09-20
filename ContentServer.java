package com.weather.app;
import java.net.*;
import java.io.*;
import java.util.*;

public class ContentServer {
    private static String serverAddress;
    private static int serverPort;
    private static String filePath;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java WeatherPutClient <server-address:port> <file-path>");
            return;
        }

        String serverInfo = args[0];
        filePath = args[1];

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
            String jsonData = convertFileToJson(filePath);
            if (jsonData != null) {
                sendDataToServer(jsonData);
            } else {
                System.out.println("Invalid data, not sending to server.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String convertFileToJson(String filePath) throws IOException {
        Map<String, String> dataMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(":", 2);
                if (parts.length != 2) continue;

                String key = parts[0].trim();
                String value = parts[1].trim();
                dataMap.put(key, value);
            }
        }

        // Check if 'id' exists and convert to JSON if valid
        if (!dataMap.containsKey("id")) {
            System.out.println("Missing 'id' field, entry rejected.");
            return null;
        }

        // Build JSON string
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            jsonBuilder.append(String.format("    \"%s\": \"%s\",\n", entry.getKey(), entry.getValue()));
        }
        // Remove the last comma and close the JSON object
        jsonBuilder.setLength(jsonBuilder.length() - 2);
        jsonBuilder.append("\n}");

        return jsonBuilder.toString();
    }

    private static void sendDataToServer(String jsonData) throws IOException {
        try (Socket socket = new Socket(serverAddress, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Prepare HTTP PUT request
            StringBuilder request = new StringBuilder();
            request.append("PUT /weather.json HTTP/1.1\r\n");
            request.append("Host: ").append(serverAddress).append("\r\n");
            request.append("User-Agent: ATOMClient/1.0\r\n");
            request.append("Content-Type: application/json\r\n");
            request.append("Content-Length: ").append(jsonData.length()).append("\r\n");
            request.append("\r\n"); // End of headers
            request.append(jsonData);

            // Debug output to verify request
            System.out.println("Sending PUT request:");
            System.out.println(request.toString());

            // Send entire request (headers and body)
            out.print(request.toString());
            out.flush();

            // Get server response
            String responseLine;
            while ((responseLine = in.readLine()) != null) {
                System.out.println("Server Response: " + responseLine);
                if (responseLine.isEmpty()) {
                    break; // End of HTTP response headers
                }
            }
        }
    }
}
