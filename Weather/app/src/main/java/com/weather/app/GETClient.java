package com.weather.app;
import java.net.*;
import java.io.*;

public class GETClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                
            out.println("GET");  // Send GET request

            String response;
            while ((response = in.readLine()) != null) {
                System.out.println(response);  // Print weather data
            }
        }
    }
}
