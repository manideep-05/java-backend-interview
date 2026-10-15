package production;

public class Player {
    private final String name;
    private volatile int position = 0; // read by display threads; written under game lock

    public Player(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
