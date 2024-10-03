// File: LamportClockTest.java
package com.weather.app;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LamportClockTest {

    private LamportClock lamportClock;

    @Before
    public void setup() {
        lamportClock = new LamportClock();
    }

    @Test
    public void testInitialClockValue() {
        // Verify that the initial clock value is 0
        assertEquals(0, lamportClock.getClock());
    }

    @Test
    public void testTickIncrementsClock() {
        // Tick the clock once and verify it increments by 1
        lamportClock.tick();
        assertEquals(1, lamportClock.getClock());

        // Tick again to check multiple increments
        lamportClock.tick();
        assertEquals(2, lamportClock.getClock());
    }

    @Test
    public void testUpdateWithLesserClockValue() {
        // Tick the clock to increment it
        lamportClock.tick(); // clock = 1

        // Update with a lesser clock value, expect no change other than increment
        lamportClock.update(0);
        assertEquals(2, lamportClock.getClock());
    }

    @Test
    public void testUpdateWithGreaterClockValue() {
        // Update the clock with a greater value
        lamportClock.update(5);
        assertEquals(6, lamportClock.getClock());
    }

    @Test
    public void testUpdateWithEqualClockValue() {
        // Tick the clock and update with the same value
        lamportClock.tick(); // clock = 1
        lamportClock.update(1); // Both are the same

        // Ensure that the clock increments only by one more
        assertEquals(2, lamportClock.getClock());
    }

    @Test
    public void testToStringMethod() {
        // Verify the toString method reflects the correct clock value
        lamportClock.tick();
        assertEquals("1", lamportClock.toString());

        lamportClock.update(5);
        assertEquals("6", lamportClock.toString());
    }
}
