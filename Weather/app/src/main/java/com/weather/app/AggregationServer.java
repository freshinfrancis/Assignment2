package com.weather.app;
import java.net.*;
import java.io.*;
import com.google.gson.Gson;

public class AggregationServer {
    private static final int PORT = 8080;
    private static final String DATA_FILE = "weatherData.json";

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("Server is running on port " + PORT);

			while (true) {
			    try (Socket clientSocket = serverSocket.accept()) {
			        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

			        String requestType = in.readLine(); // Assume first line is PUT or GET

			        if ("PUT".equals(requestType)) {
			            handlePutRequest(in, out);
			        } else if ("GET".equals(requestType)) {
			            handleGetRequest(out);
			        }
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
			}
		}
    }

    private static void handlePutRequest(BufferedReader in, PrintWriter out) throws IOException {
        String jsonData = in.readLine();
        saveDataToFile(jsonData);
        out.println("Updated Data");
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
        BufferedReader fileReader = new BufferedReader(new FileReader(DATA_FILE));
        StringBuilder data = new StringBuilder();
        String line;
        while ((line = fileReader.readLine()) != null) {
            data.append(line);
        }
        return data.toString();
    }
}
