package production;

public class Board {
    private final Piece[][] grid = new Piece[8][8];

    public Piece get(Position p) {
        return grid[p.row][p.col];
    }

    public void set(Position p, Piece piece) {
        grid[p.row][p.col] = piece;
    }

    public void apply(Position from, Position to) {
        grid[to.row][to.col] = grid[from.row][from.col];
        grid[from.row][from.col] = null;
    }
}
