package normal;

/**
 * Orchestrates the move loop and the turn invariant. move() runs the
 * validation checklist, then applies the move and toggles the turn.
 */
public class Game {
    private final Board board = new Board();
    private Color turn = Color.WHITE;

    public Board getBoard() {
        return board;
    }

    /** Validate (bounds, turn, rule, own-piece) then apply. */
    public boolean move(Position from, Position to) {
        if (!from.onBoard() || !to.onBoard()) {
            return false; // off board
        }
        Piece piece = board.get(from);
        if (piece == null || piece.getColor() != turn) {
            return false; // no piece, or not your turn
        }
        Piece target = board.get(to);
        if (target != null && target.getColor() == turn) {
            return false; // can't capture own piece
        }
        if (!piece.isValidMove(board, from, to)) {
            return false; // piece's own rule rejects it
        }
        board.apply(from, to);
        turn = (turn == Color.WHITE) ? Color.BLACK : Color.WHITE; // toggle turn
        return true;
    }

    public static void main(String[] args) {
        Game game = new Game();
        Board b = game.getBoard();
        b.set(new Position(0, 0), new Rook(Color.WHITE));
        b.set(new Position(0, 1), new Knight(Color.WHITE));

        System.out.println("Rook a1->a5: " + game.move(new Position(0, 0), new Position(4, 0))); // true
        // Now it's black's turn; a white move is rejected.
        System.out.println("White tries again: " + game.move(new Position(0, 1), new Position(2, 2))); // false (not white's turn)
    }
}
