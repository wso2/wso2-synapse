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
	 * @param id of the caller context
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
			distributedCounterManager.setExpiry(sharedCounterKey, expiryTimeStamp);
			distributedCounterManager.setExpiry(sharedTimeStampKey, expiryTimeStamp);

		}

	}

}
