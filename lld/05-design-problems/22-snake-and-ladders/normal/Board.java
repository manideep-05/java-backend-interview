package normal;

import java.util.HashMap;
import java.util.Map;

/**
 * Board = size + a unified "jumps" map (cell -> destination). A snake is a
 * jump to a LOWER cell, a ladder to a HIGHER one. One code path handles both.
 */
public class Board {
    private final int size;
    private final Map<Integer, Integer> jumps = new HashMap<>();

    public Board(int size) {
        this.size = size;
    }

    public void addSnake(int head, int tail) {
        if (tail >= head) throw new IllegalArgumentException("snake must go down");
        jumps.put(head, tail);
    }

    public void addLadder(int bottom, int top) {
        if (top <= bottom) throw new IllegalArgumentException("ladder must go up");
        jumps.put(bottom, top);
    }

    /**
     * Compute the resulting cell for a move from `from` by `roll`.
     * Overshoot rule here: if it would pass the last cell, stay put.
     */
    public int move(int from, int roll) {
        int next = from + roll;
        if (next > size) {
            return from; // overshoot -> stay (explicit rule)
        }
        // Apply a single jump if one exists at the landing cell.
        return jumps.getOrDefault(next, next);
    }

    public int getSize() { return size; }
}
