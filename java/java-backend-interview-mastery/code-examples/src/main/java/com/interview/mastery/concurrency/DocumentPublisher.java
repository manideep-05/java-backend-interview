package com.interview.mastery.concurrency;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chapter 06 §4 / COL-03: a backend "do exactly once" (e.g., publish/notify once) implemented two ways.
 *
 * <p>The broken check-then-act race (two threads both pass the {@code contains} check) is replaced by
 * a single atomic {@code add} on a thread-safe set. {@link #sendCount} records how many times the
 * side effect actually ran so tests can assert "exactly once per id".
 */
public final class DocumentPublisher {

    private final Set<Long> published = ConcurrentHashMap.newKeySet();
    private final AtomicInteger sendCount = new AtomicInteger();

    /**
     * Publishes a document at most once per id. {@link Set#add} returns {@code true} only for the
     * first caller for a given id — collapsing check-then-act into one atomic step.
     */
    public void publishOnce(long docId) {
        if (published.add(docId)) {   // atomic; true only once per id even under concurrency
            send(docId);
        }
    }

    private void send(long docId) {
        sendCount.incrementAndGet();  // stands in for the real side effect (email, event, etc.)
    }

    public int sendCount() {
        return sendCount.get();
    }
}
