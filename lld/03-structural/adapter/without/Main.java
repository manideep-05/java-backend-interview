package without;

/**
 * ADAPTER NOT USED — the problem.
 *
 * Our app is built around a MediaPlayer.play(filename) call. But the
 * only library we have exposes playMp3(path) with a different name and
 * shape. The interfaces are incompatible, so the client is stuck
 * calling the library directly and leaking its API everywhere.
 */
public class Main {

    // The third-party library we cannot modify.
    static class AdvancedAudioLib {
        void playMp3(String path) {
            System.out.println("Playing MP3 file: " + path);
        }
    }

    public static void main(String[] args) {
        // The client wanted to call something like player.play("song.mp3"),
        // but is forced to know and use the library's own method directly.
        AdvancedAudioLib lib = new AdvancedAudioLib();
        lib.playMp3("song.mp3");
    }
}
