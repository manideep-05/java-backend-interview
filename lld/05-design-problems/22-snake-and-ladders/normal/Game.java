package normal;

import java.util.List;

/**
 * Orchestrates the turn loop. Board/Player/Dice are dumb; the Game owns
 * the sequencing and win detection — kept small and readable.
 */
public class Game {
    private final Board board;
    private final Dice dice;
    private final List<Player> players;

    public Game(Board board, Dice dice, List<Player> players) {
        this.board = board;
        this.dice = dice;
        this.players = players;
    }

    /** Play until someone reaches the last cell. Returns the winner. */
    public Player play() {
        int turn = 0;
        while (true) {
            Player current = players.get(turn % players.size());
            int roll = dice.roll();
            int newPos = board.move(current.getPosition(), roll);
            current.setPosition(newPos);
            System.out.println(current.getName() + " rolled " + roll + " -> " + newPos);
            if (newPos == board.getSize()) {
                System.out.println(current.getName() + " wins!");
                return current;
            }
            turn++;
        }
    }

    public static void main(String[] args) {
        Board board = new Board(30);
        board.addLadder(3, 11);
        board.addSnake(27, 5);

        // FixedDice makes the demo deterministic.
        Dice dice = new FixedDice(3, 6, 6, 6, 6, 6);
        Game game = new Game(board, dice, List.of(new Player("A"), new Player("B")));
        game.play();
    }
}
