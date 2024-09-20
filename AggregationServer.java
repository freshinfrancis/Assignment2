package com.weather.app;

import java.net.*;
import java.io.*;
import java.nio.file.*;

public class AggregationServer {
    private static final int PORT = 8080;
    private static final String DATA_FILE = "weatherData.json";
    private static final String TEMP_FILE = "weatherData.tmp";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is running on port " + PORT);

        // Ensure recovery from crashes by checking if temp file exists
        recoverIfCrashed();

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
                    out.println("Invalid request type");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Handle PUT requests with intermediate storage and validation
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

        char[] bodyData = new char[contentLength];
        in.read(bodyData, 0, contentLength);
        String jsonData = new String(bodyData);

        // Validate the data before saving
        if (validateData(jsonData)) {
            // Save to a temp file first (intermediate storage)
            saveDataToTempFile(jsonData);

            // If successful, move the temp file to the actual data file
            Files.move(Paths.get(TEMP_FILE), Paths.get(DATA_FILE), StandardCopyOption.REPLACE_EXISTING);

            // Send response to the ContentServer
            out.println("HTTP/1.1 200 OK");
            out.println("Data updated successfully.");
        } else {
            out.println("HTTP/1.1 400 Bad Request");
            out.println("Invalid data format.");
        }
    }

    // Handle GET requests by returning data as JSON
    private static void handleGetRequest(PrintWriter out) throws IOException {
        String data = readDataFromFile();

        // Convert the text data to JSON format
        String jsonData = convertDataToJson(data);

        // Send the JSON data to the client
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: application/json");
        out.println("Content-Length: " + jsonData.length());
        out.println();
        out.println(jsonData);
    }

    // Save data to a temporary file before committing to the main file
    private static void saveDataToTempFile(String data) throws IOException {
        try (FileWriter fileWriter = new FileWriter(TEMP_FILE)) {
            fileWriter.write(data);
        }
    }

    // Read data from the permanent file
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

    // Move temp file to main file if it exists
    private static void recoverIfCrashed() throws IOException {
        if (Files.exists(Paths.get(TEMP_FILE))) {
            // Move temp file to main data file
            Files.move(Paths.get(TEMP_FILE), Paths.get(DATA_FILE), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Recovered from crash, restored data.");
        }
    }

    // Validate the incoming data
    private static boolean validateData(String data) {
        // Check if it contains required fields (e.g., "id")
        return data.contains("\"id\"");
    }

    // Convert the plain data to JSON
    private static String convertDataToJson(String data) {
        // Assume the data is already JSON-like and return as is
        return data;
    }
}
