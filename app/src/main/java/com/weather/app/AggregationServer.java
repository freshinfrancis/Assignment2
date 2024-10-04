// File: AggregationServer.java
// This file contains the AggregationServer class, which acts as a server to
// aggregate weather data from various content servers and handle client requests.

package com.weather.app;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class AggregationServer {
    // Constant variables defining server configurations.
    private static final int DEFAULT_PORT = 4567; // Default port number for the server.
    public static final String DATA_FILE = "weatherData.json"; // Filename to store weather data.
    public static final String TEMP_FILE = "weatherData.tmp"; // Temporary file used for writing data.
    private static final long EXPIRATION_TIME_MILLIS = 30_000; // Expiration time for data in milliseconds (30 seconds).
    public static final LamportClock lamportClock = new LamportClock(); // Instance of LamportClock for concurrency control.

    // Data structures to store weather data and timestamps from content servers.
    public static final Map<String, JsonObject> weatherData = new ConcurrentHashMap<>(); // Map to store weather data by ID.
    public static final Map<String, Long> serverTimestamps = new ConcurrentHashMap<>(); // Map to store last update timestamps of content servers.

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT; // Initialize port with the default value.

        // Check if a port number is passed as a command-line argument.
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]); // Attempt to parse the provided port number.
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port: " + DEFAULT_PORT);
            }
        }

        // Schedule a task to clean up expired entries periodically every 10 seconds.
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(AggregationServer::cleanExpiredData, 10, 10, TimeUnit.SECONDS);

        // Start the server socket to listen for client connections on the specified port.
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);

            while (true) {
                // Accept client connections and handle each client in a separate thread.
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        }
    }

    // Method to handle communication with an individual client.
    public static void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Read the request type (e.g., "GET /" or "PUT /data").
            String requestType = in.readLine();
            System.out.println("Received request: " + requestType);

            // Parse the request to extract the method and path.
            String[] requestParts = requestType.split(" ", 2);
            String method = requestParts.length >= 1 ? requestParts[0] : "";
            String path = requestParts.length >= 2 ? requestParts[1] : "";

            // Read headers and extract the Lamport-Clock value if provided.
            Map<String, String> headers = new HashMap<>();
            String line;
            int clientLamportClock = 0;

            while ((line = in.readLine()) != null && !line.isEmpty()) {
                int separatorIndex = line.indexOf(":");
                if (separatorIndex != -1) {
                    String headerName = line.substring(0, separatorIndex).trim();
                    String headerValue = line.substring(separatorIndex + 1).trim();
                    headers.put(headerName, headerValue);

                    if (headerName.equalsIgnoreCase("Lamport-Clock")) {
                        clientLamportClock = Integer.parseInt(headerValue);
                    }
                }
            }

            // Update the server's Lamport clock based on the received value.
            lamportClock.update(clientLamportClock);

            // Handle different types of HTTP methods: PUT or GET.
            if ("PUT".equalsIgnoreCase(method)) {
                handlePutRequest(in, out, clientSocket.getInetAddress().toString(), headers);
            } else if ("GET".equalsIgnoreCase(method)) {
                handleGetRequest(out, headers);
            } else {
                // Return a 400 Bad Request response if the method is not supported.
                out.println("HTTP/1.1 400 Bad Request");
                out.println("Lamport-Clock: " + lamportClock.getClock());
                out.println();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handles PUT requests to update or create new weather data entries.
    public static void handlePutRequest(BufferedReader in, PrintWriter out, String contentServer, Map<String, String> headers) throws IOException {
        lamportClock.tick(); // Increment the Lamport clock for the PUT request.

        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));

        // If no content is provided, respond with a 204 No Content status.
        if (contentLength == 0) {
            out.println("HTTP/1.1 204 No Content");
            out.println("Lamport-Clock: " + lamportClock.getClock());
            out.println();
            return;
        }

        // Read the content of the request body.
        char[] bodyData = new char[contentLength];
        in.read(bodyData, 0, contentLength);
        String jsonData = new String(bodyData);

        // Validate the JSON data.
        if (!isValidJson(jsonData)) {
            System.out.println("Invalid JSON received: " + jsonData);
            out.println("HTTP/1.1 500 Internal Server Error");
            out.println("Lamport-Clock: " + lamportClock.getClock());
            out.println();
            return;
        }

        // Parse and add metadata to the JSON object.
        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
        jsonObject.addProperty("origin", contentServer);
        jsonObject.addProperty("timestamp", Instant.now().toEpochMilli());

        // Extract the ID from the JSON object and store it in the weatherData map.
        String entryId = jsonObject.get("id").getAsString();
        weatherData.put(entryId, jsonObject);
        serverTimestamps.put(contentServer, Instant.now().toEpochMilli());

        // Attempt to write the data to a temporary file and commit it to the final file.
        boolean isNewFile = !new File(DATA_FILE).exists();
        try {
            writeToTempFile(weatherData);
            if (commitTempFile()) {
                // Respond with a 201 Created or 200 OK status depending on whether it's a new file.
                if (isNewFile) {
                    out.println("HTTP/1.1 201 Created");
                } else {
                    out.println("HTTP/1.1 200 OK");
                }
                out.println("Lamport-Clock: " + lamportClock.getClock());
                out.println();
            } else {
                out.println("HTTP/1.1 500 Internal Server Error");
                out.println("Lamport-Clock: " + lamportClock.getClock());
                out.println();
            }
        } catch (IOException e) {
            System.out.println("File write error: " + e.getMessage());
            out.println("HTTP/1.1 500 Internal Server Error");
            out.println("Lamport-Clock: " + lamportClock.getClock());
            out.println();
        }
    }

    // Validates whether the given string is a valid JSON object.
    public static boolean isValidJson(String jsonData) {
        try {
            JsonElement jsonElement = JsonParser.parseString(jsonData);
            return jsonElement.isJsonObject();
        } catch (JsonSyntaxException e) {
            return false; // Return false if JSON is invalid.
        }
    }

    // Handles GET requests to retrieve and send weather data.
    public static void handleGetRequest(PrintWriter out, Map<String, String> headers) throws IOException {
        lamportClock.tick(); // Increment the Lamport clock for the GET request.

        // Convert the weather data to JSON format.
        String jsonResponse = weatherData.isEmpty() ? "[]" : convertToJson(weatherData);

        // Send the HTTP response with the JSON data.
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: application/json");
        out.println("Content-Length: " + jsonResponse.length());
        out.println("Lamport-Clock: " + lamportClock.getClock());
        out.println();

        out.print(jsonResponse); // Send the JSON response body.
        out.flush();
    }

    // Writes the weather data to a temporary file.
    public static void writeToTempFile(Map<String, JsonObject> data) throws IOException {
        try (FileWriter fileWriter = new FileWriter(TEMP_FILE)) {
            fileWriter.write(new Gson().toJson(data.values()));
        }
    }

    // Commits the temporary file to the final data file.
    public static boolean commitTempFile() throws IOException {
        File tempFile = new File(TEMP_FILE);
        File finalFile = new File(DATA_FILE);

        try (FileReader fileReader = new FileReader(tempFile);
             FileWriter fileWriter = new FileWriter(finalFile)) {

            char[] buffer = new char[1024];
            int read;
            while ((read = fileReader.read(buffer)) != -1) {
                fileWriter.write(buffer, 0, read);
            }
            return true; // Return true if file writing is successful.
        } catch (IOException e) {
            System.out.println("Error while copying file: " + e.getMessage());
            return false;
        } finally {
            tempFile.delete(); // Delete the temporary file after committing.
        }
    }

    // Converts the weather data map to a pretty-printed JSON string.
    public static String convertToJson(Map<String, JsonObject> weatherData) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Collection<JsonObject> dataCollection = weatherData.values();
        return gson.toJson(dataCollection); // Return the JSON string.
    }

    // Removes expired data entries from the weatherData map.
    public static void cleanExpiredData() {
        long currentTime = Instant.now().toEpochMilli(); // Get the current timestamp.

        // Remove entries that have expired based on the defined expiration time.
        weatherData.entrySet().removeIf(entry -> {
            JsonObject jsonObject = entry.getValue();
            JsonElement timestampElement = jsonObject.get("timestamp");

            // If timestamp exists and is not null, check for expiration.
            if (timestampElement != null && !timestampElement.isJsonNull()) {
                long timestamp = timestampElement.getAsLong();
                return currentTime - timestamp > EXPIRATION_TIME_MILLIS; // Remove if expired.
            }

            // If timestamp is missing or invalid, remove the entry as well.
            return true;
        });
    }
}
