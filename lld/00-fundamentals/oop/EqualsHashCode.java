package oop;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * equals()/hashCode() contract — and what breaks when you ignore it.
 *
 * Contract: if a.equals(b) then a.hashCode() == b.hashCode().
 * HashSet/HashMap first bucket by hashCode, THEN compare with equals.
 * If two "equal" objects have different hash codes, they land in different
 * buckets and the set treats them as distinct -> duplicates leak in.
 */
public class EqualsHashCode {

    // BROKEN: overrides equals but NOT hashCode (uses Object's identity hash).
    static class BadPoint {
        final int x, y;
        BadPoint(int x, int y) { this.x = x; this.y = y; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof BadPoint)) return false;
            BadPoint p = (BadPoint) o;
            return x == p.x && y == p.y;
        }
        // no hashCode() -> contract violated
    }

    // CORRECT: equals and hashCode consistent and based on the same fields.
    static final class GoodPoint {
        final int x, y;
        GoodPoint(int x, int y) { this.x = x; this.y = y; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof GoodPoint)) return false;
            GoodPoint p = (GoodPoint) o;
            return x == p.x && y == p.y;
        }
        @Override public int hashCode() { return Objects.hash(x, y); }
    }

    static void demo() {
        Set<BadPoint> bad = new HashSet<>();
        bad.add(new BadPoint(1, 1));
        bad.add(new BadPoint(1, 1)); // "equal" but different hash -> added again
        System.out.println("BadPoint set size (want 1): " + bad.size()); // 2 — BUG

        Set<GoodPoint> good = new HashSet<>();
        good.add(new GoodPoint(1, 1));
        good.add(new GoodPoint(1, 1)); // correctly recognized as duplicate
        System.out.println("GoodPoint set size (want 1): " + good.size()); // 1
    }
}
