package org.apache.synapse.transport.passthru.jmx;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;


public class LatencyParameter {
    private static final int SMALL_DATA_COLLECTION_PERIOD = 5;
    private static final int LARGE_DATA_COLLECTION_PERIOD = 5 * 60;
    private static final int SAMPLES_PER_MINUTE = 60 / SMALL_DATA_COLLECTION_PERIOD;
    private static final int SAMPLES_PER_HOUR = (60 * 60) / LARGE_DATA_COLLECTION_PERIOD;

    private AtomicLong lastValue;

    /**
     * Queue of all latency values reported. The short term data collector clears this queue up
     * time to time thus ensuring it doesn't grow indefinitely.
     */
    private Queue<Long> cache;

    /**
     * Queue of samples collected by the short term data collector. This is maintained
     * as a fixed length queue
     */
    private Queue<Long> shortTermCache;

    /**
     * Queue of samples collected by the long term data collector. This is maintained
     * as a fixed length queue
     */
    private Queue<Long> longTermCache;

    private double allTimeAverage = 0.0;

    private int count = 0;

    private final Object lock = new Object();

    private final Object shortTermCacheLock = new Object();

    private final Object longTermCacheLock = new Object();

    private final boolean enabled;

    public LatencyParameter(boolean enabled) {
        this.enabled = enabled;
        if (!this.enabled) {
            return;
        }
        lastValue = new AtomicLong(0);
        cache = new ConcurrentLinkedQueue<Long>();
        shortTermCache = new LinkedList<Long>();
        longTermCache = new LinkedList<Long>();
    }

    public void reset() {
        if (!enabled) {
            return;
        }
        lastValue.set(0);
        cache.clear();
        synchronized (shortTermCacheLock) {
            shortTermCache.clear();
        }
        synchronized (longTermCacheLock) {
            longTermCache.clear();
        }
        synchronized (lock) {
            allTimeAverage = 0.0;
            count = 0;
        }
    }

    public long getLatency() {
        return enabled ? lastValue.get() : 0L;
    }

    public double getAllTimeAverage() {
        synchronized (lock) {
            return enabled ? allTimeAverage : 0.0;
        }
    }

    public void updateCache() {
        if (!enabled) {
            return;
        }
        updateCacheQueue();
    }

    public void update(long value) {
        if (!enabled) {
            return;
        }
        lastValue.set(value);
        cache.offer(lastValue.get());
    }

    private void updateCacheQueue() {
        if (!enabled) {
            return;
        }
        int size = cache.size();
        if (size > 0) {
            long sum = 0;
            for (int i = 0; i < size; i++) {
                sum += cache.poll();
            }
            synchronized (lock) {
                allTimeAverage = (allTimeAverage * count + sum) / (count + size);
                count = count + size;
            }
        }
        updateShortTermCache(size);
    }

    private void updateShortTermCache(int size) {
        if (!enabled) {
            return;
        }
        long value = getLatency();
        synchronized (shortTermCacheLock) {
            if (shortTermCache.size() != 0 || value != 0) {
                // take a sample for the short term latency calculation
                if (shortTermCache.size() == SAMPLES_PER_MINUTE * 15) {
                    shortTermCache.remove();
                }
                if (size == 0) {
                    // there's no latency data available -> no new requests received
                    shortTermCache.offer(0L);
                } else {
                    shortTermCache.offer(value);
                }
            }
        }
    }

    public void updateLongTermCache() {
        if (!enabled) {
            return;
        }
        synchronized (longTermCacheLock) {
            if (longTermCache.size() != 0 || getLatency() != 0) {
                if (longTermCache.size() == SAMPLES_PER_HOUR * 24) {
                    longTermCache.remove();
                }
                // adds the average latency value in last five minutes
                longTermCache.offer((long) getAverageLatencyByMinute(LARGE_DATA_COLLECTION_PERIOD / 60));
            }
        }
    }

    public double getAverageLatency15m() {
        return getAverageLatencyByMinute(15);
    }

    public double getAverageLatency5m() {
        return getAverageLatencyByMinute(5);
    }

    public double getAverageLatency1m() {
        return getAverageLatencyByMinute(1);
    }

    public double getAverageLatency24h() {
        return getAverageLatencyByHour(24);
    }

    public double getAverageLatency8h() {
        return getAverageLatencyByHour(8);
    }

    public double getAverageLatency1h() {
        return getAverageLatencyByHour(1);
    }

    private double getAverageLatencyByMinute(int n) {
        if (!enabled) {
            return 0.0;
        }
        int samples = n * SAMPLES_PER_MINUTE;
        double sum = 0.0;
        Long[] array;
        synchronized (shortTermCacheLock) {
            array = shortTermCache.toArray(new Long[shortTermCache.size()]);
        }
        if (samples > array.length) {
            // If we don't have enough samples collected yet
            // add up everything we have
            samples = array.length;
            for (int i = 0; i < array.length; i++) {
                sum += array[i];
            }
        } else {
            // We have enough samples to make the right calculation
            // Add up starting from the end of the queue (to give the most recent values)
            for (int i = 0; i < samples; i++) {
                sum += array[array.length - 1 - i];
            }
        }

        if (samples == 0) {
            return 0.0;
        }
        return sum / samples;
    }

    private double getAverageLatencyByHour(int n) {
        if (!enabled) {
            return 0.0;
        }
        int samples = n * SAMPLES_PER_HOUR;
        double sum = 0.0;
        Long[] array;
        synchronized (longTermCacheLock) {
            array = longTermCache.toArray(new Long[longTermCache.size()]);
        }
        if (samples > array.length) {
            samples = array.length;
            for (int i = 0; i < array.length; i++) {
                sum += array[i];
            }
        } else {
            for (int i = 0; i < samples; i++) {
                sum += array[array.length - 1 - i];
            }
        }

        if (samples == 0) {
            return 0.0;
        }
        return sum / samples;
    }
}
