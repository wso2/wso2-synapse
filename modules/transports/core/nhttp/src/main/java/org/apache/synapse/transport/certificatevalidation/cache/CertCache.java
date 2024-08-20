/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.transport.certificatevalidation.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.certificatevalidation.Constants;

import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a cache to store a certificate against a unique string (can be a serial number). This is a singleton since
 * more than one cache of this kind should not be allowed. This cache can be used by any place where certificate
 * caching is needed.
 */
public class CertCache implements ManageableCache {

    private static volatile CertCache cache;
    private static volatile Map<String, CertCacheValue> hashMap = new ConcurrentHashMap<String, CertCacheValue>();
    private static volatile Iterator<Map.Entry<String, CertCacheValue>> iterator = hashMap.entrySet().iterator();
    private static volatile CacheManager cacheManager;
    private static final Log log = LogFactory.getLog(CertCache.class);

    private CertCache() {
    }

    public static CertCache getCache() {
        //Double-checked locking
        if (cache == null) {
            synchronized (CertCache.class) {
                if (cache == null) {
                    cache = new CertCache();
                    cacheManager = new CacheManager(cache, Constants.CACHE_DEFAULT_ALLOCATED_SIZE,
                            Constants.CACHE_DEFAULT_DELAY_MINS);
                }
            }
        }
        return cache;
    }

    public synchronized X509Certificate getCacheValue(String serialNumber) {
        CertCacheValue cacheValue = hashMap.get(serialNumber);
        if (cacheValue != null) {
            return cacheValue.getValue();
        } else
            return null;
    }

    @Override
    public ManageableCacheValue getNextCacheValue() {

        if (iterator.hasNext()) {
            return hashMap.get(iterator.next().getKey());
        } else {
            resetIterator();
            return null;
        }
    }

    @Override
    public int getCacheSize() {

        return hashMap.size();
    }

    @Override
    public void resetIterator() {

        iterator = hashMap.entrySet().iterator();
    }

    public static void resetCache() {

        hashMap.clear();
    }

    public synchronized void setCacheValue(String serialNumber, X509Certificate cert) {
        CertCacheValue cacheValue = new CertCacheValue(serialNumber, cert);

        if (log.isDebugEnabled()) {
            log.debug("Before set - HashMap size " + hashMap.size());
        }
        hashMap.put(serialNumber, cacheValue);
        if (log.isDebugEnabled()) {
            log.debug("After set - HashMap size " + hashMap.size());
        }
    }

    public synchronized void removeCacheValue(String serialNumber) {

        if (log.isDebugEnabled()) {
            log.debug("Before remove - HashMap size " + hashMap.size());
        }
        hashMap.remove(serialNumber);
        if (log.isDebugEnabled()) {
            log.debug("After remove - HashMap size " + hashMap.size());
        }
    }

    /**
     * This is the wrapper class of the actual cache value.
     */
    private class CertCacheValue implements ManageableCacheValue {

        private final String serialNumber;
        private final X509Certificate issuerCertificate;
        private final long timeStamp = System.currentTimeMillis();

        public CertCacheValue(String serialNumber, X509Certificate issuerCertificate) {

            this.serialNumber = serialNumber;
            this.issuerCertificate = issuerCertificate;
        }

        public X509Certificate getValue() {

            return issuerCertificate;
        }

        public String getKey() {

            return serialNumber;
        }

        public boolean isValid() {

            // Will be always return true since we only set defined data.
            return true;
        }

        public long getTimeStamp() {

            return timeStamp;
        }

        /**
         * Used by cacheManager to remove invalid entries.
         */
        public void removeThisCacheValue() {

            removeCacheValue(serialNumber);
        }

        public void updateCacheWithNewValue() {
            // No implementation needed since there are no scenarios of the cache value being invalid.
        }
    }
}
