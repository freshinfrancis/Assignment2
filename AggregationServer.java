package com.weather.app;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class AggregationServer {
    private static final int DEFAULT_PORT = 4567;
    private static final String DATA_FILE = "weatherData.json";
    private static final String TEMP_FILE = "weatherData.tmp";
    private static final long EXPIRATION_TIME_MILLIS = 30_000; // 30 seconds

    // Data structures to store weather data and timestamps of content servers
    private static final Map<String, JsonObject> weatherData = new ConcurrentHashMap<>();
    private static final Map<String, Long> serverTimestamps = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;

        // Check if a port number is passed as a command-line argument
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port: " + DEFAULT_PORT);
            }
        }

        // Periodically clean up expired entries
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(AggregationServer::cleanExpiredData, 10, 10, TimeUnit.SECONDS);

        // Start server
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running on port " + port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    String requestType = in.readLine();
                    System.out.println(requestType);

                    String[] requestParts = requestType.split(" ", 2);
                    String method = requestParts.length >= 2 ? requestParts[0] : "";
                    String path = requestParts.length >= 2 ? requestParts[1] : "";

                    if ("PUT".equals(method)) {
                        handlePutRequest(in, out, clientSocket.getInetAddress().toString());
                    } else if ("GET".equals(method)) {
                        handleGetRequest(out);
                    } else {
                        out.println("HTTP/1.1 400 Bad Request");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void handlePutRequest(BufferedReader in, PrintWriter out, String contentServer) throws IOException {
        StringBuilder headers = new StringBuilder();
        String line;
        int contentLength = 0;

        // Read headers
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            headers.append(line).append("\n");

            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
            }
        }

        // No content
        if (contentLength == 0) {
            out.println("HTTP/1.1 204 No Content");
            return;
        }

        char[] bodyData = new char[contentLength];
        in.read(bodyData, 0, contentLength);
        String jsonData = new String(bodyData);

        if (!isValidJson(jsonData)) {
            System.out.println("Invalid JSON received: " + jsonData);
            out.println("HTTP/1.1 500 Internal Server Error");
            return;
        }

        // Add metadata (timestamp and content server origin)
        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
        jsonObject.addProperty("origin", contentServer);
        jsonObject.addProperty("timestamp", Instant.now().toEpochMilli());

        String entryId = jsonObject.get("id").getAsString();
        weatherData.put(entryId, jsonObject);
        serverTimestamps.put(contentServer, Instant.now().toEpochMilli());

        boolean isNewFile = !new File(DATA_FILE).exists();
        try {
            writeToTempFile(weatherData);
            if (commitTempFile()) {
                if (isNewFile) {
                    out.println("HTTP/1.1 201 Created");
                } else {
                    out.println("HTTP/1.1 200 OK");
                }
            } else {
                out.println("HTTP/1.1 500 Internal Server Error");
            }
        } catch (IOException e) {
            System.out.println("File write error: " + e.getMessage());
            out.println("HTTP/1.1 500 Internal Server Error");
        }
    }

    private static boolean isValidJson(String jsonData) {
        try {
            JsonElement jsonElement = JsonParser.parseString(jsonData);
            return jsonElement.isJsonObject();
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    private static void handleGetRequest(PrintWriter out) throws IOException {
    	String jsonResponse = convertToJson(weatherData);

        // Send the correct headers and JSON response
    	out.println("HTTP/1.1 200 OK");
        out.println();
        out.println(jsonResponse);
    }

    private static void writeToTempFile(Map<String, JsonObject> data) throws IOException {
        try (FileWriter fileWriter = new FileWriter(TEMP_FILE)) {
            fileWriter.write(new Gson().toJson(data.values()));
        }
    }

    private static boolean commitTempFile() throws IOException {
        File tempFile = new File(TEMP_FILE);
        File finalFile = new File(DATA_FILE);

        try (FileReader fileReader = new FileReader(tempFile);
             FileWriter fileWriter = new FileWriter(finalFile)) {

            char[] buffer = new char[1024];
            int read;
            while ((read = fileReader.read(buffer)) != -1) {
                fileWriter.write(buffer, 0, read);
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error while copying file: " + e.getMessage());
            return false;
        } finally {
            tempFile.delete();
        }
    }
    
    private static String convertToJson(Map<String, JsonObject> weatherData) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{").append("\n"); // Start of JSON object

        boolean firstEntry = true;
        for (Map.Entry<String, JsonObject> entry : weatherData.entrySet()) {
            // No need for an additional key wrapping, just flatten the JsonObject
            if (!firstEntry) {
                jsonBuilder.append(","); // Separate entries with a comma
            }

            jsonBuilder.append(convertJsonObject(entry.getValue())); // Convert the JsonObject directly
            firstEntry = false;
        }

        jsonBuilder.append("\n").append("}"); // End of JSON object
        return jsonBuilder.toString();
    }

    private static String convertJsonObject(JsonObject jsonObject) {
        StringBuilder jsonBuilder = new StringBuilder();
        boolean firstField = true;

        // Iterate over the JsonObject fields and convert them
        for (Map.Entry<String, JsonElement> field : jsonObject.entrySet()) {
            if (!firstField) {
                jsonBuilder.append(",").append("\n"); // Separate fields with a comma
            }

            jsonBuilder.append("  \"").append(field.getKey()).append("\":");
            jsonBuilder.append(formatJsonValue(field.getValue()));

            firstField = false;
        }

        return jsonBuilder.toString();
    }

    private static String formatJsonValue(JsonElement value) {
        if (value.isJsonPrimitive()) {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isString()) {
                return "\"" + primitive.getAsString() + "\""; // Return string values with quotes
            } else if (primitive.isNumber()) {
                return primitive.getAsNumber().toString(); // Return numbers without quotes
            } else if (primitive.isBoolean()) {
                return Boolean.toString(primitive.getAsBoolean()); // Return booleans without quotes
            }
        } else if (value.isJsonObject()) {
            return convertJsonObject(value.getAsJsonObject()); // Recursively handle nested JSON objects
        }
        return "\"\""; // Return empty string for unknown types
    }



    private static void cleanExpiredData() {
        long currentTime = Instant.now().toEpochMilli();
        Iterator<Map.Entry<String, JsonObject>> iterator = weatherData.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonObject> entry = iterator.next();
            JsonObject jsonObject = entry.getValue();
            long timestamp = jsonObject.get("timestamp").getAsLong();
            String origin = jsonObject.get("origin").getAsString();

            if (currentTime - timestamp > EXPIRATION_TIME_MILLIS) {
                System.out.println("Removing expired entry from " + origin);
                iterator.remove();
                serverTimestamps.remove(origin);
            }
        }
    }
}
