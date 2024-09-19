package com.weather.app;

import java.net.*;
import java.io.*;

public class ContentServer {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final String FILE_PATH = "C:\\Users\\fresh\\Downloads\\weatherData.json";

    public static void main(String[] args) throws IOException {
        String jsonData = readDataFromFile(FILE_PATH);

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("PUT"); // Send PUT request
            out.println(jsonData); // Send JSON data

            String response = in.readLine(); // Get server response
            System.out.println(response);
        }
    }

    private static String readDataFromFile(String filePath) throws IOException {
        StringBuilder jsonData = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonData.append(line).append("\n");
            }
        }
        return jsonData.toString();
    }
}
