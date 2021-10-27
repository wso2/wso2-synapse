/*
 * Copyright WSO2, Inc. http://wso2.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package org.apache.synapse.commons.throttle.core;

import com.hazelcast.core.AsyncAtomicLong;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IFunction;
import org.apache.synapse.commons.throttle.core.internal.ThrottleServiceDataHolder;

/**
 * Distributed Counter implementation for HazelCast.
 */
public class HazelcastDistributedCounterManager implements DistributedCounterManager {

    @Override
    public long getCounter(String key) {

        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        return hazelcastInstance.getAtomicLong(key).get();
    }

    @Override
    public void setCounter(String key, long value) {

        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        hazelcastInstance.getAtomicLong(key).set(value);
    }

    @Override
    public long addAndGetCounter(String key, long value) {

        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        return hazelcastInstance.getAtomicLong(key).addAndGet(value);
    }

    @Override
    public void removeCounter(String key) {

        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        hazelcastInstance.getAtomicLong(key).destroy();
    }

    @Override
    public long asyncGetAndAddCounter(String key, long value) {

        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        AsyncAtomicLong asyncAtomicLong = (AsyncAtomicLong) hazelcastInstance.getAtomicLong(key);
        long currentGlobalCounter = asyncAtomicLong.get();
        asyncAtomicLong.asyncAddAndGet(value);
        return currentGlobalCounter;
    }

    @Override
    public long asyncGetAndAlterCounter(String key, long value) {

        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        AsyncAtomicLong asyncAtomicLong = (AsyncAtomicLong) hazelcastInstance.getAtomicLong(key);
        long currentGlobalCounter = asyncAtomicLong.get();
        asyncAtomicLong.asyncAlter(new HazelcastDistributedCounterManager.AddLocalCount(value));
        return currentGlobalCounter;
    }

    @Override
    public long getTimestamp(String key) {

        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        return hazelcastInstance.getAtomicLong(key).get();
    }

    @Override
    public void setTimestamp(String key, long timeStamp) {

        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        hazelcastInstance.getAtomicLong(key).set(timeStamp);
    }

    @Override
    public void removeTimestamp(String key) {

        HazelcastInstance hazelcastInstance = getHazelcastInstance();
        hazelcastInstance.getAtomicLong(key).destroy();
    }

    @Override
    public boolean isEnable() {

        return getHazelcastInstance() != null;
    }

    @Override
    public String getType() {

        return ThrottleConstants.HAZELCAST;
    }

    @Override
    public void setExpiry(String key, long expiryTimeStamp) {

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
