package with;

/**
 * BUILDER APPLIED.
 *
 * - The Burger is IMMUTABLE: all fields are final and there are no
 *   setters. It can only be created fully-formed via the Builder.
 * - The private constructor forces use of the Builder.
 * - Each builder step is NAMED and chainable, so call sites read like
 *   a sentence and new optional fields don't break existing callers.
 */
public class Burger {

    private final int size;          // required
    private final boolean cheese;    // optional
    private final boolean bacon;     // optional
    private final boolean lettuce;   // optional
    private final int extraPatties;  // optional

    // Only the Builder can call this.
    private Burger(Builder builder) {
        this.size = builder.size;
        this.cheese = builder.cheese;
        this.bacon = builder.bacon;
        this.lettuce = builder.lettuce;
        this.extraPatties = builder.extraPatties;
    }

    @Override
    public String toString() {
        return "Burger{size=" + size + ", cheese=" + cheese + ", bacon=" + bacon
                + ", lettuce=" + lettuce + ", extraPatties=" + extraPatties + "}";
    }

    /** The fluent builder. */
    public static class Builder {
        private final int size;          // required -> in builder's constructor
        private boolean cheese = false;  // sensible defaults for optionals
        private boolean bacon = false;
        private boolean lettuce = false;
        private int extraPatties = 0;

        public Builder(int size) {
            this.size = size;
        }

        public Builder addCheese() {
            this.cheese = true;
            return this; // return `this` to allow chaining
        }

        public Builder addBacon() {
            this.bacon = true;
            return this;
        }

        public Builder addLettuce() {
            this.lettuce = true;
            return this;
        }

        public Builder extraPatties(int count) {
            this.extraPatties = count;
            return this;
        }

        public Burger build() {
            // You can validate invariants here before constructing.
            if (size <= 0) {
                throw new IllegalStateException("size must be positive");
            }
            return new Burger(this);
        }
    }

    public static void main(String[] args) {
        // Reads like a sentence; order-independent; self-documenting.
        Burger burger = new Burger.Builder(12)
                .addCheese()
                .addLettuce()
                .extraPatties(1)
                .build();

        System.out.println(burger);
    }
}
