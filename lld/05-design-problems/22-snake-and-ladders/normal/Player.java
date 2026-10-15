package normal;

/** A player: a name and a current position (dumb data). */
public class Player {
    private final String name;
    private int position = 0;

    public Player(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
