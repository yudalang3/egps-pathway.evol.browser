package test.module.evolview.common;

import module.evolview.common.SwingDebouncer;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SwingDebouncerTest {

    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        SwingDebouncer debouncer = new SwingDebouncer(50, () -> {
            calls.incrementAndGet();
            latch.countDown();
        });

        SwingUtilities.invokeAndWait(() -> {
            debouncer.restart();
            debouncer.restart();
            debouncer.restart();
        });

        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Debounced action did not run");
        }
        Thread.sleep(150);
        if (calls.get() != 1) {
            throw new AssertionError("Expected one debounced call, got " + calls.get());
        }
    }
}
