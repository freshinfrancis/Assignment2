package com.weather.app;

import java.net.*;
import java.io.*;
import com.google.gson.Gson;

public class ContentServer {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) throws IOException {
        // Simulate reading data from a file
        WeatherData weatherData = new WeatherData("CityName", "25°C", "60%");

        Gson gson = new Gson();
        String jsonData = gson.toJson(weatherData);

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("PUT");  // Send PUT request
            out.println(jsonData);  // Send JSON data

            String response = in.readLine();  // Get server response
            System.out.println(response);
        }
    }
}

class WeatherData {
    String location;
    String temperature;
    String humidity;

    public WeatherData(String location, String temperature, String humidity) {
        this.location = location;
        this.temperature = temperature;
        this.humidity = humidity;
    }
}
