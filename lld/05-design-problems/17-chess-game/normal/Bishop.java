package normal;

/** Bishop: moves diagonally any distance; path must be clear. */
public class Bishop extends Piece {
    public Bishop(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(Board board, Position from, Position to) {
        int dr = Math.abs(to.row - from.row);
        int dc = Math.abs(to.col - from.col);
        if (dr != dc || dr == 0) {
            return false; // must be a non-zero diagonal
        }
        return pathClear(board, from, to);
    }

    @Override
    public String symbol() {
        return color == Color.WHITE ? "B" : "b";
    }
}
