/*
* Copyright 2014 WSO2, Inc. http://wso2.com
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
import java.util.concurrent.atomic.AtomicInteger;

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
    /* The nextTimeWindow - beginning of next unit time period- end of current unit time period */
    private long nextTimeWindow = 0;
    /* The globalCount to keep track number of request */
    private AtomicInteger globalCount = new AtomicInteger(0);

    /**
     * Count to keep track of local (specific to this node) number of requests
     */
    private AtomicInteger localCount = new AtomicInteger(0);

    /**
     * Used for debugging purposes. *
     */
    private UUID uuid = UUID.randomUUID();

    /* The Id of caller */
    private String ID;

    public CallerContext clone() throws CloneNotSupportedException {
        super.clone();
        CallerContext clone = new CallerContext(this.ID) {
            @Override
            public int getType() {
                return CallerContext.this.getType();
            }
        };
        clone.nextAccessTime = this.nextAccessTime;
        clone.firstAccessTime = this.firstAccessTime;
        clone.nextTimeWindow = this.nextTimeWindow;
        clone.globalCount = new AtomicInteger(this.globalCount.intValue());
        clone.localCount = new AtomicInteger(this.localCount.intValue());
        localCount.set(0);
        return clone;
    }

    public CallerContext(String ID) {
        if (ID == null || "".equals(ID)) {
            throw new InstantiationError("Couldn't create a CallContext for an empty " +
                    "remote caller ID");
        }
        this.ID = ID.trim();
        log.debug("CallerContext created with ID : " + this.ID);
    }

    public UUID getUuid() {
        return uuid;
    }

    /**
     * @return Returns Id of caller
     */
    public String getID() {
        return this.ID;
    }

    /**
     * Init the access for a particular caller , caller will registered with context
     *
     * @param configuration   -The Configuration for this caller
     * @param throttleContext -The Throttle Context
     * @param currentTime     -The system current time in milliseconds
     */
    private void initAccess(CallerConfiguration configuration, ThrottleContext throttleContext,
                            long currentTime) {
        this.firstAccessTime = currentTime;  // set the first access time
        // the end of this time window
        this.nextTimeWindow = currentTime + configuration.getUnitTime();
        //throttleContext.addCallerContext(this, ID); // register this in the throttle
    }

    /**
     * To verify access if the unit time has already not over
     *
     * @param configuration   -The Configuration for this caller
     * @param throttleContext -The Throttle Context
     * @param currentTime     -The system current time
     * @return boolean        -The boolean value which say access will allow or not
     */
    private boolean canAccessIfUnitTimeNotOver(CallerConfiguration configuration,
                                               ThrottleContext throttleContext, long currentTime) {
        boolean canAccess = false;
        int maxRequest = configuration.getMaximumRequestPerUnitTime();
        if (!(maxRequest == 0)) {
            if (this.globalCount.get() <= maxRequest - 1) {
            //If the globalCount is less than max request
                if (log.isDebugEnabled()) {
                    log.debug("Access allowed :: " + (maxRequest - this.globalCount.get())
                            + " of available of " + maxRequest + " connections " + this.ID);
                }
                canAccess = true;     // can continue access
                this.globalCount.incrementAndGet();
                this.localCount.incrementAndGet();
                // Send the current state to others (clustered env)
                throttleContext.flushCallerContext(this, ID);
                // can complete access

            } else {
                //else , if caller has not already prohibit
                if (this.nextAccessTime == 0) {
                    //and if there is no prohibit time  period in configuration
                    long prohibitTime = configuration.getProhibitTimePeriod();
                    if (prohibitTime == 0) {
                        //prohibit access until unit time period is over
                        this.nextAccessTime = this.firstAccessTime +
                                configuration.getUnitTime();
                    } else {
                        //if there is a prohibit time period in configuration ,then
                        //set it as prohibit period
                        this.nextAccessTime = currentTime + prohibitTime;
                    }
                    if (log.isDebugEnabled()) {
                        String type = ThrottleConstants.IP_BASE == configuration.getType() ?
                                "IP address" : "domain";
                        log.debug("Maximum Number of requests are reached for caller with "
                                + type + " - " + this.ID);
                    }
                    // Send the current state to others (clustered env)
                    throttleContext.flushCallerContext(this, ID);
                } else {
                    // else , if the caller has already prohibit and prohibit
                    // time period has already over
                    if (this.nextAccessTime
                            <= currentTime) {
                        if (log.isDebugEnabled()) {
                            log.debug("Access allowed :: " + (maxRequest)
                                    + " of available of " + maxRequest + " connections " + this.ID);
                        }
                        // remove previous caller context
                        if (this.nextTimeWindow != 0) {
                            throttleContext.removeCallerContext(ID);
                        }
                        // reset the states so that, this is the first access
                        this.nextAccessTime = 0;
                        canAccess = true;
                        int prevGlobal = globalCount.get();
                        this.globalCount.set(1);
                        // can access the system   and this is same as first access
                        if (prevGlobal > globalCount.get()) {
                            log.debug("Global Count , Previous = " + prevGlobal);
                        }
                        this.firstAccessTime = currentTime;
                        this.nextTimeWindow = currentTime + configuration.getUnitTime();
                        // registers caller and send the current state to others (clustered env)
                        throttleContext.addAndFlushCallerContext(this, ID);
                    } else {
                        if (log.isDebugEnabled()) {
                            String type = ThrottleConstants.IP_BASE == configuration.getType() ?
                                    "IP address" : "domain";
                            log.debug("Prohibit period is not yet over for caller with "
                                    + type + " - " + this.ID);
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
    private boolean canAccessIfUnitTimeOver(CallerConfiguration configuration,
                                            ThrottleContext throttleContext, long currentTime) {

        boolean canAccess = false;
        // if number of access for a unit time is less than MAX and
        // if the unit time period (session time) has just over
        int maxRequest = configuration.getMaximumRequestPerUnitTime();
        if (!(maxRequest == 0)) {
            if (this.globalCount.get() <= maxRequest - 1) {
                if (this.nextTimeWindow != 0) {
                    // Removes and sends the current state to others  (clustered env)
                    throttleContext.removeAndFlushCaller(ID);
                }
                canAccess = true; // this is bonus access
                //next time callers can access as a new one
            } else {
                // if number of access for a unit time has just been greater than MAX
                // now same as a new session
                // OR
                //  if caller in prohibit session  and prohibit period has just over
                if ((this.nextAccessTime == 0) ||
                        (this.nextAccessTime <= currentTime)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Access allowed :: " + (maxRequest) + " of available of "
                                + maxRequest + " connections " + this.ID);
                    }
                    //remove previous callercontext instance
                    if (this.nextTimeWindow != 0) {
                        throttleContext.removeCallerContext(ID);
                    }
                    // reset the states so that, this is the first access
                    this.nextAccessTime = 0;
                    canAccess = true;
                    int prevGlobal = globalCount.get();
                    this.globalCount.set(1);
                    // can access the system   and this is same as first Access
                    if (prevGlobal > globalCount.get()) {
                        log.debug("Global Count Reduced : Previous = " + prevGlobal);
                    }
                    this.firstAccessTime = currentTime;
                    this.nextTimeWindow = currentTime + configuration.getUnitTime();
                    // registers caller and send the current state to others (clustered env)
                    throttleContext.addAndFlushCallerContext(this, ID);
                } else {
                    // if  caller in prohibit session  and prohibit period has not  over
                    if (log.isDebugEnabled()) {
                        String type = ThrottleConstants.IP_BASE == configuration.getType() ?
                                "IP address" : "domain";
                        log.debug("Even unit time has over , CallerContext in prohibit state :"
                                + type + " - " + this.ID);
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
            if (this.globalCount.get() <= maxRequest - 1) {
                if (this.nextTimeWindow != 0) {
                    if (log.isDebugEnabled()) {
                        log.debug("Removing caller with id " + this.ID);
                    }
                    //Removes the previous callercontext and Sends the current state to
                    //  others (clustered env)
                    throttleContext.removeAndFlushCaller(ID);
                }
            } else {
                // if number of access for a unit time has just been greater than MAX
                // now same as a new session
                // OR
                //  if caller in prohibit session  and prohibit period has just over
                if ((this.nextAccessTime == 0) ||
                        (this.nextAccessTime <= currentTime)) {
                    if (this.nextTimeWindow != 0) {
                        if (log.isDebugEnabled()) {
                            log.debug("Removing caller with id " + this.ID);
                        }
                        //Removes the previous callercontext and Sends
                        //  the current state to others (clustered env)
                        throttleContext.removeAndFlushCaller(ID);
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

        if (log.isDebugEnabled()) {
            log.debug("Current global count=" + globalCount);
            log.debug("Current local count=" + localCount);
        }

        // if caller access first time in his new session
        if (this.firstAccessTime == 0) {
            initAccess(configuration, throttleContext, currentTime);
        }
        // if unit time period (session time) is not over
        if (this.nextTimeWindow > currentTime) {
            canAccess = canAccessIfUnitTimeNotOver(configuration, throttleContext, currentTime);
            log.debug(" canAccessIfUnitTimeNotOver : " + canAccess + " Global Count : " +
                    globalCount + " , UUID : " + uuid);
        } else {
            canAccess = canAccessIfUnitTimeOver(configuration, throttleContext, currentTime);
            log.debug(" canAccessIfUnitTimeOver : " + canAccess + " Global Count : " +
                    globalCount + " , UUID : " + uuid);
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

    public int getGlobalCounter() {
        return globalCount.get();
    }

    public void setGlobalCounter(int counter) {
        if (counter < globalCount.get()) {
            log.debug("Global Counter Reduced : Previous " + globalCount.get());
        }
        globalCount.set(counter);
    }

    public int getLocalCounter() {
        return localCount.get();
    }

    public void resetLocalCounter() {
        localCount.set(0);
    }

    /**
     * Gets type of throttle that this caller belong  ex : ip/domain
     *
     * @return Returns the type of the throttle
     */
    public abstract int getType();

}