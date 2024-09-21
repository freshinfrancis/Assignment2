package com.weather.app;

import java.net.*;
import java.io.*;
import java.util.*;
import com.google.gson.*;

public class GETClient {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java GETClient <server:port> [stationId]");
            return;
        }

        try {
            // Parse server and port from command line argument
            String[] serverDetails = args[0].replace("http://", "").split(":");
            String serverAddress = serverDetails[0];
            int serverPort = Integer.parseInt(serverDetails[1]);

            // Check if station ID is provided
            String stationId = (args.length > 1) ? args[1] : "";

            // Create socket connection
            try (Socket socket = new Socket(serverAddress, serverPort);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Send GET request with optional station ID
                String request = "GET /weather" + (stationId.isEmpty() ? "" : "?id=" + stationId);
                out.println(request);

                // Read and accumulate response
                StringBuilder jsonResponse = new StringBuilder();
                String responseLine;
                while ((responseLine = in.readLine()) != null) {
                    jsonResponse.append(responseLine);
                }


                // Check if the response is valid JSON or a plain string
                String responseStr = jsonResponse.toString().trim();
                if (responseStr.startsWith("{")) {
                    // Parse JSON response
                    Gson gson = new Gson();
                    Map<String, Object> weatherData = gson.fromJson(responseStr, Map.class);

                    // Strip JSON and display attribute-value pairs
                    System.out.println("Weather Data:");
                    for (Map.Entry<String, Object> entry : weatherData.entrySet()) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                } else {
                    // Handle non-JSON responses (e.g., error messages)
                    System.out.println("Received non-JSON response: " + responseStr);
                }

            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
