package production;

/**
 * PRODUCTION game — online two-player server.
 *
 * Concurrency design:
 *  - move() is `synchronized` on the game, so the compound action
 *    "check status + check turn + check cell + place + win/draw check +
 *    advance turn" is atomic. Two players submitting moves on different
 *    threads can't corrupt the board or both move in one turn.
 *  - Out-of-turn and post-game moves are rejected (return false).
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

    public synchronized boolean move(Player player, int r, int c) {
        if (status != GameStatus.IN_PROGRESS) return false;
        if (player != players[turn % 2]) return false;
        if (!board.isEmpty(r, c)) return false;

        board.place(r, c, player.getMark());
        if (board.wins(r, c, player.getMark())) {
            status = (player.getMark() == Mark.X) ? GameStatus.X_WON : GameStatus.O_WON;
        } else if (board.isFull()) {
            status = GameStatus.DRAW;
        }
        turn++;
        return true;
    }

    public synchronized GameStatus getStatus() { return status; }

    public static void main(String[] args) throws InterruptedException {
        Player x = new Player("X", Mark.X);
        Player o = new Player("O", Mark.O);
        Game game = new Game(3, x, o);

        // Two clients hammer the same cell concurrently; only one legal
        // move can win the turn, and the board can't be corrupted.
        Runnable xMove = () -> game.move(x, 0, 0);
        Runnable oMove = () -> game.move(o, 0, 0); // o is out of turn here
        Thread t1 = new Thread(xMove), t2 = new Thread(oMove);
        t1.start(); t2.start(); t1.join(); t2.join();

        // Then finish a quick X win sequentially.
        game.move(o, 1, 0);
        game.move(x, 0, 1);
        game.move(o, 1, 1);
        game.move(x, 0, 2);
        System.out.println("Status: " + game.getStatus()); // X_WON
    }
}
