/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.commons.throttle.core;

/**
 * This interface used to replicate throttling counters and windows in distributed manner.
 */
public interface DistributedCounterManager {

    /**
     * Returns the Distributed Counter for specific key.
     *
     * @param key key to check in distributed map.
     * @return value of distributed counter.
     */
    public long getCounter(String key);

    /**
     * Sets the Distributed counter with value.
     *
     * @param key   key to add in distributed map.
     * @param value value of distributed counter.
     */
    public void setCounter(String key, long value);

    /**
     * Sets the Distributed counter with the given value while setting expiry time too.
     *
     * @param key        counter key name
     * @param value      counter value
     * @param expiryTime expiry time in milliseconds
     */
    public void setCounterWithExpiry(String key, long value, long expiryTime);
    /**
     * This method used to add and return the distributed counter value.
     *
     * @param key   key to add in distributed map.
     * @param value value to add to distributed counter.
     * @return added value of distributed counter.
     */
    public long addAndGetCounter(String key, long value);

    /**
     * This method used to remove specified key.
     *
     * @param key key to check in distributed map.
     */
    public void removeCounter(String key);

    /**
     * This method is used to get and then increment distributed counter asynchronously.
     *
     * @param key   key to check in distributed map.
     * @param value value to add to distributed counter.
     * @return the original distributed counter value.
     */
    public long asyncGetAndAddCounter(String key, long value);

    /**
     * This method is used to increment distributed counter asynchronously.
     *
     * @param key   key to update in distributed map.
     * @param value value to increment
     * @return the updated distributed counter value.
     */
    public long asyncAddCounter(String key, long value);

    /**
     * This method used to alter the DistributedCounter.
     *
     * @param key   key to check in distributed map.
     * @param value value of distributed counter.
     * @return the original distributed counter value.
     */
    public long asyncGetAndAlterCounter(String key, long value);

    /**
     * This method is used to get and then alter and then set expiry time of the DistributedCounter.
     *
     * @param key             key to alter in distributed counter.
     * @param value           value to alter in distributed counter.
     * @param expiryTimeStamp expiry time to set.
     * @return the original distributed counter value.
     */
    public long asyncGetAlterAndSetExpiryOfCounter(String key, long value, long expiryTimeStamp);

        /**
         * This method returns shared TimeStamp of distributed Key.
         *
         * @param key key to check in distributed map.
         * @return timestamp value of key.
         */
    public long getTimestamp(String key);

    /**
     * This method set the Timestamp to distributed map.
     *
     * @param key       key to add in distributed map.
     * @param timeStamp timestamp to add.
     */
    public void setTimestamp(String key, long timeStamp);

    /**
     * This method set the Timestamp to distributed map with an expiry time.
     *
     * @param key             key to add in distributed map.
     * @param timeStamp       timestamp to add.
     * @param expiryTimeStamp expiry timestamp to set
     */
    public void setTimestampWithExpiry(String key, long timeStamp, long expiryTimeStamp);

    /**
     * This method removes the timestamp relevant to key.
     *
     * @param key key to check in distributed map.
     */
    public void removeTimestamp(String key);

    public boolean isEnable();

    public String getType();

    void setExpiry(String key, long expiryTimeStamp);

    public long getTtl(String key);

    public long setLock(String key, String value);

    public boolean setLockWithExpiry(String key, String value, long expiryTimeStamp);

    public long getKeyLockRetrievalTimeout();

    public void removeLock(String key);
}
