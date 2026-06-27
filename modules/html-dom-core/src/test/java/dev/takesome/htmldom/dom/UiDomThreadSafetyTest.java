package dev.takesome.htmldom.dom;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiDomThreadSafetyTest {
    @Test
    void computedStyleSnapshotsCanBeReadWhileStylesAreMutated() throws Exception {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("div");
        document.setRoot(root);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> writer = executor.submit(() -> {
                await(start);
                for (int i = 0; i < 8_000; i++) {
                    root.setComputedStyle("width", i + "px");
                    root.setComputedStyle("height", (i + 1) + "px");
                    if ((i & 15) == 0) root.clearComputedStyle();
                }
            });
            Future<?> reader = executor.submit(() -> {
                await(start);
                for (int i = 0; i < 8_000; i++) {
                    Map<String, String> snapshot = root.computedStyle();
                    snapshot.entrySet().forEach(entry -> {
                        assertTrue(entry.getKey() != null);
                        assertTrue(entry.getValue() != null);
                    });
                }
            });
            start.countDown();
            writer.get(10, TimeUnit.SECONDS);
            reader.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void mutationQueueCanDrainWhileDocumentIsMutated() throws Exception {
        UiDomDocument document = new UiDomDocument();
        UiDomElement root = document.createElement("div");
        document.setRoot(root);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> writer = executor.submit(() -> {
                await(start);
                for (int i = 0; i < 8_000; i++) root.setAttribute("data-i", Integer.toString(i));
            });
            Future<?> drainer = executor.submit(() -> {
                await(start);
                for (int i = 0; i < 8_000; i++) document.drainMutations();
            });
            start.countDown();
            writer.get(10, TimeUnit.SECONDS);
            drainer.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }
}
