package oop;

/**
 * An ENUM WITH BEHAVIOR.
 *
 * Each constant overrides apply(), so the enum itself is a tiny, type-safe
 * strategy table. Beats a switch on a String/int: the compiler guarantees
 * every operation is handled, and you can't pass an invalid one.
 */
public enum Operation {
    ADD("+") { public int apply(int a, int b) { return a + b; } },
    SUBTRACT("-") { public int apply(int a, int b) { return a - b; } },
    MULTIPLY("*") { public int apply(int a, int b) { return a * b; } };

    private final String symbol;

    Operation(String symbol) { this.symbol = symbol; } // enums can have fields + constructors

    public abstract int apply(int a, int b); // each constant supplies behavior

    public String getSymbol() { return symbol; }

    static void demo() {
        for (Operation op : Operation.values()) {
            System.out.println("6 " + op.getSymbol() + " 3 = " + op.apply(6, 3));
        }
    }
}
