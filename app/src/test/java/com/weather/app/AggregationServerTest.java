// File: AggregationServerTest.java
package com.weather.app;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.*;
import java.net.Socket;
import java.time.Instant;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class AggregationServerTest {

    private Socket mockSocket;
    private BufferedReader mockReader;
    private PrintWriter mockWriter;

    @Before
    public void setup() throws IOException {
        mockSocket = mock(Socket.class);
        mockReader = mock(BufferedReader.class);
        mockWriter = mock(PrintWriter.class);

        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
        when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
    }

    @Test
    public void testHandlePutRequestWithValidData() throws IOException {
        String validJson = "{\"id\":\"123\", \"temperature\":\"25\", \"state\":\"SA\"}";

        when(mockReader.readLine()).thenReturn("PUT / HTTP/1.1")
                .thenReturn("Content-Length: " + validJson.length())
                .thenReturn("Lamport-Clock: 1")
                .thenReturn(validJson)
                .thenReturn(""); // End of headers

        AggregationServer.handleClient(mockSocket);

        verify(mockWriter, atLeastOnce()).println("HTTP/1.1 201 Created");
    }

    @Test
    public void testHandleGetRequest() throws IOException {
        JsonObject jsonData = JsonParser.parseString("{\"id\":\"123\", \"temperature\":\"25\", \"state\":\"SA\"}").getAsJsonObject();
        AggregationServer.weatherData.put("123", jsonData);

        when(mockReader.readLine()).thenReturn("GET /weather.json HTTP/1.1").thenReturn("").thenReturn(null);

        AggregationServer.handleClient(mockSocket);

        verify(mockWriter).println("HTTP/1.1 200 OK");
    }

    @Test
    public void testCleanExpiredData() {
        // Simulate expired data in weatherData map
        JsonObject expiredData = new JsonObject();
        expiredData.addProperty("id", "123");
        expiredData.addProperty("timestamp", Instant.now().toEpochMilli() - 40000); // 40 seconds ago
        expiredData.addProperty("origin", "content-server");

        AggregationServer.weatherData.put("123", expiredData);
        AggregationServer.cleanExpiredData();

        assertTrue(AggregationServer.weatherData.isEmpty());
    }
}
