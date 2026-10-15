package oop;

/**
 * A correct IMMUTABLE value object.
 *
 * Recipe:
 *  - class is final (can't be subclassed to add mutable state)
 *  - all fields final, set once in the constructor
 *  - no setters
 *  - "mutating" operations RETURN A NEW INSTANCE instead of changing this one
 *
 * Immutable objects are inherently thread-safe (no mutation => no races),
 * safe to share/cache, and make good map keys.
 */
public final class ImmutableMoney {
    private final long amountCents;
    private final String currency;

    public ImmutableMoney(long amountCents, String currency) {
        this.amountCents = amountCents;
        this.currency = currency;
    }

    // Returns a NEW Money; the original is untouched.
    public ImmutableMoney plus(ImmutableMoney other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency mismatch");
        }
        return new ImmutableMoney(this.amountCents + other.amountCents, currency);
    }

    public long getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }

    @Override public String toString() { return amountCents + " " + currency; }

    static void demo() {
        ImmutableMoney a = new ImmutableMoney(1000, "USD");
        ImmutableMoney b = new ImmutableMoney(250, "USD");
        ImmutableMoney sum = a.plus(b);
        System.out.println(a + " + " + b + " = " + sum); // a and b unchanged
    }
}
