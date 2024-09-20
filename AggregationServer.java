package com.weather.app;
import java.net.*;
import java.io.*;

public class AggregationServer {
    private static final int PORT = 8080;
    private static final String DATA_FILE = "weatherData.json";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is running on port " + PORT);

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
                } else if ("GET".equals(requestType)) {
                    handleGetRequest(out);
                } else {
                    out.println("Invalid request type");
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

        // Read the JSON data from the body based on Content-Length
        char[] bodyData = new char[contentLength];
        in.read(bodyData, 0, contentLength);  // Read exactly contentLength characters
        String jsonData = new String(bodyData);

        // Save the JSON data to a file (or perform other operations)
        saveDataToFile(jsonData);

        // Send response to the ContentServer
        out.println("Data updated successfully.");
    }

    private static void handleGetRequest(PrintWriter out) throws IOException {
        String data = readDataFromFile();
        out.println(data);
    }

    private static void saveDataToFile(String data) throws IOException {
        try (FileWriter fileWriter = new FileWriter(DATA_FILE)) {
            fileWriter.write(data);
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
