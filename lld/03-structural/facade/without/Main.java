package without;

/**
 * FACADE NOT USED — the problem.
 *
 * The client must know every subsystem class AND the exact order of
 * operations to watch a movie. This procedure gets copy-pasted into
 * every place that starts a movie, and a change to the sequence must
 * be fixed everywhere.
 */
public class Main {

    static class Amplifier { void on() { System.out.println("Amplifier on"); } void setVolume(int v) { System.out.println("Volume = " + v); } }
    static class Lights    { void dim(int p) { System.out.println("Lights dimmed to " + p + "%"); } }
    static class Projector { void on() { System.out.println("Projector on"); } void wideScreen() { System.out.println("Projector widescreen mode"); } }
    static class Player    { void on() { System.out.println("Player on"); } void play(String m) { System.out.println("Playing '" + m + "'"); } }

    public static void main(String[] args) {
        Amplifier amp = new Amplifier();
        Lights lights = new Lights();
        Projector projector = new Projector();
        Player player = new Player();

        // The client is burdened with the whole sequence.
        lights.dim(10);
        amp.on();
        amp.setVolume(5);
        projector.on();
        projector.wideScreen();
        player.on();
        player.play("Interstellar");
    }
}
