package org.apache.synapse.commons.throttle.core;

import com.hazelcast.core.AsyncAtomicLong;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IFunction;
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
	 * Return hazelcast shared counter for this caller context with given id. If it's not distributed will get from the
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
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			return hazelcastInstance.getAtomicLong(id).get();
		} else {
			Long counter = counters.get(id);
			if(counter != null) {
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
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			hazelcastInstance.getAtomicLong(id).set(value);
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
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		if(hazelcastInstance != null) {
			return hazelcastInstance.getAtomicLong(id).addAndGet(value);
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
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			AsyncAtomicLong asyncAtomicLong = (AsyncAtomicLong) hazelcastInstance.getAtomicLong(id);
			long currentGlobalCounter = asyncAtomicLong.get();
			asyncAtomicLong.asyncAddAndGet(value);
			return currentGlobalCounter;
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
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		id = ThrottleConstants.THROTTLE_SHARED_COUNTER_KEY + id;
		if(hazelcastInstance != null) {
			AsyncAtomicLong asyncAtomicLong = (AsyncAtomicLong) hazelcastInstance.getAtomicLong(id);
			long currentGlobalCounter = asyncAtomicLong.get();
			asyncAtomicLong.asyncAlter(new AddLocalCount(value));
			return currentGlobalCounter;
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
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			hazelcastInstance.getAtomicLong(id).destroy();
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
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			return hazelcastInstance.getAtomicLong(key).get();
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
	 * Return hazelcast shared timestamp for this caller context with given full id. If it's not distributed will get from the
	 * local counter
	 *
	 * @param id of the shared counter
	 * @return shared hazelcast current shared counter
	 */
	public static long getSharedTimestampWithFullId(String id) {
		if(log.isDebugEnabled()) {
			log.info("GET TIMESTAMP WITH FULL ID " + id);
		}
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			return hazelcastInstance.getAtomicLong(id).get();
		} else {
			Long timestamp = timestamps.get(id);
			if(timestamp != null) {
				return timestamp;
			} else {
				timestamps.put(id, 0L);
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
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			hazelcastInstance.getAtomicLong(key).set(timestamp);
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
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			hazelcastInstance.getAtomicLong(key).destroy();
		} else {
			timestamps.remove(key);
		}
	}

	/**
	 * Destroy hazelcast shared timggestamp counter with full id, if it's local then remove the map entry
	 *
	 * @param id of the caller context
	 */
	public static void removeTimestampWithFullId(String id) {
		if(log.isDebugEnabled()) {
			log.info("REMOVING TIMESTAMP WITH FULL ID " + id);
		}
		HazelcastInstance hazelcastInstance = getHazelcastInstance();
		if(hazelcastInstance != null) {
			hazelcastInstance.getAtomicLong(id).destroy();
		} else {
			timestamps.remove(id);
		}
	}

	private static HazelcastInstance getHazelcastInstance() {
		return ThrottleServiceDataHolder.getInstance().getHazelCastInstance();
	}

	/**
	* This class is used for asynchronously update the value of distributed counter which is reside in the particular
	* partition.
	*/
	private static class AddLocalCount implements IFunction<Long, Long> {

		private long localCount;

		public AddLocalCount(long localCount) {
			this.localCount = localCount;
		}

		public Long apply( Long input ) {
			return input + localCount;
		}
	}
}
