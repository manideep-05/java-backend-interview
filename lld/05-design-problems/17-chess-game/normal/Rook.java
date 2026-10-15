package normal;

/** Rook: moves any distance along a rank or file; path must be clear. */
public class Rook extends Piece {
    public Rook(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(Board board, Position from, Position to) {
        // Must stay on one rank or file (not both unchanged = not a move).
        if (from.row != to.row && from.col != to.col) {
            return false;
        }
        return pathClear(board, from, to);
    }

    @Override
    public String symbol() {
        return color == Color.WHITE ? "R" : "r";
    }
}
