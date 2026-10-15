package normal;

/**
 * N x N board with an INCREMENTAL win check.
 *
 * After placing at (r,c) we only inspect row r, column c, and the two
 * diagonals if (r,c) lies on them — O(N) per move, not an O(N^2) rescan.
 * Generalizes to any N (and could extend to K-in-a-row).
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
        return inBounds(r, c) && grid[r][c] == Mark.EMPTY;
    }

    public void place(int r, int c, Mark mark) {
        grid[r][c] = mark;
        filled++;
    }

    public boolean isFull() {
        return filled == n * n;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < n && c >= 0 && c < n;
    }

    /** True if the last move at (r,c) by `mark` completed a line. O(N). */
    public boolean wins(int r, int c, Mark mark) {
        boolean row = true, col = true, diag = true, anti = true;
        for (int i = 0; i < n; i++) {
            if (grid[r][i] != mark) row = false;   // row r
            if (grid[i][c] != mark) col = false;   // column c
            if (grid[i][i] != mark) diag = false;  // main diagonal
            if (grid[i][n - 1 - i] != mark) anti = false; // anti-diagonal
        }
        // Diagonals only count if (r,c) is actually on them.
        boolean onDiag = (r == c);
        boolean onAnti = (r + c == n - 1);
        return row || col || (onDiag && diag) || (onAnti && anti);
    }

    public int size() { return n; }
}
