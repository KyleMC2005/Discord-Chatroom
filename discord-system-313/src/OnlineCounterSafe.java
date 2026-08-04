
import java.util.concurrent.locks.ReentrantReadWriteLock;

// To make it thread safe, ReentrantReadWriteLock is used, so that only one thread can increment or decrement the counter,
// but multiple threads can read the number of users online at once

public class OnlineCounterSafe {          // Total number of online users
    private int counter = 0;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // Uses write lock
    public void incrementOnline() {
        lock.writeLock().lock();  // Acquire write lock
        try {
            counter++;
        } finally {
            lock.writeLock().unlock();  // Release write lock
        }
    }

    // Uses write lock
    public void decrementOnline() {
        lock.writeLock().lock();  // Acquire write lock
        try {
            counter--;
        } finally {
            lock.writeLock().unlock();  // Release write lock
        }
    }

    // Uses read lock
    public int getOnlineCount() {
        lock.readLock().lock();  // Acquire read lock
        try {
            return counter;
        } finally {
            lock.readLock().unlock();  // Release read lock
        }
    }

}