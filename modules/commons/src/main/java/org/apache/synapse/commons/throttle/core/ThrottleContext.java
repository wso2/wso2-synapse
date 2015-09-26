/*
* Copyright 2005,2006 WSO2, Inc. http://wso2.com
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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Holds the all runtime data corresponding to call remote callers.
 * In addition to that this hold clean list for callers.
 */

public abstract class ThrottleContext {

    private static Log log = LogFactory.getLog(ThrottleContext.class.getName());

    /* The callersMap that contains all registered callers for a particular throttle */
    private Map callersMap;
    /* For mapping id (ip | domainame) to TimeStamp */
    private Map keyToTimeStampMap;
    /* The Time which next cleaning for this throttle will have to take place */
    private long nextCleanTime;
    /* The configuration of a throttle */
    private ThrottleConfiguration throttleConfiguration;
    /* The configuration that corresponding to this context – this holds all
     static (configuration) data */
    private String throttleId;
    /* Configuration Context for the current environment */
    private ConfigurationContext configctx;
    /* ThrottleDataHolder is used to keep the CallerContexts pertaining to a Throttle Context.*/
    private volatile ThrottleDataHolder dataHolder;
    /* The pre-fix of key for any caller */
    private String keyPrefix;
    /*is log level has set to debug */
    private boolean debugOn;

    /* Throttle replicating to replicate Throttle data */
    private ThrottleReplicator throttleReplicator;

    /**
     * default constructor – expects a throttle configuration.
     *
     * @param throttleConfiguration - configuration data according to the policy
     */
    public ThrottleContext(ThrottleConfiguration throttleConfiguration,
                           ThrottleReplicator throttleReplicator) {
        if (throttleConfiguration == null) {
            throw new InstantiationError("Couldn't create the throttle context " +
                    "from null a throttle configuration");
        }
        this.throttleReplicator = throttleReplicator;
        this.keyToTimeStampMap = new ConcurrentHashMap();
        this.callersMap = new ConcurrentSkipListMap();
        this.nextCleanTime = 0;
        this.throttleConfiguration = throttleConfiguration;
        this.debugOn = log.isDebugEnabled();
    }

    /**
     * To get the ThrottleConfiguration
     *
     * @return ThrottleConfiguration returns the ThrottleConfiguration of this context
     */
    public ThrottleConfiguration getThrottleConfiguration() {
        return throttleConfiguration;
    }

    /**
     * To get the runtime states of a remote caller
     *
     * @param id the remote caller id ex: domain , ip
     * @return Returns the CallerContext which holds runtime state of a remote caller
     */
    public CallerContext getCallerContext(String id) {

        if (id != null) {

            if (debugOn) {
                log.debug("Found a configuration with id :" + id);
            }
            // for cluster env , caller state is contained in the axis configuration context
            if (dataHolder != null && keyPrefix != null) {
                log.debug("CallerContext fetched from dataHolder");
                CallerContext cc = dataHolder.getCallerContext(keyPrefix + id);
                if (cc == null) {
                    log.debug("CallerContext null in dataHolder");
                }
                return cc;
            }
            // for non - clustered  env
            Long timeKey = (Long) keyToTimeStampMap.get(id);
            if (timeKey != null) {
                Object co = callersMap.get(timeKey);
                if (co != null) {
                    if (co instanceof CallerContext) {
                        return (CallerContext) co;
                    } else if (co instanceof LinkedList) {    // callers with same time window
                        LinkedList callers = (LinkedList) co;
                        for (Iterator it = callers.iterator(); it.hasNext(); ) {
                            CallerContext cc = (CallerContext) it.next();
                            if (cc != null && id.equals(cc.getID())) {
                                log.debug("CallerContext found from keyToTimeStampMap");
                                return cc;
                            }
                        }
                    }
                }
            }
        } else {
            if (debugOn) {
                log.debug("Couldn't find a configuration for the remote caller : " + id);
            }
        }
        return null;
    }

    /**
     * setting callerContext - put callersMap against  time and
     * put time against remote caller id (ip/domain)
     *
     * @param callerContext - The remote caller's runtime data.
     * @param id            - The id of the remote caller
     */
    public CallerContext addCallerContext(CallerContext callerContext, String id) {
        if (callerContext != null && id != null) {
            return addCaller(callerContext, id);
        }
        return callerContext;
    }

    /**
     * Helper method to add a caller context
     *
     * @param callerContext The CallerContext
     * @param id            The id of the remote caller
     */
    private CallerContext addCaller(CallerContext callerContext, String id) {

        if (debugOn) {
            log.debug("Setting the caller with an id " + id);
        }
        /*
        Put the CallerContext only if there's no values previously associated with it.
        If a CallerContext already exists re-use it rather than create a new one. If a new one
        is put without checking the existence, Throttle values can get erased.
         */
        if (dataHolder != null && keyPrefix != null) {
            CallerContext previous =
                    dataHolder.addCallerContextIfAbsent(keyPrefix + id, callerContext);
            if (previous != null) {
                callerContext = previous;
            }
        }
        // for clean up list
        Long time = new Long(callerContext.getNextTimeWindow());
        if (!callersMap.containsKey(time)) {
            callersMap.put(time, callerContext);
        } else {
            //if there are callersMap with same timewindow ,then use linkedList to hold those
            Object callerObject = callersMap.get(time);
            if (callerObject != null) {
                if (callerObject instanceof CallerContext) {
                    LinkedList callersWithSameTimeStamp = new LinkedList();
                    callersWithSameTimeStamp.add(callerObject);
                    callersWithSameTimeStamp.add(callerContext);
                    callersMap.remove(time);
                    callersMap.put(time, callersWithSameTimeStamp);
                } else if (callerObject instanceof LinkedList) {
                    LinkedList callersWithSameTimeStamp = (LinkedList) callerObject;
                    callersWithSameTimeStamp.add(callerContext);
                }
            }
        }
        //set Time Vs key
        keyToTimeStampMap.put(id, time);
        return callerContext;
    }

    /**
     * removing a caller with a given id - caller will remove from clean list
     *
     * @param id Caller ID
     */
    public void removeCallerContext(String id) {
        if (id != null) {
            removeCaller(id);
        }
    }

    /**
     * Helper method to remove a caller
     *
     * @param id The id of the caller
     */
    private void removeCaller(String id) {
        Long time = (Long) keyToTimeStampMap.get(id);
        // if (time != null) {
        if (dataHolder != null && keyPrefix != null) {
            log.debug("Removing the caller with the configuration id " + id);
            dataHolder.removeCaller(keyPrefix + id);
        }
        if (time != null) {
            callersMap.remove(time);
            keyToTimeStampMap.remove(id);
        }
    }

    /**
     * /**
     * processing cleaning list- only process callerContexts which unit time already had over
     *
     * @param time - the current System Time
     * @throws ThrottleException
     */

    public void processCleanList(long time) throws ThrottleException {
        if (debugOn) {
            log.debug("Cleaning up process is executing");
        }
        if (time > nextCleanTime) {
            SortedMap map = ((ConcurrentNavigableMap) callersMap).headMap(new Long(time));
            if (map != null && map.size() > 0) {
                for (Iterator it = map.values().iterator(); it.hasNext(); ) {
                    Object o = it.next();
                    if (o != null) {
                        if (o instanceof CallerContext) { // In the case nextAccessTime is unique
                            CallerContext c = ((CallerContext) o);
                            String key = c.getID();
                            if (key != null) {
                                if (dataHolder != null && keyPrefix != null) {
                                    c = dataHolder.getCallerContext(keyPrefix + key);
                                }
                                if (c != null) {
                                    c.cleanUpCallers(
                                            this.throttleConfiguration.getCallerConfiguration(key)
                                            , this
                                            , time);
                                }
                            }
                        }
                        if (o instanceof LinkedList) {
                            //In the case nextAccessTime of callers are same
                            LinkedList callers = (LinkedList) o;
                            for (Iterator ite = callers.iterator(); ite.hasNext(); ) {
                                CallerContext c = (CallerContext) ite.next();
                                String key = c.getID();
                                if (key != null) {
                                    if (dataHolder != null && keyPrefix != null) {
                                        c = dataHolder.getCallerContext(keyPrefix + key);
                                    }
                                    if (c != null) {
                                        c.cleanUpCallers(
                                             this.throttleConfiguration.getCallerConfiguration(key)
                                             , this, time);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            nextCleanTime = time + ThrottleConstants.DEFAULT_THROTTLE_CLEAN_PERIOD;
        }
    }

    public void setThrottleId(String throttleId) {
        if (throttleId == null) {
            throw new IllegalArgumentException("The throttle id cannot be null");
        }
        this.throttleId = throttleId;
        this.keyPrefix = ThrottleConstants.THROTTLE_PROPERTY_PREFIX + throttleId;
    }

    public String getThrottleId() {
        return this.throttleId;
    }

    public ConfigurationContext getConfigurationContext() {
        return this.configctx;
    }

    public void setConfigurationContext(ConfigurationContext configurationContext) {
        this.configctx = configurationContext;
        if (dataHolder == null) {
            initDataHolder();
        }
    }

    private void initDataHolder() {
        if (configctx != null) {
            if (dataHolder == null) {
                dataHolder = (ThrottleDataHolder) configctx
                        .getPropertyNonReplicable(ThrottleConstants.THROTTLE_INFO_KEY);
                synchronized (configctx) {
                    if (dataHolder == null) {
                        dataHolder = new ThrottleDataHolder();
                        configctx.setNonReplicableProperty
                                (ThrottleConstants.THROTTLE_INFO_KEY, dataHolder);
                    }
                }
            }
        }
    }

    /**
     * @return Returns the type of throttle ex : ip /domain
     */
    public abstract int getType();

    /**
     * To add the caller and replicates the states of the given caller
     *
     * @param callerContext The states of the caller
     * @param id            The id of the caller
     */
    public void addAndFlushCallerContext(CallerContext callerContext, String id) {
        if (callerContext != null && id != null) {
            //addCaller(callerContext, id);
            replicateCaller(id);
        }
    }

    /**
     * To replicates the states of the already exist caller
     *
     * @param callerContext The states of the caller
     * @param id            The id of the remote caller
     */
    public void flushCallerContext(CallerContext callerContext, String id) {
        if (dataHolder != null && callerContext != null && id != null) {
            String key = keyPrefix + id;
            dataHolder.addCallerContextIfAbsent(key, callerContext);
            // have to do ,because we always gets
            //  any property as non-replicable
            replicateCaller(id);
        }
    }

    /**
     * Removes the caller and repicate the states
     *
     * @param id The Id of the caller
     */
    public void removeAndFlushCaller(String id) {
        if (id != null) {
            removeCaller(id);
            replicateCaller(id);
        }
    }

    /**
     * Helper method to replicates states of the caller with given key
     *
     * @param id The id of the caller
     */
    private void replicateCaller(String id) {

        if (configctx != null && keyPrefix != null) {
            try {
                if (debugOn) {
                    log.debug("Going to replicate the states of the caller : " + id);
                }

                throttleReplicator.setConfigContext(configctx);
                throttleReplicator.add(keyPrefix + id);

            } catch (Exception clusteringFault) {
                log.error("Error during the replicating states ", clusteringFault);
            }
        }
    }
}
