package production;

/** Deterministic die for tests/demos. */
public class FixedDice implements Dice {
    private final int[] sequence;
    private int index = 0;

    public FixedDice(int... sequence) {
        this.sequence = sequence;
    }

    @Override
    public int roll() {
        int v = sequence[Math.min(index, sequence.length - 1)];
        index++;
        return v;
    }
}
