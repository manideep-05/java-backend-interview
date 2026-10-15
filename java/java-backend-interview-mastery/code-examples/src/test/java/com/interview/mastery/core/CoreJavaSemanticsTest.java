package com.interview.mastery.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chapter 01: proves the Integer-cache == trap, String pool identity, and that a correct
 * value type ({@link Period}) works as a HashMap key.
 */
class CoreJavaSemanticsTest {

    @Test
    @SuppressWarnings({"deprecation", "removal", "UnnecessaryBoxing", "NumberEquality", "CachedNumberConstructorCall"})
    void integerCacheEqualityTrap() {
        Integer a = 127, b = 127;      // within cache (-128..127) -> same instance
        Integer c = 128, d = 128;      // outside cache -> distinct instances
        assertSame(a, b, "127 is cached, so == is true");
        assertNotSame(c, d, "128 is outside the cache, so == is false");
        assertEquals(c, d, "equals compares value, so it is true");
    }

    @Test
    void stringPoolIdentity() {
        String a = "hello";
        String b = "hello";
        String c = new String("hello");
        assertSame(a, b, "literals share the pooled instance");
        assertNotSame(a, c, "new String creates a distinct object");
        assertSame(a, c.intern(), "intern returns the pooled instance");
        assertEquals(a, c, "equals is always true for same content");
    }

    @Test
    void immutableValueTypeWorksAsMapKey() {
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t1 = Instant.parse("2026-01-02T00:00:00Z");

        Map<Period, String> map = new HashMap<>();
        map.put(new Period(t0, t1), "january-first-slot");

        // A different instance with equal fields must find the same entry (correct equals/hashCode).
        assertEquals("january-first-slot", map.get(new Period(t0, t1)));
    }

    @Test
    void periodValidatesOrdering() {
        Instant t0 = Instant.parse("2026-01-02T00:00:00Z");
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z"); // before t0
        assertThrows(IllegalArgumentException.class, () -> new Period(t0, t1));
    }

    @Test
    void equalObjectsHaveEqualHashCodes() {
        Instant t0 = Instant.parse("2026-03-01T00:00:00Z");
        Instant t1 = Instant.parse("2026-03-02T00:00:00Z");
        Period p1 = new Period(t0, t1);
        Period p2 = new Period(t0, t1);
        assertEquals(p1, p2);
        assertTrue(p1.hashCode() == p2.hashCode(), "equal objects must have equal hashCodes");
    }
}
