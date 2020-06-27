# Concurrency Basics

Example repo for my Concurrency Basics talk.

The repo demonstrates three concurrency issues. Two of the issues has tests, those should fail if you run `./gradlew clean build`, you should see something like this:  

```
> Task :test

CounterTest > [2] org.example.concurrent.UnsafeCounter@6472519b FAILED
    java.lang.AssertionError at CounterTest.java:79

CounterTest > [2] org.example.concurrent.UnsafeCounter@2782620a FAILED
    org.opentest4j.AssertionFailedError at CounterTest.java:63

8 tests completed, 2 failed

> Task :test FAILED
```

Because of the non-deterministic nature of the issues, there is no guarantee that both of the tests will fail but most of the times they should.
Try to fix the `UnsafeCounter` class and make the tests pass. :) 
