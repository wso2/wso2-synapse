/*
*  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 LLC. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.commons.throttle.core;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.throttle.core.internal.ThrottleServiceDataHolder;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unified throttle window and counter replicator that serializes both window and counter
 * operations per caller to eliminate race conditions. Coordinates state synchronization across
 * cluster nodes via Redis while detecting concurrent window resets. No Redis operations are
 * performed inside synchronization locks. Enable via {@code throttling.unified_replicator.enable=true}.
 */
public class UnifiedThrottleReplicator {

    private static final Log log = LogFactory.getLog(UnifiedThrottleReplicator.class);

    private volatile ConfigurationContext configContext;
    private final ThrottleProperties throttleProperties;
    private final AtomicInteger replicatorCount = new AtomicInteger(0);
    private int replicatorPoolSize;

    // workQueue: keys to replicate this tick; inQueue: dedup guard so the same key is
    // not offered twice by concurrent request threads.
    private final ConcurrentLinkedQueue<String> workQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> inQueue = ConcurrentHashMap.newKeySet();

    // processing: per-caller mutex guaranteeing at most one replicator thread works a given
    // caller at a time. Distinct from inQueue (which only dedups queue entries). Without this,
    // when throttling.pool.size > 1 two threads can process the same caller concurrently — one
    // pushing while another runs the lock-free global-counter refresh — which double-counts the
    // in-flight push (its delta is in Redis but not yet subtracted from localCount) and can
    // briefly reject a caller that is actually under its limit.
    private final ConcurrentHashMap<String, Boolean> processing = new ConcurrentHashMap<>();

    public UnifiedThrottleReplicator() {
        throttleProperties = ThrottleServiceDataHolder.getInstance().getThrottleProperties();
        replicatorPoolSize = Integer.parseInt(throttleProperties.getThrottlingPoolSize());

        if (!throttleProperties.isUnifiedThrottleReplicatorEnabled()
                || throttleProperties.isThrottleSyncAsyncHybridModeEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("Unified throttle replicator is not enabled or hybrid mode is active."
                        + " Scheduler will not be started.");
            }
            return;
        }

        String frequency = throttleProperties.getThrottlingReplicationFrequency();
        int freq = Integer.parseInt(frequency);

        if (log.isDebugEnabled()) {
            log.debug("Starting unified throttle replicator: pool=" + replicatorPoolSize
                    + " frequency=" + freq + "ms");
        }

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(replicatorPoolSize,
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setName("Unified Throttle Replicator - " + replicatorCount.getAndIncrement());
                        return t;
                    }
                });

        for (int i = 0; i < replicatorPoolSize; i++) {
            executor.scheduleAtFixedRate(new ReplicatorTask(), freq, freq, TimeUnit.MILLISECONDS);
        }
    }

    public void setConfigContext(ConfigurationContext configContext) {
        if (configContext == null) {
            throw new IllegalArgumentException("ConfigurationContext must not be null");
        }
        if (this.configContext == null) {
            this.configContext = configContext;
        }
    }

    public void add(String key) {
        if (configContext == null) {
            throw new IllegalStateException("ConfigurationContext has not been set");
        }
        if (inQueue.add(key)) {
            workQueue.offer(key);
            if (log.isTraceEnabled()) {
                log.trace("Enqueued key for replication: " + key);
            }
        }
    }

    private class ReplicatorTask implements Runnable {

        public void run() {
            if (workQueue.isEmpty()) {
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug("Unified throttle replicator tick started. Queue size=" + workQueue.size());
            }

            String key;
            while ((key = workQueue.poll()) != null) {
                // Key is now owned by this thread for this attempt; remove from inQueue so
                // concurrent add() calls for the same key can re-enqueue it if needed.
                inQueue.remove(key);

                // Claim exclusive processing of this caller. If another replicator thread already
                // owns it, drop this (redundant) copy: the owner is already handling the caller's
                // current state, and any later change will re-enqueue via add(). This serialization
                // is what prevents a concurrent push and lock-free refresh from double-counting.
                if (processing.putIfAbsent(key, Boolean.TRUE) != null) {
                    if (log.isTraceEnabled()) {
                        log.trace("Caller already being processed by another thread, skipping: " + key);
                    }
                    continue;
                }

                try {
                    ThrottleDataHolder dataHolder = (ThrottleDataHolder)
                            configContext.getProperty(ThrottleConstants.THROTTLE_INFO_KEY);
                    if (dataHolder == null) {
                        log.warn("ThrottleDataHolder not available for key=" + key
                                + ". Will retry on next tick.");
                        continue;
                    }

                    CallerContext callerContext = dataHolder.getCallerContext(key);
                    if (callerContext == null) {
                        continue;
                    }

                    String id     = callerContext.getId();
                    long localFAT = callerContext.getFirstAccessTime();
                    long unitTime = callerContext.getUnitTime();
                    long currentTime = System.currentTimeMillis();

                    // Skip expired windows — no point syncing stale state.
                    if (localFAT + unitTime <= currentTime) {
                        continue;
                    }

                    // Quick-exit if local counter is zero — avoid Redis lock round-trip.
                    // Still refresh the cached global counter (lock-free) so a node with
                    // nothing to push does not freeze on a stale cluster total.
                    if (callerContext.getLocalCounter() == 0) {
                        refreshGlobalCounterLockFree(callerContext, id, localFAT, unitTime);
                        continue;
                    }

                    // Single-shot Redis lock attempt — no spin. If the lock is held by another
                    // cluster node, re-queue this key so another replicator thread can retry it
                    // within the same tick rather than blocking this thread or discarding the key.
                    boolean lockAcquired = SharedParamManager.tryWindowLock(id, localFAT + unitTime);
                    if (!lockAcquired) {
                        if (log.isDebugEnabled()) {
                            log.debug("Could not acquire window lock for callerId=" + id
                                    + ". Re-queuing for peer thread retry this tick.");
                        }
                        // Couldn't push this tick — refresh the cached global counter (lock-free)
                        // so a node that keeps losing the lock still tracks the cluster total.
                        refreshGlobalCounterLockFree(callerContext, id, localFAT, unitTime);
                        requeueThisTick(key);
                        continue;
                    }

                    try {
                        // Verify window is still active and hasn't changed since lock acquisition.
                        if (callerContext.getFirstAccessTime() != localFAT) { continue; }
                        if (localFAT + unitTime <= System.currentTimeMillis()) { continue; }

                        // Snapshot local counter under lock. Ensures all code paths use consistent value
                        // and prevents divergence from concurrent request threads.
                        long snapshotCounter = callerContext.getLocalCounter();
                        if (snapshotCounter == 0) {
                            continue;
                        }

                        // Read Redis window state (no race while lock is held).
                        long[] windowState;
                        try {
                            windowState = SharedParamManager.getWindowState(id);
                        } catch (Exception e) {
                            log.error("Failed to read window state from Redis for key=" + key
                                    + ". Will retry on next tick.", e);
                            continue;
                        }
                        long sharedTimestamp    = windowState[0];
                        long distributedCounter = windowState[1];
                        long sharedNextWindow   = sharedTimestamp + unitTime;

                    // State routing based on Redis window and local window alignment:
                    // A (no/expired Redis window): sharedTimestamp==0 or expired or local ahead
                    // B (window desync): sharedTimestamp != localFAT (cluster rolled or FAT drift)
                    // C (fully synced): sharedTimestamp == localFAT (counters only)
                    // States A & B handle window creation/alignment; State C pushes counters only.

                    if (sharedTimestamp == 0 || sharedNextWindow <= currentTime || localFAT >= sharedNextWindow) {
                        // Branch A: No active Redis window. Create it with this node's snapshot.
                        try {
                            SharedParamManager.setWindow(id, snapshotCounter, localFAT, localFAT + unitTime);
                        } catch (Exception e) {
                            log.error("Failed to set Redis window for key=" + key
                                    + ". Will retry on next tick.", e);
                            continue;
                        }

                        // Commit local state. The synchronized block is the authoritative gate:
                        // if the window rolled or expired during the setWindow call the check
                        // fails and counts are left for the next tick. setGlobalCounter atomically
                        // replaces any stale previous-window value — no pre-call reset needed.
                        synchronized (id.intern()) {
                            if (callerContext.getFirstAccessTime() == localFAT
                                    && localFAT + unitTime > System.currentTimeMillis()) {
                                callerContext.subtractFromLocalCounterSafe(snapshotCounter);
                                callerContext.setGlobalCounter(snapshotCounter);
                            } else {
                                if (log.isDebugEnabled()) {
                                    log.debug("Window changed or expired while pushing counter"
                                            + " for key=" + key + ". Local count retained.");
                                }
                                continue;
                            }
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Replicated callerId=" + id
                                    + " pushed=" + snapshotCounter + " total=" + snapshotCounter);
                        }

                    } else if (sharedTimestamp != localFAT) {
                        // Branch B: Window mismatch — either cluster rolled (State 4) or FAT drift (State 5).
                        // Both require window realignment; State 4 also resets local counters.

                        synchronized (id.intern()) {
                            long currentFAT = callerContext.getFirstAccessTime();

                            // Guard: if a concurrent window change occurred between the localFAT
                            // capture (top of run()) and here, currentFAT != localFAT.  Overwriting
                            // the newer window's FAT with sharedTimestamp (from an older epoch) would
                            // set it backwards.  Skip all alignment and let the newer window's own
                            // replication tick handle it.
                            if (localFAT != currentFAT) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Concurrent window change detected for callerId=" + id
                                            + " localFAT=" + localFAT + " currentFAT=" + currentFAT
                                            + ". Skipping Branch B alignment.");
                                }
                                continue;
                            }

                            // State 4: cluster is in a newer epoch — clear local counts so this
                            // node does not carry forward stale numbers that would throttle valid requests.
                            // State 5: FAT drift within the same epoch. This can occur in either direction:
                            //   sharedTimestamp < localFAT — another node started slightly earlier.
                            //   sharedTimestamp > localFAT (by a small delta < unitTime) — another node
                            //     started slightly later. In this sub-case sharedTimestamp > currentFAT
                            //     is TRUE so the reset fires, discarding counts from the small drift
                            //     interval. This conservative behaviour is shared with ThrottleWindowReplicator.
                            if (sharedTimestamp > currentFAT) {
                                callerContext.resetLocalCounter();
                                callerContext.resetGlobalCounter();
                                if (log.isDebugEnabled()) {
                                    log.debug("Cluster window is newer — resetting local counters for callerId=" + id
                                            + " currentFAT=" + currentFAT + " sharedTimestamp=" + sharedTimestamp);
                                }
                            } else {
                                if (log.isDebugEnabled()) {
                                    log.debug("FAT drift within same epoch for callerId=" + id
                                            + " currentFAT=" + currentFAT + " sharedTimestamp=" + sharedTimestamp);
                                }
                            }

                            // localFAT == currentFAT is guaranteed here, so FAT/counter updates are safe.
                            callerContext.setFirstAccessTime(sharedTimestamp);
                            callerContext.setNextTimeWindow(sharedNextWindow);
                            callerContext.setGlobalCounter(distributedCounter);
                        }

                        // Push remaining local counts. State 4 reset localCounter (~0 after reset); State 5 has counts.
                        long alignedSnapshot = callerContext.getLocalCounter();
                        if (alignedSnapshot == 0) {
                            continue;
                        }

                        // Verify alignment persists and window is active. Prevents double-increment
                        // if window changes after alignment but before Redis push.
                        if (callerContext.getFirstAccessTime() != sharedTimestamp
                                || sharedNextWindow <= System.currentTimeMillis()) {
                            continue;
                        }

                        try {
                            long newTotal = SharedParamManager.incrWindowCounter(id, alignedSnapshot, sharedNextWindow);
                            synchronized (id.intern()) {
                                if (callerContext.getFirstAccessTime() == sharedTimestamp
                                        && sharedNextWindow > System.currentTimeMillis()) {
                                    callerContext.subtractFromLocalCounterSafe(alignedSnapshot);
                                    callerContext.setGlobalCounter(newTotal);
                                } else {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Window changed or expired while pushing counter"
                                                + " for key=" + key + ". Local count retained.");
                                    }
                                    continue;
                                }
                            }
                            if (log.isDebugEnabled()) {
                                log.debug("Replicated callerId=" + id
                                        + " pushed=" + alignedSnapshot + " total=" + newTotal);
                            }
                        } catch (Exception e) {
                            log.error("Failed to push counter for key=" + key
                                    + ". Local count preserved (" + alignedSnapshot + ").", e);
                            continue;
                        }

                    } else {
                        // Branch C: Windows fully synced — push counter only.
                        if (log.isDebugEnabled()) {
                            log.debug("Windows are in sync for callerId=" + id);
                        }

                        // Verify window hasn't changed since cluster state was read.
                        if (callerContext.getFirstAccessTime() != localFAT) {
                            if (log.isDebugEnabled()) {
                                log.debug("Window changed during replication for key=" + key
                                        + ". Discarding " + snapshotCounter
                                        + " pending counts. New window will be replicated on next tick.");
                            }
                            continue;
                        }
                        if (localFAT + unitTime <= System.currentTimeMillis()) {
                            continue;
                        }

                        try {
                            long newTotal = SharedParamManager.incrWindowCounter(id, snapshotCounter, localFAT + unitTime);
                            synchronized (id.intern()) {
                                if (callerContext.getFirstAccessTime() == localFAT
                                        && localFAT + unitTime > System.currentTimeMillis()) {
                                    callerContext.subtractFromLocalCounterSafe(snapshotCounter);
                                    callerContext.setGlobalCounter(newTotal);
                                } else {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Window changed or expired while pushing counter"
                                                + " for key=" + key + ". Local count retained.");
                                    }
                                    continue;
                                }
                            }
                            if (log.isDebugEnabled()) {
                                log.debug("Replicated callerId=" + id
                                        + " pushed=" + snapshotCounter + " total=" + newTotal);
                            }
                        } catch (Exception e) {
                            log.error("Failed to push counter for key=" + key
                                    + ". Local count preserved (" + snapshotCounter + ").", e);
                            continue;
                        }
                    }

                } finally {
                    // Always release per-caller Redis lock. Failure blocks all cluster nodes for window TTL.
                    SharedParamManager.releaseWindowLock(id);
                }

                } catch (Throwable t) {
                    log.error("Unexpected error during throttle replication for key=" + key
                            + ". Will retry on next tick.", t);
                } finally {
                    // Release the per-caller processing claim on EVERY exit path. A leaked claim
                    // would permanently block this caller from ever being replicated again.
                    processing.remove(key);
                }
            }
        }
        private void requeueThisTick(String key) {
            if (inQueue.add(key)) {
                workQueue.offer(key);
                if (log.isDebugEnabled()) {
                    log.debug("Re-queued key=" + key + " for lock retry");
                }
            }
        }

        /**
         * Lock-free refresh of this node's cached global counter from Redis. Decouples the
         * global-counter read from the lock-gated push so that a node which never wins the
         * window lock — or has nothing to push — still keeps a fresh view of the cluster
         * total instead of freezing on the value from its last successful push.
         *
         * <p>The Redis value is adopted only if the Redis window timestamp matches this
         * node's current window — checked both at read time and again under the per-caller
         * monitor — so a window roll cannot contaminate a freshly started window with the
         * previous epoch's counter. Best-effort: any Redis hiccup is swallowed and retried
         * on the next tick.</p>
         */
        private void refreshGlobalCounterLockFree(CallerContext callerContext, String id,
                                                  long localFAT, long unitTime) {
            try {
                long[] windowState = SharedParamManager.getWindowState(id);
                if (windowState[0] != localFAT) {
                    // Different or absent Redis window — leave realignment to the push path.
                    return;
                }
                synchronized (id.intern()) {
                    // Re-check the live window under the per-caller monitor: a request thread
                    // may have rolled the window between the Redis read above and here.
                    if (callerContext.getFirstAccessTime() == localFAT
                            && localFAT + unitTime > System.currentTimeMillis()) {
                        callerContext.setGlobalCounter(windowState[1]);
                        if (log.isTraceEnabled()) {
                            log.trace("Lock-free refresh of global counter for callerId=" + id
                                    + " to " + windowState[1]);
                        }
                    }
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Lock-free global counter refresh failed for callerId=" + id
                            + "; relying on push-path refresh next tick.", e);
                }
            }
        }
    }
}
