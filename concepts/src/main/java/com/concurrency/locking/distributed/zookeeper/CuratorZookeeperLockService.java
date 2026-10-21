package com.concurrency.locking.distributed.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <h1>DISTRIBUTED LOCK: ZOOKEEPER & APACHE CURATOR</h1>
 *
 * <h2>ZooKeeper Mechanics: Ephemeral Sequential Nodes:</h2>
 * <ol>
 *   <li>When a client wants to acquire a lock on `/locks/order_payment`, it creates an
 *       <b>EPHEMERAL_SEQUENTIAL</b> znode: `/locks/order_payment/lock-000000001`.</li>
 *   <li>The client retrieves all children under `/locks/order_payment`.</li>
 *   <li>If its created znode has the <b>lowest sequence number</b>, the client owns the lock!</li>
 *   <li>If not lowest: The client registers a Watcher <b>ONLY on the znode immediately preceding its own sequence number</b>
 *       (e.g., node 3 watches node 2, node 2 watches node 1).</li>
 * </ol>
 *
 * <h2>Herd Effect Prevention (Crucial SDE-2 Concept):</h2>
 * <p>If all 1,000 waiting clients watched the root lock znode, releasing the lock would broadcast 1,000 wake-up events simultaneously
 * (Herd Effect / Thundering Herd), crushing network and ZooKeeper CPU.
 * By watching only the immediate predecessor, each lock release only wakes up <b>ONE client (O(1))</b>.</p>
 *
 * <h2>Automatic Fault Tolerance:</h2>
 * <p>Because nodes are <b>EPHEMERAL</b>, if a client process crashes, its TCP heartbeat with ZooKeeper is severed.
 * ZooKeeper detects session timeout and automatically deletes the ephemeral node, preventing deadlocks!</p>
 */
@Service
@Slf4j
public class CuratorZookeeperLockService {

    private final CuratorFramework curatorFramework;

    public CuratorZookeeperLockService(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    /**
     * Executes logic guarded by a ZooKeeper InterProcessMutex distributed lock.
     */
    public <T> T executeWithZkLock(String lockPath, long waitTime, TimeUnit timeUnit, Supplier<T> task) {
        InterProcessMutex mutex = new InterProcessMutex(curatorFramework, lockPath);
        boolean acquired = false;

        try {
            log.info("Attempting to acquire ZooKeeper lock at path: {}", lockPath);
            acquired = mutex.acquire(waitTime, timeUnit);
            if (!acquired) {
                log.warn("Failed to acquire ZK lock at path {} within {} {}", lockPath, waitTime, timeUnit);
                throw new IllegalStateException("Failed to acquire ZK lock: " + lockPath);
            }

            log.info("Acquired ZooKeeper lock at path: {}", lockPath);
            return task.get();

        } catch (Exception e) {
            log.error("Error during ZooKeeper lock execution", e);
            throw new RuntimeException("ZK lock error", e);
        } finally {
            if (acquired) {
                try {
                    mutex.release();
                    log.info("Released ZooKeeper lock at path: {}", lockPath);
                } catch (Exception e) {
                    log.error("Failed to release ZooKeeper lock at path: {}", lockPath, e);
                }
            }
        }
    }
}
