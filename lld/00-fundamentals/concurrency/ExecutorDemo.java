package conc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * ExecutorService — a managed thread pool. Submit tasks, collect Futures,
 * always shut down. Don't hand-spawn threads in real code.
 */
public class ExecutorDemo {
    static void demo() throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                final int n = i;
                // Callable returns a value via a Future.
                futures.add(pool.submit(() -> n * n));
            }
            int sum = 0;
            for (Future<Integer> f : futures) {
                sum += f.get(); // blocks until that task completes
            }
            System.out.println("Sum of squares 1..8 (computed in pool) = " + sum); // 204
        } finally {
            pool.shutdown(); // stop accepting new tasks
            pool.awaitTermination(2, TimeUnit.SECONDS);
        }
    }
}
