package com.weather.app;

import org.junit.jupiter.api.*; 
import com.google.gson.JsonObject; 

import java.io.*; 
import java.net.Socket;
import java.time.Instant; 

import static org.junit.jupiter.api.Assertions.*; 

class AggregationServerTest {

    private static Thread serverThread; 

    @BeforeAll
    static void startServer() {
        serverThread = new Thread(() -> {
            try {
                AggregationServer.main(new String[] { "4568" }); // Server listens on port 4568
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();

        // Give some time for the server to start
        try {
            Thread.sleep(2000); // Wait for 2 seconds to ensure server is up
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    static void stopServer() {
        // Interrupt the server thread to stop the server
        serverThread.interrupt();
    }

    @Test
    void testPutAndGetRequest() throws IOException {
        // Prepare JSON data for PUT request
        String jsonData = "{ \"id\": \"001\", \"name\": \"Test Station\", \"state\": \"Test State\" }";
        Socket socket = new Socket("localhost", 4568); // Connect to the server
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // Output stream to send data
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Input stream to receive data

        // Send the PUT request
        out.println("PUT /weather.json HTTP/1.1");
        out.println("Host: localhost");
        out.println("Content-Type: application/json");
        out.println("Content-Length: " + jsonData.length());
        out.println("Lamport-Clock: 1");
        out.println();
        out.println(jsonData);

        // Read the response from the server
        String response = in.readLine();
        assertTrue(response.contains("201") || response.contains("200")); // Check if response is 200 OK or 201 Created

        socket.close(); // Close the socket

        // Send GET request to retrieve the stored data
        socket = new Socket("localhost", 4568); // Reconnect to the server
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Send the GET request
        out.println("GET /weather.json HTTP/1.1");
        out.println("Host: localhost");
        out.println("Accept: application/json");
        out.println("Lamport-Clock: 2");
        out.println();

        // Read the response status
        String statusLine = in.readLine();
        assertTrue(statusLine.contains("200")); // Ensure we get OK status

        // Read JSON body from the response
        StringBuilder responseBody = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            responseBody.append(line); // Append response lines to the StringBuilder
        }

        // Check if the response contains the expected data
        assertTrue(responseBody.toString().contains("Test Station"));
        assertTrue(responseBody.toString().contains("Test State"));

        socket.close(); // Close the socket
    }

    @Test
    void testPutInvalidJson() throws IOException {
        // Prepare invalid JSON data for testing
        String invalidJsonData = "{ \"id\": }";  // Invalid JSON format

        Socket socket = new Socket("localhost", 4568); // Connect to the server
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Send PUT request with invalid JSON
        out.println("PUT /weather.json HTTP/1.1");
        out.println("Host: localhost");
        out.println("Content-Type: application/json");
        out.println("Content-Length: " + invalidJsonData.length());
        out.println("Lamport-Clock: 1");
        out.println();
        out.println(invalidJsonData);

        // Check response for 500 Internal Server Error due to invalid JSON
        String response = in.readLine();
        assertTrue(response.contains("500")); // Expect server error response

        socket.close(); // Close the socket
    }

    @Test
    void testPutRequestUpdatesClock() throws IOException {
        // Prepare JSON data for PUT request
        String jsonData = "{ \"id\": \"002\", \"name\": \"Update Test\", \"state\": \"Test State\" }";

        Socket socket = new Socket("localhost", 4568); // Connect to the server
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Send PUT request
        out.println("PUT /weather.json HTTP/1.1");
        out.println("Host: localhost");
        out.println("Content-Type: application/json");
        out.println("Content-Length: " + jsonData.length());
        out.println("Lamport-Clock: 5");
        out.println();
        out.println(jsonData);

        // Check if server response contains updated Lamport clock
        String response = in.readLine();
        String clockHeader = in.readLine(); // Read the Lamport-Clock header
        assertTrue(clockHeader.contains("Lamport-Clock")); // Ensure Lamport-Clock is in the header
        assertTrue(Integer.parseInt(clockHeader.split(":")[1].trim()) >= 5); // Verify the clock value is updated

        socket.close(); // Close the socket
    }

    @Test
    void testGetRequestWithMultipleDataEntries() throws IOException {
        // Prepare multiple JSON data entries for testing
        String jsonData1 = "{ \"id\": \"003\", \"name\": \"Station 1\", \"state\": \"State 1\" }";
        String jsonData2 = "{ \"id\": \"004\", \"name\": \"Station 2\", \"state\": \"State 2\" }";

        // Add two data entries using the helper method
        sendPutRequest(jsonData1, 1);
        sendPutRequest(jsonData2, 2);

        // Send GET request to verify both entries
        Socket socket = new Socket("localhost", 4568);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("GET /weather.json HTTP/1.1");
        out.println("Host: localhost");
        out.println("Accept: application/json");
        out.println("Lamport-Clock: 3");
        out.println();

        // Check response
        String statusLine = in.readLine();
        assertTrue(statusLine.contains("200")); // Expect OK status

        // Check body for both stations
        StringBuilder responseBody = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            responseBody.append(line); // Append response lines to the StringBuilder
        }
        // Validate that both stations are present in the response
        assertTrue(responseBody.toString().contains("Station 1"));
        assertTrue(responseBody.toString().contains("Station 2"));

        socket.close(); // Close the socket
    }

    private void sendPutRequest(String jsonData, int clockValue) throws IOException {
        // Helper method to send a PUT request
        Socket socket = new Socket("localhost", 4568);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Send PUT request
        out.println("PUT /weather.json HTTP/1.1");
        out.println("Host: localhost");
        out.println("Content-Type: application/json");
        out.println("Content-Length: " + jsonData.length());
        out.println("Lamport-Clock: " + clockValue);
        out.println();
        out.println(jsonData);

        in.readLine();  // Read response (not used in this context)

        socket.close(); // Close the socket
    }
    
    @Test
    void testWeatherDataStorage() {
        // Test the storage of weather data in the server's data structure
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "station1");
        jsonObject.addProperty("timestamp", Instant.now().toEpochMilli()); // Add current timestamp
        AggregationServer.weatherData.put("station1", jsonObject); // Store the data

        // Validate that the data was stored correctly
        assertTrue(AggregationServer.weatherData.containsKey("station1"));
    }

    @Test
    void testServerTimestampUpdate() {
        // Test the updating of server timestamps
        AggregationServer.serverTimestamps.put("contentServer", Instant.now().toEpochMilli()); // Update the timestamp for content server
        // Validate that the timestamp was updated
        assertTrue(AggregationServer.serverTimestamps.containsKey("contentServer"));
    }

    @Test
    void testCommitTempFile() throws IOException {
        // Test writing data to a temporary file and committing it
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "station1");
        AggregationServer.weatherData.put("station1", jsonObject); // Store data for testing

        AggregationServer.writeToTempFile(AggregationServer.weatherData); // Write to temp file
        assertTrue(AggregationServer.commitTempFile()); // Commit the temp file

        // Validate that the final file exists
        File finalFile = new File(AggregationServer.DATA_FILE);
        assertTrue(finalFile.exists());
    }

    @Test
    void testCleanExpiredData() throws InterruptedException {
        // Add an entry with a timestamp older than 30 seconds
        JsonObject jsonObjectOld = new JsonObject();
        jsonObjectOld.addProperty("id", "station1");
        jsonObjectOld.addProperty("timestamp", Instant.now().toEpochMilli() - 35_000); // Old timestamp
        AggregationServer.weatherData.put("station1", jsonObjectOld); // Store old data

        // Add an entry with a valid timestamp
        JsonObject jsonObjectValid = new JsonObject();
        jsonObjectValid.addProperty("id", "station2");
        jsonObjectValid.addProperty("timestamp", Instant.now().toEpochMilli()); // Current timestamp
        AggregationServer.weatherData.put("station2", jsonObjectValid); // Store valid data

        // Add an entry without a timestamp
        JsonObject jsonObjectNoTimestamp = new JsonObject();
        jsonObjectNoTimestamp.addProperty("id", "station3");
        AggregationServer.weatherData.put("station3", jsonObjectNoTimestamp); // Store data without timestamp

        AggregationServer.cleanExpiredData(); // Call method to clean expired data

        // Validate the expired entry was removed, but valid entry remains
        assertFalse(AggregationServer.weatherData.containsKey("station1")); // Expired entry should be removed
        assertTrue(AggregationServer.weatherData.containsKey("station2")); // Valid entry should remain
        assertFalse(AggregationServer.weatherData.containsKey("station3"));  // Invalid timestamp entry should be removed
    }
}
