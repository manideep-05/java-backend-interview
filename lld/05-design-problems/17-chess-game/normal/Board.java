package normal;

/**
 * The 8x8 board: a grid of Piece (or null). Responsibility: store pieces
 * and apply a move. It does NOT know piece movement rules — those live in
 * the pieces (polymorphism), so the board never switches on type.
 */
public class Board {
    private final Piece[][] grid = new Piece[8][8];

    public Piece get(Position p) {
        return grid[p.row][p.col];
    }

    public void set(Position p, Piece piece) {
        grid[p.row][p.col] = piece;
    }

    /** Move a piece, capturing whatever is at `to` (assumes move already validated). */
    public void apply(Position from, Position to) {
        grid[to.row][to.col] = grid[from.row][from.col];
        grid[from.row][from.col] = null;
    }
}
