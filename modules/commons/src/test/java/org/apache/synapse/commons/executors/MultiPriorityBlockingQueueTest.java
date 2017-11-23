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

package org.apache.synapse.commons.executors;

import java.util.ArrayList;

/**
 * Test the MultiPriorityBlockingQueue operations in a single threaded enviorenment.
 */
public class MultiPriorityBlockingQueueTest extends MultiPriorityBlockingQueueAbstractTest {

    private static final int ITEMS = 100;

    private MultiPriorityBlockingQueue fixedQueue;

    private MultiPriorityBlockingQueue unboundedQueue;

    private final int[] priorities = {10, 1};
    private final int[] sizes = {ITEMS, ITEMS};

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        fixedQueue = createFixedQueue(2, sizes, priorities);
        unboundedQueue = createUnboundedQueue(2, priorities);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        fixedQueue.clear();
        unboundedQueue.clear();
    }

    public void testOffer() {
        performOffer(fixedQueue, sizes, priorities);

        assertEquals(ITEMS * 2, fixedQueue.size());

        fixedQueue.clear();

        assertEquals(0, fixedQueue.size());
    }

    public void testUnboundedQueueOffer() {
        performOffer(unboundedQueue, sizes, priorities);

        assertEquals(ITEMS * 2, unboundedQueue.size());

        unboundedQueue.clear();

        assertEquals(0, unboundedQueue.size());
    }

    public void testTake() {
        performOffer(fixedQueue, sizes, priorities);
        performTake(fixedQueue, ITEMS * 2);

        assertEquals(0, fixedQueue.size());
    }

    public void testUnboundedQueueTake() {
        performOffer(unboundedQueue, sizes, priorities);
        performTake(unboundedQueue, ITEMS * 2);

        assertEquals(0, unboundedQueue.size());
    }

    public void testAdd() {
        performOffer(fixedQueue, sizes, priorities);

        boolean exceptionOccured = false;

        try {
            fixedQueue.add(new DummyTask(10));
        } catch (IllegalStateException e) {
            exceptionOccured = true;
        }

        assertTrue("Exception should occur", exceptionOccured);

        fixedQueue.clear();
        assertEquals(0, fixedQueue.size());

        for (int i = 0; i < ITEMS; i++) {
            fixedQueue.add(new DummyTask(1));
            fixedQueue.add(new DummyTask(10));
        }

        assertEquals(ITEMS * 2, fixedQueue.size());

        fixedQueue.clear();
        assertEquals(0, fixedQueue.size());
    }

    public void testPut() throws InterruptedException {
        try {
            performPut(fixedQueue, sizes, priorities);
        } catch (InterruptedException e) {
            assertTrue("This exception shouldn't occur", false);
        }

        assertEquals(ITEMS * 2, fixedQueue.size());

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    fixedQueue.put(new DummyTask(10));

                    assertTrue("Above put should block and this line shouldn't execute", false);
                } catch (InterruptedException e) {
                    assertTrue("Interrupted", true);
                }
            }
        });
        t.start();

        waitForThreadState(t, Thread.State.WAITING);

        assertTrue(t.getState() == Thread.State.WAITING);
        t.interrupt();

        t = new Thread(new Runnable() {
            public void run() {
                try {
                    fixedQueue.put(new DummyTask(10));

                    assertTrue("This should execute since we are taking one " +
                            "element out from the queue", true);
                } catch (InterruptedException e) {
                    assertTrue("Interruption shouldn't occur", false);
                }
            }
        });
        t.start();

        waitForThreadState(t, Thread.State.WAITING);
        // after thread is blocked in put we are going to take an element
        fixedQueue.take();

        waitForThreadState(t, Thread.State.TERMINATED);

        assertEquals(ITEMS * 2, fixedQueue.size());

        fixedQueue.clear();
        assertEquals(0, fixedQueue.size());
    }

    public void testContains() {
        int [] puts = {ITEMS - 1, ITEMS -1};

        performOffer(fixedQueue, puts, priorities);

        DummyTask task = new DummyTask(10);
        fixedQueue.offer(task);

        assertTrue(fixedQueue.contains(task));
        fixedQueue.clear();


        performOffer(unboundedQueue, puts, priorities);

        task = new DummyTask(10);
        unboundedQueue.offer(task);

        assertTrue(unboundedQueue.contains(task));

        fixedQueue.clear();
        assertEquals(0, fixedQueue.size());
    }

    public void testRemove() {
        int [] puts = {5, 5};

        performOffer(fixedQueue, puts, priorities);

        DummyTask task = new DummyTask(10);
        fixedQueue.offer(task);

        assertTrue("Invoking remove() on an existing task should return true", fixedQueue.remove(task));
        assertFalse("Invoking remove() on a non-existing task should return false", fixedQueue.remove(new DummyTask(10)));
        fixedQueue.clear();


        performOffer(unboundedQueue, puts, priorities);

        task = new DummyTask(10);
        unboundedQueue.offer(task);

        assertTrue("Invoking remove() on an existing task should return true", unboundedQueue.remove(task));
        assertFalse("Invoking remove() on a non-existing task should return false", unboundedQueue.remove(new DummyTask(10)));

        unboundedQueue.clear();
        assertEquals("Empty queue expected", 0, fixedQueue.size());
    }

    public void testPoll() {
        DummyTask task1 = new DummyTask(10);
        task1.setMark(3);
        fixedQueue.offer(task1);
        DummyTask task2 = new DummyTask(10);
        task2.setMark(4);
        fixedQueue.offer(task2);
        DummyTask dummyTask = (DummyTask) fixedQueue.poll();
        assertEquals("Invalid object returned, DummyTask with mark=3 expected", 3, dummyTask.getMark());
        fixedQueue.clear();
        assertEquals("Empty queue expected", 0, fixedQueue.size());
    }

    public void testPeek() {
        DummyTask task1 = new DummyTask(10);
        task1.setMark(3);
        fixedQueue.offer(task1);
        DummyTask task2 = new DummyTask(10);
        task2.setMark(4);
        fixedQueue.offer(task2);
        DummyTask dummyTask = (DummyTask) fixedQueue.peek();
        assertEquals("Invalid object returned, DummyTask with mark=3 expected", 3, dummyTask.getMark());
        fixedQueue.clear();
        assertEquals("Empty queue expected", 0, fixedQueue.size());
    }

    public void testDrain() {
        MultiPriorityBlockingQueue queue = createFixedQueue(2, new int[]{2, 2}, priorities);
        queue.offer(new DummyTask(10));
        queue.offer(new DummyTask(10));
        queue.offer(new DummyTask(1));
        queue.offer(new DummyTask(1));
        int noOfElements = queue.drainTo(new ArrayList());
        assertEquals("Invalid no of elements", 4, noOfElements);
    }

    public void testDrainGivenMax() {
        MultiPriorityBlockingQueue queue = createFixedQueue(2, new int[]{2, 2}, priorities);
        queue.offer(new DummyTask(10));
        queue.offer(new DummyTask(10));
        queue.offer(new DummyTask(1));
        queue.offer(new DummyTask(1));
        int noOfElementGivenMax = queue.drainTo(new ArrayList(), 3);
        assertEquals("Invalid no of elements", 3, noOfElementGivenMax);
    }

    public void testIsEmpty() {
        performOffer(fixedQueue, sizes, priorities);
        assertFalse("Non empty queue expected", fixedQueue.isEmpty());
        fixedQueue.clear();
        assertEquals("Empty queue expected", 0, fixedQueue.size());
    }

    public void testIsFixedSize() {
        MultiPriorityBlockingQueue queue1 = createFixedQueue(2, sizes, priorities);
        assertTrue("'queue1' should contain fixed queues", queue1.isFixedSizeQueues());
    }

    public void testToString() {
        MultiPriorityBlockingQueue queue1 = createFixedQueue(2, sizes, priorities);
        assertEquals("String representation is incorrect", "[]10[]1", queue1.toString());
    }

    private void waitForThreadState(Thread t, Thread.State state) {
        int count = 0;
        while (t.getState() != state && count < 10){
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) { }

            count++;
        }
    }

    private void performPut(MultiPriorityBlockingQueue queue,
                            int []items, int []priorites) throws InterruptedException {
        for (int priority = 0; priority < priorites.length; priority++) {
            for (int i = 0; i < items[priority]; i++) {
                queue.put(new DummyTask(priorites[priority]));
            }
        }
    }

    private void performOffer(MultiPriorityBlockingQueue queue,
                              int []items, int []priorites) {
        for (int priority = 0; priority < priorites.length; priority++) {
            for (int i = 0; i < items[priority]; i++) {
                queue.offer(new DummyTask(priorites[priority]));
            }
        }
    }

    private void performTake(MultiPriorityBlockingQueue queue, int items) {
        for (int i = 0; i < items; i++) {
            try {
                Object o = queue.take();
                assertNotNull("Object cannot be null", o);
            } catch (InterruptedException cannotOccur) {
                assertFalse("Cannot execute this", true);
            }
        }
    }
}
