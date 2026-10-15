package production;

public class Knight extends Piece {
    public Knight(Color color) {
        super(color);
    }

    @Override
    public boolean isValidMove(Board board, Position from, Position to) {
        int dr = Math.abs(to.row - from.row);
        int dc = Math.abs(to.col - from.col);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }

    @Override
    public String symbol() {
        return color == Color.WHITE ? "N" : "n";
    }
}
