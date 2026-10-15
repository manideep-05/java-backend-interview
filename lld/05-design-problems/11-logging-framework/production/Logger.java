package production;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PRODUCTION logger.
 *
 * - minLevel filter is a cheap early return on the hot path.
 * - appenders in a CopyOnWriteArrayList: configured rarely, read on every
 *   log call, so copy-on-write gives lock-free iteration.
 * - The expensive write is pushed to an AsyncAppender, so log() returns
 *   almost immediately regardless of I/O latency.
 */
public class Logger {
    private final LogLevel minLevel;
    private final List<Appender> appenders = new CopyOnWriteArrayList<>();

    public Logger(LogLevel minLevel) {
        this.minLevel = minLevel;
    }

    public void addAppender(Appender appender) {
        appenders.add(appender);
    }

    public void log(LogLevel level, String message) {
        if (level.ordinal() < minLevel.ordinal()) {
            return; // cheap filter
        }
        String line = "[" + level + "] " + message;
        for (Appender appender : appenders) {
            appender.append(line);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        AsyncAppender async = new AsyncAppender(new ConsoleAppender(), 1000);
        Logger logger = new Logger(LogLevel.INFO);
        logger.addAppender(async);

        // Many threads log concurrently; writes are serialized by the
        // single async worker, so no interleaved/corrupted lines.
        Runnable w = () -> {
            for (int i = 0; i < 100; i++) {
                logger.log(LogLevel.INFO, Thread.currentThread().getName() + " msg " + i);
            }
        };
        Thread t1 = new Thread(w, "t1"), t2 = new Thread(w, "t2");
        t1.start(); t2.start();
        t1.join(); t2.join();

        async.close(); // flush on shutdown
        System.out.println("All logs flushed.");
    }
}
