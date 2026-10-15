package with;

/**
 * The subsystem classes — unchanged and still fully usable on their own.
 * A facade simplifies access to them; it does not replace or hide them.
 */
public class Subsystem {

    public static class Amplifier {
        public void on() { System.out.println("Amplifier on"); }
        public void setVolume(int v) { System.out.println("Volume = " + v); }
    }

    public static class Lights {
        public void dim(int p) { System.out.println("Lights dimmed to " + p + "%"); }
    }

    public static class Projector {
        public void on() { System.out.println("Projector on"); }
        public void wideScreen() { System.out.println("Projector widescreen mode"); }
    }

    public static class Player {
        public void on() { System.out.println("Player on"); }
        public void play(String m) { System.out.println("Playing '" + m + "'"); }
    }
}
