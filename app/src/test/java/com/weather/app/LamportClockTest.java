package com.weather.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

//File: LamportClockTest.java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LamportClockTest {

 @Test
 public void testInitialTimeIsZero() {
     LamportClock clock = new LamportClock();
     assertEquals(0, clock.getTime(), "Initial Lamport clock time should be zero");
 }

 @Test
 public void testIncrement() {
     LamportClock clock = new LamportClock();
     clock.increment();
     assertEquals(1, clock.getTime(), "Lamport clock time should be incremented to one");
 }

 @Test
 public void testUpdateWithSmallerTime() {
     LamportClock clock = new LamportClock();
     clock.update(0);
     assertEquals(0, clock.getTime(), "Lamport clock time should remain zero after update with zero");
 }

 @Test
 public void testUpdateWithLargerTime() {
     LamportClock clock = new LamportClock();
     clock.update(5);
     assertEquals(5, clock.getTime(), "Lamport clock time should update to the larger received time");
 }

 @Test
 public void testIncrementAfterUpdate() {
     LamportClock clock = new LamportClock();
     clock.update(5);
     clock.increment();
     assertEquals(6, clock.getTime(), "Lamport clock time should increment after update");
 }

 @Test
 public void testConcurrentUpdates() {
     LamportClock clock = new LamportClock();

     // Simulate concurrent events
     clock.increment(); // Local event
     clock.update(3);   // Receive message with timestamp 3

     assertEquals(3, clock.getTime(), "Lamport clock should update to 3");
     clock.increment();
     assertEquals(4, clock.getTime(), "Lamport clock should increment to 4");
 }
}
