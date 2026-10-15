package production;

/**
 * A synchronous appender whose write is `synchronized` so concurrent
 * logging threads can't interleave into a single corrupted line. (For a
 * file this would guard the actual file write / buffer flush.)
 */
public class ConsoleAppender implements Appender {
    private final Object writeLock = new Object();

    @Override
    public void append(String line) {
        synchronized (writeLock) { // serialize writes => no interleaved lines
            System.out.println(line);
        }
    }
}
