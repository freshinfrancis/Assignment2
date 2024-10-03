package com.weather.app;

//File: GETClientTest.java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class GETClientTest {

 @Test
 public void testGETClientReceivesData() throws Exception {
     // Start the AggregationServer and ContentServer as before
     Thread serverThread = new Thread(() -> {
         try {
             AggregationServer.main(new String[]{});
         } catch (Exception e) {
             e.printStackTrace();
         }
     });
     serverThread.start();
     Thread.sleep(1000);

     Thread contentServerThread = new Thread(() -> {
         try {
             ContentServer.main(new String[]{"localhost:8080", "txt.txt"});
         } catch (Exception e) {
             e.printStackTrace();
         }
     });
     contentServerThread.start();
     contentServerThread.join();

     // Capture the output of GETClient
     ByteArrayOutputStream baos = new ByteArrayOutputStream();
     PrintStream originalOut = System.out;
     System.setOut(new PrintStream(baos));

     // Run GETClient
     GETClient.main(new String[]{"localhost:8080"});

     // Restore original output
     System.setOut(originalOut);

     String output = baos.toString();
     assertTrue(output.contains("id: TEST_ID"), "Output should contain the test data");

     serverThread.interrupt();
 }
}
