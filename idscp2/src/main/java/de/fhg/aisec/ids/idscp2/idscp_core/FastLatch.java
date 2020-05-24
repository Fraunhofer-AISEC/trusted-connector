package de.fhg.aisec.ids.idscp2.idscp_core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This latch implementation uses double-checked-locking, a concept that is mostly broken.
 * However, double-checked locking <b>does</b> work for primitives that are atomic w.r.t. the memory model,
 * see https://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
 * It is assumed that the JVM implementation always handles byte vars atomically,
 * otherwise the correctness of this code may be broken!
 *
 * @author Michael Lux (michael.lux@aisec.fraunhofer.de)
 */
public class FastLatch {
    private static final Logger LOG = LoggerFactory.getLogger(FastLatch.class);
    private byte locked = 1;

    /**
     * Wait for this latch to be unlocked.
     */
    public void await() {
        // Check locked flag without synchronization, such that method returns immediately
        // without synchronization overhead if unlocked.
        while (locked != 0) {
            synchronized (this) {
                // Check the locked flag again to prevent eternal waiting if notifyAll() has been called
                // before this critical section.
                if (locked != 0) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                        LOG.warn("Ignored InterruptException, awaiting unlock...", ie);
                    }
                }
            }
        }
    }

    /**
     * Unlocks this latch instance.
     */
    public void unlock() {
        synchronized (this) {
            locked = 0;
            notifyAll();
        }
    }

}
