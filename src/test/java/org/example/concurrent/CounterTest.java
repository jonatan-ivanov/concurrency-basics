package org.example.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Warning: Since the behavior is based on "luck", the tests are not consistent, they can give false positives.
 */
@Slf4j
class CounterTest {
    private static final int TASK_COUNT = 4_000;
    private final ExecutorService executorService = Executors.newFixedThreadPool(TASK_COUNT);
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private static Stream<Counter> provideCounters() {
        return Stream.of(new SafeCounter(), new UnsafeCounter());
    }

    @ParameterizedTest
    @MethodSource("provideCounters")
    void shouldReturnTheSetValue(Counter counter) {
        assertThat(counter.get()).isEqualTo(0);

        counter.set(42);
        assertThat(counter.get()).isEqualTo(42);

        counter.set(0);
        assertThat(counter.get()).isEqualTo(0);
    }

    @ParameterizedTest
    @MethodSource("provideCounters")
    void shouldReturnTheIncrementedResult(Counter counter) {
        assertThat(counter.get()).isEqualTo(0);

        for (int i = 0; i < TASK_COUNT; i++) {
            counter.increment();
        }

        assertThat(counter.get()).isEqualTo(TASK_COUNT);
    }

    @ParameterizedTest
    @MethodSource("provideCounters")
    void shouldReturnTheIncrementedResultIfExecutedConcurrently(Counter counter) {
        assertThat(counter.get()).isEqualTo(0);

        List<Future<?>> futures = submitTasks(() -> increment(counter), TASK_COUNT);
        countDownLatch.countDown();
        await(futures);

        long result = counter.get();
        assertThat(result)
            .overridingErrorMessage("It seems there is an atomicity issue; expected: %d, actual: %d", TASK_COUNT, result)
            .isEqualTo(TASK_COUNT);
    }

    @ParameterizedTest
    @MethodSource("provideCounters")
    void aThreadShouldSeeTheResultOfTheChangeThatAnotherThreadMade(Counter counter) throws InterruptedException {
        assertThat(counter.get()).isEqualTo(0);

        CompletableFuture.runAsync(() -> detectIfCounterIsNonZero(counter));
        Thread.sleep(100);
        counter.increment();
        assertThat(counter.get()).isEqualTo(1);
        countDownLatch.await(300, MILLISECONDS);

        assertThat(countDownLatch.getCount())
            .overridingErrorMessage("It seems there is a visibility issue; the background thread did not see the change.")
            .isEqualTo(0);
    }

    private void detectIfCounterIsNonZero(Counter counter) {
        log.info("Detecting change...");
        while (counter.get() == 0) {
//            Warning: putting anything here (e.g.: the next line) will break the test, it will always report false positive
//            log.debug("No change so far...");
        }

        log.info("Change detected!");
        countDownLatch.countDown();
    }

    private List<Future<?>> submitTasks(Runnable task, int taskCount) {
        List<Future<?>> futures = new ArrayList<>(taskCount);
        for (int i = 0; i < taskCount; i++) {
            futures.add(executorService.submit(task));
        }

        return futures;
    }

    private void await(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            await(future);
        }
    }

    private void await(Future<?> future) {
        try {
            future.get();
        }
        catch (InterruptedException e) {
            log.error("Execution interrupted", e);
        }
        catch (ExecutionException e) {
            log.error("Execution error", e);
        }
        finally {
            future.cancel(false);
        }
    }

    private void increment(Counter counter) {
        try {
            countDownLatch.await();
            counter.increment();
        }
        catch (InterruptedException e) {
            fail("Execution interrupted", e);
        }
    }
}
