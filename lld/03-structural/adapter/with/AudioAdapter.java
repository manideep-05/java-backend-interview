package with;

/**
 * ADAPTER APPLIED.
 *
 * Implements the interface our app expects (MediaPlayer) and internally
 * HOLDS the incompatible adaptee (AdvancedAudioLib), translating the
 * call. Our app depends only on MediaPlayer and never sees playMp3.
 */
public class AudioAdapter implements MediaPlayer {

    // "Object adapter": composition over inheritance.
    private final AdvancedAudioLib advancedAudioLib;

    public AudioAdapter(AdvancedAudioLib advancedAudioLib) {
        this.advancedAudioLib = advancedAudioLib;
    }

    @Override
    public void play(String fileName) {
        // Translate our interface's call into the adaptee's API.
        advancedAudioLib.playMp3(fileName);
    }
}
