package org.apache.synapse.commons.throttle.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.throttle.core.internal.ThrottleServiceDataHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedParamManager {

	private static Map<String, Long> counters= new ConcurrentHashMap<String, Long>();//Locally managed counters map for non clustered environment
	private static Map<String, Long> timestamps = new ConcurrentHashMap<String, Long>();//Locally managed time stamps map for non clustered environment
	private static Log log = LogFactory.getLog(SharedParamManager.class.getName());

	private SharedParamManager() {
	}

	/**
	 * Return distributed shared counter for this caller context with given id. If it's not distributed will get from the
	 * local counter
	 *
	 * @param id of the shared counter
	 * @return shared hazelcast current shared counter
	 */
	public static long getDistributedCounter(String id) {
		if(log.isDebugEnabled()) {
			log.debug("GET TIMESTAMP WITH ID " + id);
		}
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			return distributedCounterManager.getCounter(id);
		} else {
			Long counter = counters.get(id);
			if (counter != null) {
				return counter;
			} else {
				counters.put(id, 0L);
				return 0;
			}
		}
	}

	/**
	 * Set distribute counter of caller context of given id to the provided value. If it's not distributed do the same for
	 * local counter
	 *
	 * @param id of the caller context
	 * @param value to set to the global counter
	 */
	public static void setDistributedCounter(String id, long value) {
		if(log.isDebugEnabled()) {
			log.debug("SETTING COUNTER WITH ID " + id);
		}
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			distributedCounterManager.setCounter(id,value);
		} else {
			counters.put(id, value);
		}
	}

	/**
	 * Set the distributed counter with the given id key with an expiry time
	 *
	 * @param id         key id
	 * @param value      value to set
	 * @param expiryTime expiry time in milliseconds
	 */
	public static void setDistributedCounterWithExpiry(String id, long value, long expiryTime) {
		if (log.isDebugEnabled()) {
			log.debug("SETTING COUNTER WITH ID " + id);
		}
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			//distributedCounterManager.setCounter(id, value);
			distributedCounterManager.setCounterWithExpiry(id, value, expiryTime);
		} else {
			counters.put(id, value);
		}
	}

	/**
	 * Set the shared timestamp with the given id key with an expiry time
	 *
	 * @param id         key id
	 * @param timestamp  timestamp value to set
	 * @param expiryTime expiry time in milliseconds
	 */
	public static void setSharedTimestampWithExpiry(String id, long timestamp, long expiryTime) {
		if (log.isDebugEnabled()) {
			log.debug("Setting the shared timestamp of key " + id + " with value " + timestamp + " with an expiry "
					+ "time of " + expiryTime);
		}
		String key = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			distributedCounterManager.setTimestampWithExpiry(key, timestamp, expiryTime);
		} else {
			timestamps.put(id, timestamp);
		}
	}

	/**
	 * Add given value to the distribute counter of caller context of given id. If it's not
	 * distributed return local counter
	 *
	 * @param id of the caller context
	 * @param value to set to the global counter
	 */
	public static long addAndGetDistributedCounter(String id, long value) {

		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			return distributedCounterManager.addAndGetCounter(id, value);
		} else {
			// Atomically increment counter and return new total.
			return counters.compute(id, (k, v) -> (v == null ? 0L : v) + value);
		}
	}

	/**
	 * Asynchronously add given value to the distribute counter of caller context of given id. If it's not
	 * distributed return local counter. This will return global value before add the provided counter
	 *
	 * @param id of the caller context
	 * @param value to set to the global counter
	 */
	public static long asyncGetAndAddDistributedCounter(String id, long value) {
		if(log.isDebugEnabled()) {
			log.debug("ASYNC CREATING AND SETTING COUNTER WITH ID " + id);
		}
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			return distributedCounterManager.asyncGetAndAddCounter(id, value);
		} else {
			// Atomically increment counter and return old value (before the add).
			long[] oldValue = {0L};
			counters.compute(id, (k, v) -> {
				oldValue[0] = (v == null ? 0L : v);
				return oldValue[0] + value;
			});
			return oldValue[0];
		}
	}

	/**
	 * Asynchronously add given value to the distribute counter of caller context of given id. If it's not
	 * distributed return local counter. This will return global value before add the provided counter
	 *
	 * @param id of the caller context
	 * @param value to set to the global counter
	 */
	public static long asyncGetAndAlterDistributedCounter(String id, long value) {
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			return distributedCounterManager.asyncGetAndAlterCounter(id,value);
		} else {
			// Atomically alter counter and return old value (before the alter).
			long[] oldValue = {0L};
			counters.compute(id, (k, v) -> {
				oldValue[0] = (v == null ? 0L : v);
				return oldValue[0] + value;
			});
			return oldValue[0];
		}
	}

	/**
	 * Destroy hazelcast global counter, if it's local then remove the map entry
	 *
	 * @param id of the caller context
	 */
	public static void removeCounter(String id) {
		if(log.isDebugEnabled()) {
			log.debug("REMOVING COUNTER WITH ID " + id);
		}
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			distributedCounterManager.removeCounter(id);
		} else {
			counters.remove(id);
		}
	}

	/**
	 * Return hazelcast shared timestamp for this caller context with given id. If it's not distributed will get from the
	 * local counter
	 *
	 * @param id of the shared counter
	 * @return shared hazelcast current shared counter
	 */
	public static long getSharedTimestamp(String id) {
		if(log.isDebugEnabled()) {
			log.debug("GET TIMESTAMP WITH ID " + id);
		}
		String key = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;

		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			return distributedCounterManager.getTimestamp(key);
		} else {
			Long timestamp = timestamps.get(key);
			if(timestamp != null) {
				return timestamp;
			} else {
				timestamps.put(key, 0L);
				return 0;
			}
		}
	}


	/**
	 * Set distribute timestamp of caller context of given id to the provided value. If it's not distributed do the same for
	 * local counter
	 *
	 * @param id        of the caller context
	 * @param timestamp to set to the global counter
	 */
	public static void setSharedTimestamp(String id, long timestamp) {
		if(log.isDebugEnabled()) {
			log.debug("SETTING TIMESTAMP WITH ID" + id);
		}
		String key = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			distributedCounterManager.setTimestamp(key, timestamp);
		} else {
			timestamps.put(id, timestamp);
		}
	}

	/**
	 * Destroy hazelcast shared timggestamp counter, if it's local then remove the map entry
	 *
	 * @param id of the caller context
	 */
	public static void removeTimestamp(String id) {
		if(log.isDebugEnabled()) {
			log.debug("REMOVING TIMESTAMP WITH ID " + id);
		}
		String key = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			distributedCounterManager.removeTimestamp(key);
		} else {
			timestamps.remove(key);
		}
	}



	public static void setExpiryTime(String id, long expiryTimeStamp) {
		if(log.isDebugEnabled()) {
			log.debug("SETTING Expiry WITH ID " + id);
		}
		String sharedCounterKey = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		String sharedTimeStampKey = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;

		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			if (log.isTraceEnabled()) {
				log.trace("Setting expiry time for key:" + sharedCounterKey + " value: " + expiryTimeStamp);
			}
			distributedCounterManager.setExpiry(sharedCounterKey, expiryTimeStamp);
			if (log.isTraceEnabled()) {
				log.trace("Setting expiry time for key:" + sharedTimeStampKey + " value: " + expiryTimeStamp);
			}
			distributedCounterManager.setExpiry(sharedTimeStampKey, expiryTimeStamp);

		}

	}

	/**
	 * Get the time-to-live value for the given key
	 *
	 * @param key name key of the key
	 * @return time-to-live value
	 */
	public static long getTtl(String key) {
		long ttl = 0;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			ttl = distributedCounterManager.getTtl(key);
		}
		return ttl;
	}

	/**
	 * Acquire lock for the given callerContext (with the given value), so that another process cannot acquire the same
	 * lock
	 *
	 * @return true if lock acquired, false if lock is not acquired within the configured timeout period
	 */
	public static boolean lockSharedKeys(String callerContextId, String lockValue) {
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();

		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			boolean lockAcquired;
			// key of the lock tried to acquire. i.e. "lock-/pizzashack/1.0.0:1.0.0:PRODUCTION"
			String lockKey = ThrottleConstants.THROTTLE_LOCK_KEY_PREFIX + callerContextId;
			long startTime = System.currentTimeMillis();
			do {
				lockAcquired = distributedCounterManager.setLockWithExpiry(lockKey, lockValue, System.currentTimeMillis() +
					                                                 distributedCounterManager.getKeyLockRetrievalTimeout() * 2);

				if (lockAcquired) {
					// lock acquired
					if (log.isTraceEnabled()) {
						long timeNow = System.currentTimeMillis();
						log.trace(
								"current time:" + timeNow + "Lock acquired for key: " + lockKey + " within " + (timeNow
										- startTime) + " ms");
					}
					return true;
				} else {
					long time = System.currentTimeMillis();
					long timeElapsed = time - startTime;
					if (timeElapsed > distributedCounterManager.getKeyLockRetrievalTimeout()) {
						log.warn("current time:" + time + " Unable to" + " acquire lock for key: " + lockKey
								+ " within the configured " + "timeout period. Elapsed time: " + timeElapsed + " ms");
						return false;
					}

					try {
						Thread.sleep(5);
						if (log.isTraceEnabled()) {
							log.trace("current time:" + time + "Retrying to get lock for key: " + lockKey);
						}
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			} while (true);
		}
		return true;
	}

	/**
	 * Release the lock of the given callerContext
	 */
	public static void releaseSharedKeys(String callerContextId) {
		DistributedCounterManager distributedCounterManager = ThrottleServiceDataHolder.getInstance()
				.getDistributedCounterManager();

		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			String lockKey = ThrottleConstants.THROTTLE_LOCK_KEY_PREFIX + callerContextId;
			distributedCounterManager.removeLock(lockKey);
			if (log.isTraceEnabled()) {
				log.trace("Current time:" + System.currentTimeMillis() + "Lock released for key: " + lockKey);
			}
		}
	}
	/**
	 * Atomically reads the window timestamp and counter for the given caller id.
	 *
	 * @param id caller context id
	 * @return long[2]: {timestamp, counter}
	 */
	public static long[] getWindowState(String id) {
		String key = ThrottleConstants.THROTTLE_WINDOW_HASH_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			return distributedCounterManager.getWindowState(key);
		} else {
			String tsKey      = ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id;
			String counterKey = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
			Long ts      = timestamps.get(tsKey);
			Long counter = counters.get(counterKey);
			return new long[]{
					ts      != null ? ts      : 0L,
					counter != null ? counter : 0L
			};
		}
	}

	/**
	 * Unconditionally sets a throttle window by overwriting both timestamp and counter, then
	 * refreshing the TTL. Must be called while holding the per-caller window lock.
	 *
	 * @param id         caller context id
	 * @param count      initial counter value
	 * @param ts         window first-access time in milliseconds
	 * @param expiryTime absolute expiry timestamp in milliseconds (ts + unitTime)
	 */
	public static void setWindow(String id, long count, long ts, long expiryTime) {
		String key = ThrottleConstants.THROTTLE_WINDOW_HASH_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			distributedCounterManager.setWindow(key, count, ts, expiryTime);
		} else {
			timestamps.put(ThrottleConstants.THROTTLE_TIMESTAMP_KEY + id, ts);
			counters.put(ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id, count);
		}
	}

	/**
	 * Atomically increments the window counter by {@code delta}.
	 *
	 * @param id    caller context id
	 * @param delta value to add to the counter
	 * @return new global counter value after the increment
	 */
	public static long incrWindowCounter(String id, long delta, long expiryTime) {
		String key = ThrottleConstants.THROTTLE_WINDOW_HASH_KEY + id;
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			return distributedCounterManager.incrWindowCounter(key, delta, expiryTime);
		} else {
			String counterKey = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
			long[] result = {0L};
			counters.compute(counterKey, (k, v) -> {
				result[0] = ((v != null) ? v : 0L) + delta;
				return result[0];
			});
			return result[0];
		}
	}

	/**
	 * Tries to acquire the per-caller window lock without blocking.
	 * Returns {@code true} if the lock was acquired, {@code false} if another node holds it.
	 * The lock TTL is set to {@code expiryTime} (absolute window boundary) so it auto-expires
	 * when the window ends, even if the holder crashes mid-tick.
	 *
	 * @param id         caller context id
	 * @param expiryTime absolute window-end timestamp in ms (localFAT + unitTime)
	 * @return true if this node acquired the lock
	 */
	public static boolean tryWindowLock(String id, long expiryTime) {
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			String lockKey = ThrottleConstants.THROTTLE_WINDOW_LOCK_KEY + id;
			return distributedCounterManager.setLockWithExpiry(lockKey, "1", expiryTime);
		}
		return true;  // Single-JVM: no cluster contention, always proceed.
	}

	/**
	 * Releases the per-caller window lock acquired by {@link #tryWindowLock}.
	 * Should be called in a {@code finally} block after lock-protected Redis work completes.
	 *
	 * @param id caller context id
	 */
	public static void releaseWindowLock(String id) {
		DistributedCounterManager distributedCounterManager =
				ThrottleServiceDataHolder.getInstance().getDistributedCounterManager();
		if (distributedCounterManager != null && distributedCounterManager.isEnable()) {
			String lockKey = ThrottleConstants.THROTTLE_WINDOW_LOCK_KEY + id;
			distributedCounterManager.removeLock(lockKey);
		}
	}
}
