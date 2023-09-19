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
			long currentCount = counters.get(id);
			long updatedCount = currentCount + value;
			counters.put(id, updatedCount);
			return updatedCount;
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
			Long currentCount = counters.get(id);
			if(currentCount == null) {
				currentCount = 0L;
			}
			long updatedCount = currentCount + value;
			counters.put(id, updatedCount);
			return currentCount;
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
			Long currentCount = counters.get(id);
			if(currentCount == null) {
				currentCount = 0L;
			}
			long updatedCount = currentCount + value;
			counters.put(id, updatedCount);
			return currentCount;
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
			distributedCounterManager.removeLock(callerContextId);
			if (log.isTraceEnabled()) {
				log.trace("Current time:" + System.currentTimeMillis() + "Lock released for key: " + callerContextId);
			}
		}
	}
}
