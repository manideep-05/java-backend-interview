package normal;

/**
 * Orchestrates turn alternation and status. Validates each move, applies
 * it, runs the incremental win check, and detects draws via the full-board
 * flag (no scanning for empties).
 */
public class Game {
    private final Board board;
    private final Player[] players;
    private int turn = 0;
    private GameStatus status = GameStatus.IN_PROGRESS;

    public Game(int n, Player x, Player o) {
        this.board = new Board(n);
        this.players = new Player[]{x, o};
    }

    /** Returns true if the move was legal and applied. */
    public boolean move(Player player, int r, int c) {
        if (status != GameStatus.IN_PROGRESS) return false;   // game over
        if (player != players[turn % 2]) return false;        // not your turn
        if (!board.isEmpty(r, c)) return false;               // occupied/out of bounds

        board.place(r, c, player.getMark());
        if (board.wins(r, c, player.getMark())) {
            status = (player.getMark() == Mark.X) ? GameStatus.X_WON : GameStatus.O_WON;
        } else if (board.isFull()) {
            status = GameStatus.DRAW;
        }
        turn++;
        return true;
    }

    public GameStatus getStatus() { return status; }

    public static void main(String[] args) {
        Player x = new Player("X", Mark.X);
        Player o = new Player("O", Mark.O);
        Game game = new Game(3, x, o);

        game.move(x, 0, 0);
        game.move(o, 1, 0);
        game.move(x, 0, 1);
        game.move(o, 1, 1);
        game.move(x, 0, 2); // X completes the top row
        System.out.println("Status: " + game.getStatus()); // X_WON
    }
}
