package org.example.concurrent;

public interface Counter {
    long get();
    void set(long newValue);
    void increment();
}
