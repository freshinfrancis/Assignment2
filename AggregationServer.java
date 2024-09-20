package com.weather.app;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.*;
import java.net.*;

public class AggregationServer {
    private static final int DEFAULT_PORT = 4567;
    private static final String DATA_FILE = "weatherData.json";
    private static final String TEMP_FILE = "weatherData.tmp";

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT; // Set the default port

        // Check if a port number is passed as a command-line argument
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]); // Parse the port number from the argument
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port: " + DEFAULT_PORT);
            }
        }

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server is running on port " + port);

        while (true) {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String requestType = in.readLine(); // First line is request type
                System.out.println(requestType);

                String[] requestParts = requestType.split(" ", 2);  // Split into method and path
                String method = "";
                String path = "";

                if (requestParts.length >= 2) {
                    method = requestParts[0];  // First part is the HTTP method
                    path = requestParts[1];    // Second part is the requested path
                }

                if ("PUT".equals(method)) {
                    handlePutRequest(in, out);
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

    private static void handlePutRequest(BufferedReader in, PrintWriter out) throws IOException {
        StringBuilder headers = new StringBuilder();
        String line;
        int contentLength = 0;

        // Read headers
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            headers.append(line).append("\n");

            // Check for Content-Length header
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
            }
        }

        // No content
        if (contentLength == 0) {
            out.println("HTTP/1.1 204 No Content");
            return;
        }

        // Read the JSON data from the body based on Content-Length
        char[] bodyData = new char[contentLength];
        in.read(bodyData, 0, contentLength);  // Read exactly contentLength characters
        String jsonData = new String(bodyData);

        if (!isValidJson(jsonData)) {
            System.out.println("Invalid JSON received: " + jsonData);  // Debug output
            out.println("HTTP/1.1 500 Internal Server Error");
            return;
        }

        // Handle file write safely with a temporary file
        boolean isNewFile = !new File(DATA_FILE).exists();
        try {
            writeToTempFile(jsonData);
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
            System.out.println("File write error: " + e.getMessage());  // Debug output
            out.println("HTTP/1.1 500 Internal Server Error");
        }
    }

    private static boolean isValidJson(String jsonData) {
        try {
            JsonElement jsonElement = JsonParser.parseString(jsonData);  // Try parsing the JSON
            return jsonElement.isJsonObject();  // Check if it's a valid JSON object
        } catch (JsonSyntaxException e) {
            return false;  // If it's invalid, return false
        }
    }

    private static void handleGetRequest(PrintWriter out) throws IOException {
        String data = readDataFromFile();
        out.println("HTTP/1.1 200 OK");
        out.println();
        out.println(data);
    }

    private static void writeToTempFile(String data) throws IOException {
        try (FileWriter fileWriter = new FileWriter(TEMP_FILE)) {
            fileWriter.write(data);
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
            tempFile.delete();  // Ensure temp file is deleted
        }
    }

    private static String readDataFromFile() throws IOException {
        try (BufferedReader fileReader = new BufferedReader(new FileReader(DATA_FILE))) {
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = fileReader.readLine()) != null) {
                data.append(line).append("\n");
            }
            return data.toString();
        }
    }
}
