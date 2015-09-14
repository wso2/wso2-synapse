/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Contains all runtime data for a particular remote caller.
 * provides the default rate based access controller algorithm implementation.
 * This is not thread-safe
 */

public abstract class CallerContext implements Serializable, Cloneable {
    private static final long serialVersionUID = 1652165180220263492L;
    private static Log log = LogFactory.getLog(CallerContext.class.getName());

    /* next access time - the end of prohibition */
    private long nextAccessTime = 0;
    /* first access time - when caller came across the on first time */
    private long firstAccessTime = 0;
    /* The nextTimeWindow - beginning of next unit time period- end of current unit time period  */
    private long nextTimeWindow = 0;
    /* The globalCount to keep track number of request */
    private AtomicLong globalCount = new AtomicLong(0);

    private long unitTime;
    /**
     * Count to keep track of local (specific to this node) number of requests
     */
    private AtomicLong localCount = new AtomicLong(0);

    /**
     * Used for debugging purposes. *
     */
    private UUID uuid = UUID.randomUUID();

    /* The Id of caller */
    private String id;

    public CallerContext clone() throws CloneNotSupportedException {
        super.clone();
        CallerContext clone = new CallerContext(this.id) {
            @Override
            public int getType() {
                return CallerContext.this.getType();
            }
        };
        clone.nextAccessTime = this.nextAccessTime;
        clone.firstAccessTime = this.firstAccessTime;
        clone.nextTimeWindow = this.nextTimeWindow;
        clone.globalCount = new AtomicLong(this.globalCount.longValue());
        clone.localCount = new AtomicLong(this.localCount.longValue());

        localCount.set(0);
        return clone;
    }

    public CallerContext(String ID) {
        if (ID == null || "".equals(ID)) {
            throw new InstantiationError("Couldn't create a CallContext for an empty " +
                                         "remote caller ID");
        }
        this.id = ID.trim();
    }

    public UUID getUuid() {
        return uuid;
    }

    /**
     * @return Returns Id of caller
     */
    public String getId() {
        return this.id;
    }

    /**
     * Init the access for a particular caller , caller will registered with context
     *
     * @param configuration   -The Configuration for this caller
     * @param throttleContext -The Throttle Context
     * @param currentTime     -The system current time in milliseconds
     */
    private void initAccess(CallerConfiguration configuration, ThrottleContext throttleContext, long currentTime) {
        this.unitTime = configuration.getUnitTime();
        this.firstAccessTime = currentTime;
        this.nextTimeWindow = this.firstAccessTime + this.unitTime;
        //Also we need to pick counter value associated with time window.
        throttleContext.addCallerContext(this, this.id);
        throttleContext.replicateTimeWindow(this.id);
    }

    /**
     * To verify access if the unit time has already not over
     *
     * @param configuration   -  The Configuration for this caller
     * @param throttleContext -The Throttle Context
     * @param currentTime     -The system current time
     * @return boolean        -The boolean value which say access will allow or not
     */
    private boolean canAccessIfUnitTimeNotOver(CallerConfiguration configuration,
                                               ThrottleContext throttleContext, long currentTime) {
        boolean canAccess = false;
        int maxRequest = configuration.getMaximumRequestPerUnitTime();
        if (maxRequest != 0) {
            if ((this.globalCount.get() + this.localCount.get()) < maxRequest) {    //If the globalCount is less than max request
                if (log.isDebugEnabled()) {
                    log.debug("CallerContext Checking access if unit time is not over and less than max count>> Access "
                            + "allowed=" + maxRequest + " available="+ (maxRequest - (this.globalCount.get() + this.localCount.get()))
                            +" key=" + this.getId() + " currentGlobalCount=" + globalCount + " currentTime="
                            +  currentTime + " " + "nextTimeWindow=" + this.nextTimeWindow + " currentLocalCount=" + localCount + " Tier="
                            + configuration.getID() + " nextAccessTime=" + this.nextAccessTime);
                }
                canAccess = true;     // can continue access
                this.globalCount.incrementAndGet();
                this.localCount.incrementAndGet();
                // Send the current state to others (clustered env)
                throttleContext.flushCallerContext(this, id);
                // can complete access

            } else {
                //else , if caller has not already prohibit
                if (this.nextAccessTime == 0) {
                    //and if there is no prohibit time  period in configuration
                    long prohibitTime = configuration.getProhibitTimePeriod();
                    if (prohibitTime == 0) {
                        //prohibit access until unit time period is over
                        this.nextAccessTime = this.firstAccessTime + configuration.getUnitTime();
                    } else {
                        //if there is a prohibit time period in configuration ,then
                        //set it as prohibit period
                        this.nextAccessTime = currentTime + prohibitTime;
                    }
                    if (log.isDebugEnabled()) {
                        String type = ThrottleConstants.IP_BASE == configuration.getType() ?
                                "IP address" : "domain";
                        log.debug("Maximum Number of requests are reached for caller with "
                                + type + " - " + this.id);
                    }
                    // Send the current state to others (clustered env)
                    throttleContext.flushCallerContext(this, id);
                } else {
                    // else , if the caller has already prohibit and prohibit
                    // time period has already over
                    if (this.nextAccessTime <= currentTime) {
                        if (log.isDebugEnabled()) {
                            log.debug("CallerContext Checking access if unit time is not over before time window exceed >> "
                                    + "Access allowed=" + maxRequest + " available="
                                    +  (maxRequest - (this.globalCount.get() + this.localCount.get()))
                                    + " key=" + this.getId() + " currentGlobalCount=" + globalCount
                                    + " currentTime=" + currentTime + " " + "nextTimeWindow=" + this.nextTimeWindow
                                    + " currentLocalCount=" + localCount + " " + "Tier=" + configuration.getID()
                                    + " nextAccessTime=" + this.nextAccessTime);
                        }
                        // remove previous caller context
                        if (this.nextTimeWindow != 0) {
                            throttleContext.removeCallerContext(id);
                        }
                        // reset the states so that, this is the first access
                        this.nextAccessTime = 0;
                        canAccess = true;

                        this.globalCount.set(0);// can access the system   and this is same as first access
                        this.localCount.set(1);
                        this.firstAccessTime = currentTime;
                        this.nextTimeWindow = currentTime + configuration.getUnitTime();
                        throttleContext.addAndFlushCallerContext(this, this.id);
                        throttleContext.replicateTimeWindow(this.id);

                        if(log.isDebugEnabled()) {
                            log.debug("Caller=" + this.getId() + " has reset counters and added for replication when unit "
                                      + "time is not over");
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            String type = ThrottleConstants.IP_BASE == configuration.getType() ?
                                    "IP address" : "domain";
                            log.debug("Prohibit period is not yet over for caller with "
                                    + type + " - " + this.id);
                        }
                    }
                }
            }

        }
        return canAccess;
    }

    /**
     * To verify access if unit time has already over
     *
     * @param configuration   -The Configuration for this caller
     * @param throttleContext -The Throttle that caller having pass
     * @param currentTime     -The system current time
     * @return boolean        -The boolean value which say access will allow or not
     */
    private boolean canAccessIfUnitTimeOver(CallerConfiguration configuration, ThrottleContext throttleContext, long currentTime) {

        boolean canAccess = false;
        // if number of access for a unit time is less than MAX and
        // if the unit time period (session time) has just over
        int maxRequest = configuration.getMaximumRequestPerUnitTime();
        if (maxRequest != 0) {
            if ((this.globalCount.get() + this.localCount.get()) < maxRequest) {
                if (this.nextTimeWindow != 0) {
                    // Removes and sends the current state to others  (clustered env)
                    throttleContext.removeAndFlushCaller(this.id);
                }
                if (log.isDebugEnabled()) {
                    log.debug("CallerContext Checking access if unit time over next time window>> Access allowed="
                            +  maxRequest + " available=" + (maxRequest - (this.globalCount.get() + this.localCount.get()))
                            + " key=" + this.getId()+ " currentGlobalCount=" + globalCount + " currentTime=" + currentTime
                            + " nextTimeWindow=" + this.nextTimeWindow +" currentLocalCount=" + localCount + " Tier="
                            + configuration.getID() + " nextAccessTime="+ this.nextAccessTime);
                }
                canAccess = true; // this is bonus access
                //next time callers can access as a new one
            } else {
                // if number of access for a unit time has just been greater than MAX now same as a new session
                // OR if caller in prohibit session  and prohibit period has just over
                if ((this.nextAccessTime == 0) || (this.nextAccessTime <= currentTime)) {
                    if (log.isDebugEnabled()) {
                        log.debug("CallerContext Checking access if unit time over>> Access allowed=" + maxRequest
                                + " available=" + (maxRequest - (this.globalCount.get() + this.localCount.get())) + " key=" + this.getId()
                                + " currentGlobalCount=" + globalCount + " currentTime=" + currentTime + " nextTimeWindow=" + this.nextTimeWindow
                                + " currentLocalCount=" + localCount + " Tier=" + configuration.getID() + " nextAccessTime="
                                + this.nextAccessTime);
                    }
                    //remove previous callercontext instance
                    if (this.nextTimeWindow != 0) {
                        throttleContext.removeCallerContext(id);
                    }
                    // reset the states so that, this is the first access
                    this.nextAccessTime = 0;
                    canAccess = true;

                    this.globalCount.set(0);// can access the system   and this is same as first access
                    this.localCount.set(1);
                    this.firstAccessTime = currentTime;
                    this.nextTimeWindow = currentTime + configuration.getUnitTime();
                    // registers caller and send the current state to others (clustered env)
                    throttleContext.addAndFlushCallerContext(this, id);
                    throttleContext.replicateTimeWindow(this.id);

                    if(log.isDebugEnabled()) {
                        log.debug("Caller=" + this.getId() + " has reset counters and added for replication when unit "
                                  + "time is over");
                    }
                } else {
                    // if  caller in prohibit session  and prohibit period has not  over
                    if (log.isDebugEnabled()) {
                        String type = ThrottleConstants.IP_BASE == configuration.getType() ?
                                "IP address" : "domain";
                        log.debug("Even unit time has over , CallerContext in prohibit state :"
                                + type + " - " + this.id);
                    }
                }
            }

        }
        return canAccess;

    }

    /**
     * Clean up the callers - remove all callers that have expired their time window
     *
     * @param configuration   -The Configuration for this caller
     * @param throttleContext -The Throttle that caller having pass
     * @param currentTime     -The system current time
     */
    public void cleanUpCallers(CallerConfiguration configuration,
                               ThrottleContext throttleContext, long currentTime) {

        if (log.isDebugEnabled()) {
            log.debug("Cleaning up the inactive caller's states ... ");
        }
        if (configuration == null) {
            if (log.isDebugEnabled()) {
                log.debug("Couldn't find the configuration .");
            }
            return;
        }
        // if number of access for a unit time is less than MAX and
        // if the unit time period (session time) has over

        int maxRequest = configuration.getMaximumRequestPerUnitTime();
        if (!(maxRequest == 0)) {
            if ((this.globalCount.get() + this.localCount.get()) <= (maxRequest - 1)) {
                if (this.nextTimeWindow != 0 && this.nextTimeWindow < (currentTime - this.unitTime)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Removing caller with id " + this.id);
                    }
                    //Removes the previous callercontext and Sends the current state to
                    //  others (clustered env)
                    throttleContext.removeAndDestroyShareParamsOfCaller(id);
                }
            } else {
                // if number of access for a unit time has just been greater than MAX
                // now same as a new session
                // OR
                //  if caller in prohibit session  and prohibit period has just over and only
                if ((this.nextAccessTime == 0) || this.nextAccessTime < (currentTime - this.unitTime)) {
                    if (this.nextTimeWindow != 0 && this.nextTimeWindow < (currentTime - this.unitTime)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Removing caller with id " + this.id);
                        }
                        //Removes the previous callercontext and Sends
                        //  the current state to others (clustered env)
                        throttleContext.removeAndDestroyShareParamsOfCaller(id);
                    }
                }
            }
        }
    }

    /**
     * Check whether that caller can access or not ,based on current state and pre-defined policy
     *
     * @param throttleContext -The Context for this caller - runtime state
     * @param configuration   -The Configuration for this caller - data from policy
     * @param currentTime     -The current system time
     * @return boolean        -The boolean value which say access will allow or not
     * @throws ThrottleException throws for invalid throttle configuration
     */
    public boolean canAccess(ThrottleContext throttleContext, CallerConfiguration configuration,
                             long currentTime) throws ThrottleException {
        boolean canAccess;
        if (configuration == null) {
            if (log.isDebugEnabled()) {
                log.debug("Couldn't find the configuration .");
            }
            return true;
        }
        if (configuration.getMaximumRequestPerUnitTime() < 0
                || configuration.getUnitTime() <= 0
                || configuration.getProhibitTimePeriod() < 0) {
            throw new ThrottleException("Invalid Throttle Configuration");
        }

        // if caller access first time in his new session
        if (this.firstAccessTime == 0) {
            initAccess(configuration, throttleContext, currentTime);
        }
        // if unit time period (session time) is not over
        if (this.nextTimeWindow > currentTime) {
            canAccess = canAccessIfUnitTimeNotOver(configuration, throttleContext, currentTime);
        } else {
            canAccess = canAccessIfUnitTimeOver(configuration, throttleContext, currentTime);
        }

        return canAccess;

    }

    /**
     * Returns the next time window
     *
     * @return long value of next time window
     */
    public long getNextTimeWindow() {
        return this.nextTimeWindow;
    }

    public void incrementGlobalCounter(int incrementBy) {
        globalCount.addAndGet(incrementBy);
    }

    public long getGlobalCounter() {
        return globalCount.get();
    }

    public void setGlobalCounter(long counter) {
        globalCount.set(counter);
    }

    public long getLocalCounter() {
        return localCount.get();
    }

    public void resetLocalCounter() {
        localCount.set(0);
    }

    public void resetGlobalCounter() {
        globalCount.set(0);
    }

    /**
     * Gets type of throttle that this caller belong  ex : ip/domain
     *
     * @return Returns the type of the throttle
     */
    public abstract int getType();

    public long getFirstAccessTime() {
        return firstAccessTime;
    }

    public void setFirstAccessTime(long firstAccessTime) {
        this.firstAccessTime = firstAccessTime;
    }

    public void setNextTimeWindow(long nextTimeWindow) {
        this.nextTimeWindow = nextTimeWindow;
    }

    public long getUnitTime() {
        return unitTime;
    }

    public void setUnitTime(long unitTime) {
        this.unitTime = unitTime;
    }
}