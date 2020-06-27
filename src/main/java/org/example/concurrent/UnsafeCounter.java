package org.example.concurrent;

public class UnsafeCounter implements Counter {
    private long value = 0;

    @Override
    public long get() {
        return value;
    }

    @Override
    public void set(long newValue) {
        value = newValue;
    }

    @Override
    public void increment() {
        value++;
    }
}
