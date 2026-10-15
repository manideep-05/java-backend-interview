package with;

/**
 * The client talks only to MediaPlayer. The adapter hides the
 * third-party library entirely — we could swap the library for another
 * by writing a new adapter, with no change to this code.
 */
public class Main {
    public static void main(String[] args) {
        MediaPlayer player = new AudioAdapter(new AdvancedAudioLib());
        player.play("song.mp3"); // our interface, library does the work
    }
}
