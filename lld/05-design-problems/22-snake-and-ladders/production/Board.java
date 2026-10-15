package production;

import java.util.HashMap;
import java.util.Map;

/** Board + unified jumps map (immutable after setup, so safe to read concurrently). */
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

    public int move(int from, int roll) {
        int next = from + roll;
        if (next > size) {
            return from;
        }
        return jumps.getOrDefault(next, next);
    }

    public int getSize() { return size; }
}
