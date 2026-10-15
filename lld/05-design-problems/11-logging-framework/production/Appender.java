package production;

/** STRATEGY: where/how a formatted line is written. */
public interface Appender {
    void append(String line);
}
