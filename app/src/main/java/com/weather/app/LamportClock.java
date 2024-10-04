// File: LamportClock.java
// This class implements a simple Lamport Clock for keeping track of event order in distributed systems.
package com.weather.app;

public class LamportClock {
    public int clock; // Holds the current clock value.

    // Constructor to initialize the clock to zero.
    public LamportClock() {
        this.clock = 0; // Start the clock at 0.
    }

    public synchronized void tick() {
        clock++; // Increase the clock by 1.
    }

    public synchronized void update(int receivedClock) {
        clock = Math.max(clock, receivedClock); // Take the maximum clock value.
        clock++; // Increment by 1 to ensure progress.
    }

    public synchronized int getClock() {
        return clock; // Return the current clock value.
    }

    @Override
    public synchronized String toString() {
        return Integer.toString(clock); // Convert clock value to string.
    }
}
