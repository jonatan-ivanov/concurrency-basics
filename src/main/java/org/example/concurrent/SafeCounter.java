package org.example.concurrent;

import java.util.concurrent.atomic.AtomicLong;

public class SafeCounter implements Counter {
    private final AtomicLong value = new AtomicLong();

    @Override
    public long get() {
        return value.get();
    }

    @Override
    public void set(long newValue) {
        value.set(newValue);
    }

    @Override
    public void increment() {
        value.incrementAndGet();
    }
}
