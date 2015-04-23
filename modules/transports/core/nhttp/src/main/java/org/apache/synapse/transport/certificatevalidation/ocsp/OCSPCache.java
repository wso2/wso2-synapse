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
package org.apache.synapse.transport.certificatevalidation.ocsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.jmx.MBeanRegistrar;
import org.apache.synapse.transport.certificatevalidation.CertificateVerificationException;
import org.apache.synapse.transport.certificatevalidation.cache.CacheController;
import org.apache.synapse.transport.certificatevalidation.cache.CacheManager;
import org.apache.synapse.transport.certificatevalidation.cache.ManageableCache;
import org.apache.synapse.transport.certificatevalidation.cache.ManageableCacheValue;
import org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle.cert.ocsp.*;

import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a cache to store OSCP responses against Certificate Serial Number since an OCSP response depends on
 * the certificate. This is a singleton since more than one cache of this kind should not be allowed. This cache
 * can be shared by many transports which need SSL validation through OCSP.
 */
public class OCSPCache implements ManageableCache {

    private static volatile OCSPCache cache;
    private static volatile Map<BigInteger,OCSPCacheValue> hashMap = new ConcurrentHashMap<BigInteger, OCSPCacheValue>();
    private static volatile Iterator<Map.Entry<BigInteger,OCSPCacheValue>> iterator = hashMap.entrySet().iterator();
    private static volatile CacheManager cacheManager;
    private static OCSPVerifier ocspVerifier = new OCSPVerifier(null);
    private static final Log log = LogFactory.getLog(OCSPCache.class);

    private OCSPCache() {}

    public static OCSPCache getCache() {
        //Double checked locking
        if (cache == null) {
            synchronized (OCSPCache.class) {
                if (cache == null)
                    cache = new OCSPCache();
            }
        }
        return cache;
    }

    /**
     * This lazy initializes the Cache with a CacheManager. If this method is not called, a cache manager will not be used.
     * @param size max size of the cache
     * @param delay defines how frequently the CacheManager will be started
     */
    public void init(int size, int delay) {
        if (cacheManager == null) {
            synchronized (OCSPCache.class) {
                if (cacheManager == null) {
                    cacheManager = new CacheManager(cache, size, delay);
                    CacheController mbean = new CacheController(cache,cacheManager);
                    MBeanRegistrar.getInstance().registerMBean(mbean, "CacheController", "OCSPCacheController");
                }
            }
        }
    }

    /**
     * This method is needed by the cache Manager to go through the cache entries to remove invalid values or
     * to remove LRU cache values if the cache has reached its max size.
     * Todo: Can move to an abstract class.
     * @return next cache value of the cache.
     */
    public ManageableCacheValue getNextCacheValue() {
        //Changes to the hash map are reflected on the keySet. And its iterator is weakly consistent. so will never
        //throw concurrent modification exception.
        if (iterator.hasNext())
            return hashMap.get(iterator.next().getKey());
        else {
            resetIterator();
            return null;
        }
    }

    /**
     * @return the current cache size (size of the hash map)
     */
    public int getCacheSize() {
        return hashMap.size();
    }

    public void resetIterator(){
        iterator = hashMap.entrySet().iterator();
    }

    //This has to be synchronized coz several threads will try to replace cache value (cacheManager and Reactor thread)
    private synchronized void replaceNewCacheValue(OCSPCacheValue cacheValue){
        //If someone has updated with the new value before current Thread.
        if(cacheValue.isValid())
            return;

        try {
            String serviceUrl = cacheValue.serviceUrl;
            OCSPReq request = cacheValue.request;
            OCSPResp response= ocspVerifier.getOCSPResponce(serviceUrl, request);

            if (OCSPResponseStatus.SUCCESSFUL != response.getStatus())
                throw new CertificateVerificationException("OCSP response status not SUCCESSFUL");

            BasicOCSPResp basicResponse = (BasicOCSPResp) response.getResponseObject();
            SingleResp[] responses = (basicResponse == null) ? null : basicResponse.getResponses();

            if (responses == null)
                throw new CertificateVerificationException("Cant get OCSP response");

            SingleResp resp = responses[0];
            this.setCacheValue(cacheValue.serialNumber, resp, request, serviceUrl);

        } catch (Exception e){
            log.info("Cant replace old CacheValue with new CacheValue. So remove", e);
            //If cant be replaced remove.
            cacheValue.removeThisCacheValue();
        }
    }

    public synchronized SingleResp getCacheValue(BigInteger serialNumber) {
        OCSPCacheValue cacheValue = hashMap.get(serialNumber);
        if(cacheValue != null) {
            //If who ever gets this cache value before Cache manager task found its invalid, update it and get the
            // new value.
            if (!cacheValue.isValid()) {
                cacheValue.updateCacheWithNewValue();
                OCSPCacheValue ocspCacheValue = hashMap.get(serialNumber);
                return (ocspCacheValue!=null? ocspCacheValue.getValue(): null);
            }

            return cacheValue.getValue();
        }
        else
            return null;
    }

    public synchronized void setCacheValue(BigInteger serialNumber, SingleResp singleResp, OCSPReq request, String serviceUrl) {
        OCSPCacheValue cacheValue = new OCSPCacheValue(serialNumber, singleResp, request, serviceUrl);
        log.info("Before set - HashMap size " + hashMap.size());
        hashMap.put(serialNumber, cacheValue);
        log.info("After set - HashMap size " + hashMap.size());
    }

    public synchronized void removeCacheValue(BigInteger serialNumber) {
        log.info("Before remove - HashMap size " + hashMap.size());
        hashMap.remove(serialNumber);
        log.info("After remove - HashMap size " + hashMap.size());
    }

    /**
     * This is the wrapper class of the actual cache value which is a SingleResp.
     */
    private class OCSPCacheValue implements ManageableCacheValue {

        private BigInteger serialNumber;
        private SingleResp singleResp;
        private OCSPReq request;
        private String serviceUrl;
        private long timeStamp = System.currentTimeMillis();

        public OCSPCacheValue(BigInteger serialNumber, SingleResp singleResp, OCSPReq request, String serviceUrl) {
            this.serialNumber = serialNumber;
            this.singleResp = singleResp;
            //request and serviceUrl are needed to update the cache with new values.
            this.request = request;
            this.serviceUrl = serviceUrl;
        }

        public BigInteger getKey() {
            return serialNumber;
        }

        public SingleResp getValue() {
            timeStamp = System.currentTimeMillis();
            return singleResp;
        }

        /**
         * An OCSP response is valid during its validity period.
         */
        public boolean isValid() {
            //todo: elapse
            Date now = new Date();
            Date nextUpdate = singleResp.getNextUpdate();
            return nextUpdate != null && nextUpdate.after(now);
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
            replaceNewCacheValue(this);
        }
    }
}
