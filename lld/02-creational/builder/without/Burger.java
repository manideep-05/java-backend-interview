package without;

/**
 * BUILDER NOT USED — the telescoping-constructor problem.
 *
 * Required: size. Optional: cheese, bacon, lettuce, extraPatties.
 * To support optional combinations you end up overloading constructors
 * or passing a confusing line of booleans. Call sites are unreadable
 * and fragile: swap two booleans and it still compiles but is wrong.
 */
public class Burger {

    private final int size;
    private final boolean cheese;
    private final boolean bacon;
    private final boolean lettuce;
    private final int extraPatties;

    public Burger(int size, boolean cheese, boolean bacon, boolean lettuce, int extraPatties) {
        this.size = size;
        this.cheese = cheese;
        this.bacon = bacon;
        this.lettuce = lettuce;
        this.extraPatties = extraPatties;
    }

    @Override
    public String toString() {
        return "Burger{size=" + size + ", cheese=" + cheese + ", bacon=" + bacon
                + ", lettuce=" + lettuce + ", extraPatties=" + extraPatties + "}";
    }

    public static void main(String[] args) {
        // Which boolean is which? Easy to get wrong, hard to read.
        Burger b = new Burger(12, true, false, true, 1);
        System.out.println(b);
    }
}
