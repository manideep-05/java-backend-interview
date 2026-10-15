package normal;

/**
 * Abstract piece. Each concrete piece defines its OWN movement rule by
 * overriding isValidMove — polymorphism replaces a switch(type) on the
 * board (OCP). The base also offers a path-clear helper for sliding pieces.
 */
public abstract class Piece {
    protected final Color color;

    protected Piece(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    /** Does this piece's rule permit from->to on this board? */
    public abstract boolean isValidMove(Board board, Position from, Position to);

    /** Helper: is the straight/diagonal path between from and to clear? */
    protected boolean pathClear(Board board, Position from, Position to) {
        int dr = Integer.compare(to.row, from.row);
        int dc = Integer.compare(to.col, from.col);
        int r = from.row + dr;
        int c = from.col + dc;
        while (r != to.row || c != to.col) {
            if (board.get(new Position(r, c)) != null) {
                return false; // something blocks the path
            }
            r += dr;
            c += dc;
        }
        return true;
    }

    public abstract String symbol();
}
