package production;

/**
 * N x N board with incremental O(N) win check. Mutated only under the
 * Game's lock, so the board itself needs no internal synchronization.
 */
public class Board {
    private final int n;
    private final Mark[][] grid;
    private int filled = 0;

    public Board(int n) {
        this.n = n;
        this.grid = new Mark[n][n];
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                grid[r][c] = Mark.EMPTY;
            }
        }
    }

    public boolean isEmpty(int r, int c) {
        return r >= 0 && r < n && c >= 0 && c < n && grid[r][c] == Mark.EMPTY;
    }

    public void place(int r, int c, Mark mark) {
        grid[r][c] = mark;
        filled++;
    }

    public boolean isFull() {
        return filled == n * n;
    }

    public boolean wins(int r, int c, Mark mark) {
        boolean row = true, col = true, diag = true, anti = true;
        for (int i = 0; i < n; i++) {
            if (grid[r][i] != mark) row = false;
            if (grid[i][c] != mark) col = false;
            if (grid[i][i] != mark) diag = false;
            if (grid[i][n - 1 - i] != mark) anti = false;
        }
        return row || col || (r == c && diag) || (r + c == n - 1 && anti);
    }

    public int size() { return n; }
}
