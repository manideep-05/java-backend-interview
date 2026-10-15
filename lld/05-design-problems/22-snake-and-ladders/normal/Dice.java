package normal;

/**
 * Dice as an injectable SEAM (Strategy/DI). Production rolls randomly;
 * tests feed a fixed sequence for determinism.
 */
public interface Dice {
    int roll();
}
