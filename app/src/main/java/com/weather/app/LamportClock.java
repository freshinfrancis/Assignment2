// File: LamportClock.java
package com.weather.app;

public class LamportClock {
    public int clock;

    public LamportClock() {
        this.clock = 0;
    }

    // Increment the clock for internal events
    public synchronized void tick() {
        clock++;
    }

    // Update the clock on message reception
    public synchronized void update(int receivedClock) {
        clock = Math.max(clock, receivedClock);
        clock++;
    }

    public synchronized int getClock() {
        return clock;
    }

    @Override
    public synchronized String toString() {
        return Integer.toString(clock);
    }
}
