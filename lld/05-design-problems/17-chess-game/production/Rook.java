package production;

public class Rook extends Piece {
    public Rook(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(Board board, Position from, Position to) {
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
