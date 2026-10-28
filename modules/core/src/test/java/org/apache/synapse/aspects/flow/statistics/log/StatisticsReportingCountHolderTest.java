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
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.flow.statistics.log;

import junit.framework.TestCase;

/**
 * Tests the main branch and the pending branch count of {@link StatisticsReportingCountHolder}, and the single enqueue
 * guard of {@link StatisticsReportingEventHolder}.
 */
public class StatisticsReportingCountHolderTest extends TestCase {

    public void testCloseMainBranchOnce() {
        StatisticsReportingCountHolder counts = new StatisticsReportingCountHolder();
        assertEquals(1, counts.getBranchCount());
        assertTrue(counts.isMainBranchOpen());

        assertTrue(counts.closeMainBranch());
        assertEquals(0, counts.getBranchCount());
        assertFalse(counts.isMainBranchOpen());

        assertFalse("A second close is a no-op", counts.closeMainBranch());
        assertEquals(0, counts.getBranchCount());
    }

    public void testPendingBranchCount() {
        StatisticsReportingCountHolder counts = new StatisticsReportingCountHolder();
        assertEquals(0, counts.getPendingBranchCount());

        counts.incrementBranchCount();
        counts.incrementBranchCount();
        assertEquals(3, counts.getBranchCount());
        assertEquals("Main branch open", 2, counts.getPendingBranchCount());

        counts.closeMainBranch();
        assertEquals(2, counts.getBranchCount());
        assertEquals("Main branch closed", 2, counts.getPendingBranchCount());

        counts.decrementAndGetBranchCount();
        counts.decrementAndGetBranchCount();
        assertEquals(0, counts.getBranchCount());
        assertEquals(0, counts.getPendingBranchCount());
    }

    public void testMarkEnqueuedOnce() {
        StatisticsReportingEventHolder eventHolder = new StatisticsReportingEventHolder();
        assertTrue(eventHolder.markEnqueued());
        assertFalse(eventHolder.markEnqueued());
        assertFalse(eventHolder.markEnqueued());
    }
}
