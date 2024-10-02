
package com.weather.app;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class AggregationServer {
    private static final int SERVER_PORT = 4567;
    private static final int MAX_WEATHER_UPDATES = 20;
    private static ConcurrentHashMap<String, WeatherData> weatherDataMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Long> contentServerTimestamps = new ConcurrentHashMap<>();
    private static LamportClock lamportClock = new LamportClock();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Aggregation Server started on port " + SERVER_PORT);

            // Schedule task to remove expired data
            scheduler.scheduleAtFixedRate(() -> removeExpiredData(), 0, 5, TimeUnit.SECONDS);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Remove data from content servers that haven't communicated in the last 30 seconds
    private static void removeExpiredData() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = contentServerTimestamps.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (currentTime - entry.getValue() > 30000) {
                String serverId = entry.getKey();
                weatherDataMap.remove(serverId);
                iterator.remove();
                System.out.println("Removed data from content server: " + serverId);
            }
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream()));

                String requestLine = in.readLine();
                if (requestLine == null) {
                    socket.close();
                    return;
                }

                String[] requestParts = requestLine.split(" ");
                String method = requestParts[0];
                String path = requestParts[1];

                // Read headers
                Map<String, String> headers = new HashMap<>();
                String line;
                while (!(line = in.readLine()).isEmpty()) {
                    String[] headerParts = line.split(": ");
                    headers.put(headerParts[0], headerParts[1]);
                }

                if (method.equals("GET")) {
                    handleGet(out);
                } else if (method.equals("PUT")) {
                    handlePut(in, headers);
                } else {
                    sendResponse(out, 405, "Method Not Allowed", "");
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleGet(BufferedWriter out) throws IOException {
            synchronized (lamportClock) {
                lamportClock.increment();
            }

            // Aggregate weather data
            Collection<WeatherData> weatherDataCollection = weatherDataMap.values();
            List<WeatherData> weatherDataList = new ArrayList<>(weatherDataCollection);

            // Sort by Lamport timestamp
            weatherDataList.sort(Comparator.comparingLong(WeatherData::getLamportTimestamp));

            // Keep only the most recent updates
            if (weatherDataList.size() > MAX_WEATHER_UPDATES) {
                weatherDataList = weatherDataList.subList(
                        weatherDataList.size() - MAX_WEATHER_UPDATES, weatherDataList.size());
            }

            // Build JSON response
            StringBuilder jsonResponse = new StringBuilder();
            jsonResponse.append("[");
            for (int i = 0; i < weatherDataList.size(); i++) {
                jsonResponse.append(weatherDataList.get(i).toJson());
                if (i < weatherDataList.size() - 1) {
                    jsonResponse.append(",");
                }
            }
            jsonResponse.append("]");

            sendResponse(out, 200, "OK", jsonResponse.toString());
        }

        private void handlePut(BufferedReader in, Map<String, String> headers) throws IOException {
            int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
            char[] bodyChars = new char[contentLength];
            in.read(bodyChars);
            String requestBody = new String(bodyChars);

            // Parse Lamport timestamp
            long receivedLamportTimestamp = Long.parseLong(
                    headers.getOrDefault("Lamport-Timestamp", "0"));
            synchronized (lamportClock) {
                lamportClock.update(receivedLamportTimestamp);
                lamportClock.increment();
            }

            // Parse JSON data
            WeatherData weatherData = WeatherData.fromJson(requestBody);
            weatherData.setLamportTimestamp(lamportClock.getTime());

            // Update data maps
            String serverId = weatherData.getId();
            weatherDataMap.put(serverId, weatherData);
            contentServerTimestamps.put(serverId, System.currentTimeMillis());

            sendResponse(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), 200, "OK", "");

            System.out.println("Received PUT from Content Server ID: " + serverId);
        }

        private void sendResponse(BufferedWriter out, int statusCode, String statusText,
                String body) throws IOException {
            out.write("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
            out.write("Content-Length: " + body.length() + "\r\n");
            out.write("Content-Type: application/json\r\n");
            out.write("\r\n");
            out.write(body);
            out.flush();
        }
    }
}
