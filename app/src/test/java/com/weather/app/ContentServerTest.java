package com.weather.app;

//File: ContentServerTest.java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.*;

public class ContentServerTest {

 @Test
 public void testContentServerSendsData() throws Exception {
     // Prepare test data file
     String testData = "id:TEST_ID\nname:Test Station\nstate:TS";
     Path tempFile = Files.createTempFile("test_weather_data", ".txt");
     Files.write(tempFile, testData.getBytes());

     // Start the AggregationServer in a separate thread
     Thread serverThread = new Thread(() -> {
         try {
             AggregationServer.main(new String[]{});
         } catch (Exception e) {
             e.printStackTrace();
         }
     });
     serverThread.start();
     Thread.sleep(1000);

     // Start the ContentServer
     Thread contentServerThread = new Thread(() -> {
         try {
             ContentServer.main(new String[]{"localhost:8080", tempFile.toAbsolutePath().toString()});
         } catch (Exception e) {
             e.printStackTrace();
         }
     });
     contentServerThread.start();
     contentServerThread.join();

     // Verify data on the server as before
     Socket socket = new Socket("localhost", 8080);
     BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

     out.write("GET /weather HTTP/1.1\r\n");
     out.write("Host: localhost\r\n");
     out.write("\r\n");
     out.flush();

     // Read response status line
     String statusLine = in.readLine();
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
     serverThread.interrupt();
 }
}
