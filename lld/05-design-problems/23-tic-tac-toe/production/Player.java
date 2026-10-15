package production;

public class Player {
    private final String name;
    private final Mark mark;

    public Player(String name, Mark mark) {
        this.name = name;
        this.mark = mark;
    }

    public String getName() { return name; }
    public Mark getMark() { return mark; }
}
