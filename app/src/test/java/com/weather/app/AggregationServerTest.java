package com.weather.app;

//File: AggregationServerTest.java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class AggregationServerTest {

 private static Thread serverThread;

 @BeforeAll
 public static void startServer() throws Exception {
     // Start the AggregationServer in a separate thread
     serverThread = new Thread(() -> {
         try {
             AggregationServer.main(new String[]{});
         } catch (Exception e) {
             e.printStackTrace();
         }
     });
     serverThread.start();

     // Give the server time to start
     Thread.sleep(1000);
 }

 @AfterAll
 public static void stopServer() throws Exception {
     // Implement server shutdown if possible
     // For this example, we'll skip server shutdown
 }

 @Test
 public void testServerReceivesPutRequest() throws Exception {
     // Simulate ContentServer sending a PUT request
     Socket socket = new Socket("localhost", 8080);
     BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

     String jsonData = "{\"id\":\"TEST_ID\",\"name\":\"Test Station\",\"state\":\"TS\"}";
     int contentLength = jsonData.length();

     out.write("PUT /weather HTTP/1.1\r\n");
     out.write("Host: localhost\r\n");
     out.write("Content-Type: application/json\r\n");
     out.write("Content-Length: " + contentLength + "\r\n");
     out.write("Lamport-Timestamp: 1\r\n");
     out.write("\r\n");
     out.write(jsonData);
     out.flush();

     // Read response
     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
     String statusLine = in.readLine();
     assertTrue(statusLine.contains("200"), "Response should be 200 OK");

     socket.close();

     // Now, simulate a GET request to verify data was stored
     socket = new Socket("localhost", 8080);
     out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
     in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

     out.write("GET /weather HTTP/1.1\r\n");
     out.write("Host: localhost\r\n");
     out.write("\r\n");
     out.flush();

     // Read response status line
     statusLine = in.readLine();
     assertTrue(statusLine.contains("200"), "Response should be 200 OK");

     // Read headers
     String line;
     int responseContentLength = 0;
     while (!(line = in.readLine()).isEmpty()) {
         if (line.startsWith("Content-Length:")) {
             responseContentLength = Integer.parseInt(line.split(": ")[1]);
         }
     }

     // Read body
     char[] bodyChars = new char[responseContentLength];
     in.read(bodyChars);
     String responseBody = new String(bodyChars);

     assertTrue(responseBody.contains("\"id\":\"TEST_ID\""), "Response should contain the test data");

     socket.close();
 }
}

