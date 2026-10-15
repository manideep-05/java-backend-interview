package production;

/**
 * PRODUCTION game (online two-player server scenario).
 *
 * Concurrency design:
 *  - move() is `synchronized` on the game. Chess is turn-based, but on a
 *    networked server both players' move submissions can arrive on
 *    different threads at nearly the same time. Serializing move() means
 *    the sequence "read turn -> validate -> apply -> toggle turn" is
 *    atomic, so the turn invariant can't be violated (no two moves
 *    slipping through for the "same" turn, no torn board state).
 *  - `turn` is only ever read/written while holding the game lock, so it
 *    doesn't need to be volatile.
 *  - Per GAME locking (not global): different games run fully in parallel.
 */
public class Game {
    private final Board board = new Board();
    private Color turn = Color.WHITE;

    public Board getBoard() {
        return board;
    }

    public synchronized boolean move(Position from, Position to) {
        if (!from.onBoard() || !to.onBoard()) {
            return false;
        }
        Piece piece = board.get(from);
        if (piece == null || piece.getColor() != turn) {
            return false; // not your turn / no piece
        }
        Piece target = board.get(to);
        if (target != null && target.getColor() == turn) {
            return false; // own piece
        }
        if (!piece.isValidMove(board, from, to)) {
            return false;
        }
        board.apply(from, to);
        turn = (turn == Color.WHITE) ? Color.BLACK : Color.WHITE;
        return true;
    }

    public synchronized Color getTurn() {
        return turn;
    }

    public static void main(String[] args) throws InterruptedException {
        Game game = new Game();
        game.getBoard().set(new Position(0, 0), new Rook(Color.WHITE));
        game.getBoard().set(new Position(7, 7), new Rook(Color.BLACK));

        // Two players submit moves concurrently; only the side to move wins.
        Runnable white = () -> System.out.println("white move accepted: "
                + game.move(new Position(0, 0), new Position(0, 5)));
        Runnable black = () -> System.out.println("black move accepted: "
                + game.move(new Position(7, 7), new Position(7, 2)));
        Thread w = new Thread(white), b = new Thread(black);
        w.start(); b.start(); w.join(); b.join();

        // Exactly one of the two is accepted first (it was white's turn);
        // move() serialization guarantees the turn invariant holds.
        System.out.println("Turn is now: " + game.getTurn());
    }
}
