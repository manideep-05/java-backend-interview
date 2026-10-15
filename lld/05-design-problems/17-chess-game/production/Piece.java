package production;

/** Abstract piece with its own movement rule (polymorphism, not a switch). */
public abstract class Piece {
    protected final Color color;

    protected Piece(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public abstract boolean isValidMove(Board board, Position from, Position to);

    protected boolean pathClear(Board board, Position from, Position to) {
        int dr = Integer.compare(to.row, from.row);
        int dc = Integer.compare(to.col, from.col);
        int r = from.row + dr;
        int c = from.col + dc;
        while (r != to.row || c != to.col) {
            if (board.get(new Position(r, c)) != null) {
                return false;
            }
            r += dr;
            c += dc;
        }
        return true;
    }

    public abstract String symbol();
}
