package normal;

/**
 * A deterministic die for tests/demos: returns a scripted sequence,
 * then repeats the last value. Makes the game reproducible.
 */
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
