package com.interview.mastery.core;

import java.time.Instant;
import java.util.Objects;

/**
 * Chapter 01 / CJ-05: a correct immutable value type.
 *
 * <p>Final class, private final fields of immutable types ({@link Instant}), validation in the
 * constructor, no setters, no leaking of mutable internals. Safe to share across threads and to use
 * as a map/set key.
 */
public final class Period {

    private final Instant start;
    private final Instant end;

    public Period(Instant start, Instant end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start must not be after end");
        }
        this.start = start;   // Instant is immutable -> no defensive copy needed
        this.end = end;
    }

    public Instant start() { return start; }

    public Instant end() { return end; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Period p && p.start.equals(start) && p.end.equals(end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "Period[" + start + " .. " + end + "]";
    }
}
