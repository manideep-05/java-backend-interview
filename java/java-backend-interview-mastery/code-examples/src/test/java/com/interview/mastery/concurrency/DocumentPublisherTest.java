package com.interview.mastery.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentPublisherTest {

    @Test
    void publishesExactlyOncePerIdUnderConcurrency() throws InterruptedException {
        DocumentPublisher publisher = new DocumentPublisher();
        int threads = 16;
        long docId = 42L;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> publisher.publishOnce(docId)); // all race on the same id
            }
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(1, publisher.sendCount(), "the side effect must run exactly once per id");
    }
}
