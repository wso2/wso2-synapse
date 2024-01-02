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
     * This method used to update distributed counter asynchronously.
     *
     * @param key   key to check in distributed map.
     * @param value value to add to distributed counter.
     * @return the original distributed counter value.
     */
    public long asyncGetAndAddCounter(String key, long value);

    /**
     * This method used to alter the DistributedCounter.
     *
     * @param key   key to check in distributed map.
     * @param value value of distributed counter.
     * @return the original distributed counter value.
     */
    public long asyncGetAndAlterCounter(String key, long value);

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
     * This method removes the timestamp relevant to key.
     *
     * @param key key to check in distributed map.
     */
    public void removeTimestamp(String key);

    public boolean isEnable();

    public String getType();

    void setExpiry(String key, long expiryTimeStamp);
}
