package production;

import java.util.List;

/**
 * PRODUCTION game — online multiplayer server scenario.
 *
 * Concurrency design (same shape as the Chess production version):
 *  - playTurn(player) is `synchronized` on the game. In an online game,
 *    move requests from different players arrive on different threads at
 *    nearly the same time. Serializing playTurn makes "read whose-turn ->
 *    validate -> move -> advance turn" atomic, so the turn invariant holds.
 *  - It REJECTS a move from anyone who isn't the current player (returns
 *    false), so a player can't move out of turn or move twice.
 *  - Shared mutable state (currentTurn, winner, player positions) is only
 *    mutated while holding the game lock.
 */
public class Game {
    private final Board board;
    private final Dice dice;
    private final List<Player> players;
    private int currentTurn = 0;
    private Player winner = null;

    public Game(Board board, Dice dice, List<Player> players) {
        this.board = board;
        this.dice = dice;
        this.players = players;
    }

    /** A player's attempt to take their turn. Returns true if it was their turn and it was played. */
    public synchronized boolean playTurn(Player player) {
        if (winner != null) {
            return false; // game over
        }
        Player expected = players.get(currentTurn % players.size());
        if (player != expected) {
            return false; // not your turn -> rejected (turn invariant)
        }
        int roll = dice.roll();
        int newPos = board.move(player.getPosition(), roll);
        player.setPosition(newPos);
        System.out.println(player.getName() + " rolled " + roll + " -> " + newPos);
        if (newPos == board.getSize()) {
            winner = player;
            System.out.println(player.getName() + " wins!");
        }
        currentTurn++;
        return true;
    }

    public synchronized Player getWinner() { return winner; }
    public synchronized Player currentPlayer() { return players.get(currentTurn % players.size()); }

    public static void main(String[] args) throws InterruptedException {
        Board board = new Board(30);
        board.addLadder(3, 11);
        board.addSnake(27, 5);
        Player a = new Player("A"), b = new Player("B");
        Game game = new Game(board, new FixedDice(6), List.of(a, b));

        // Two client threads both try to move at once; only the current
        // player's move is accepted each round — turn invariant preserved.
        while (game.getWinner() == null) {
            Player cur = game.currentPlayer();
            Runnable moveA = () -> game.playTurn(a);
            Runnable moveB = () -> game.playTurn(b);
            Thread t1 = new Thread(moveA), t2 = new Thread(moveB);
            t1.start(); t2.start(); t1.join(); t2.join();
            if (game.currentPlayer() == cur && game.getWinner() == null) {
                // safety: both rejected shouldn't happen, but guard the demo loop
                break;
            }
        }
    }
}
