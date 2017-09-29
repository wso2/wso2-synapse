/*
 * Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.mediators.transaction;

import junit.framework.Assert;
import org.apache.synapse.config.xml.TransactionMediatorFactory;
import org.apache.synapse.config.xml.TransactionMediatorSerializer;
import org.apache.synapse.mediators.AbstractMediatorTestCase;

import java.util.Properties;

/**
 * Test for Transaction mediator creation with TransactionMediatorFactory, OMElement creation with
 * TransactionMediatorSerializer, and action getter/setter methods
 */
public class TransactionMediatorTestCase extends AbstractMediatorTestCase {

    private static final String NEW_ACTION = "new";
    private static final String COMMIT_ACTION = "commit";
    private static final String ROLLBACK_ACTION = "rollback";
    private static final String FAULT_NOTX_ACTION = "fault-if-no-tx";
    private static final String BEGIN_MEDIATOR = "<transaction xmlns=\"http://ws.apache.org/ns/synapse\" action=";
    private static final String END_MEDIATOR = " />";
    private static final String QUOT_MARK = "\"";

    private static TransactionMediator newActionMediator;
    private static TransactionMediator commitActionMediator;
    private static TransactionMediator rollbackActionMediator;
    private static TransactionMediator faultActionMediator;

    public void setUp() {
        newActionMediator = (TransactionMediator) new TransactionMediatorFactory().createSpecificMediator(
                createOMElement(BEGIN_MEDIATOR + QUOT_MARK + NEW_ACTION + QUOT_MARK + END_MEDIATOR), new Properties());
        commitActionMediator = (TransactionMediator) new TransactionMediatorFactory().createSpecificMediator(
                createOMElement(BEGIN_MEDIATOR + QUOT_MARK + COMMIT_ACTION + QUOT_MARK + END_MEDIATOR),
                new Properties());

        rollbackActionMediator = new TransactionMediator();
        rollbackActionMediator.setAction(ROLLBACK_ACTION);

        faultActionMediator = new TransactionMediator();
        faultActionMediator.setAction(FAULT_NOTX_ACTION);
    }

    /**
     * Test for transaction mediator creation using TransactionFactory mediator
     *
     * @throws Exception
     */
    public void testTransactionMediatorCreation() throws Exception {
        Assert.assertEquals("Creation of transaction mediation fails with new action. ", NEW_ACTION,
                            newActionMediator.getAction());
        Assert.assertEquals("Creation of transaction mediation fails with commit action. ", COMMIT_ACTION,
                            commitActionMediator.getAction());
    }

    /**
     * Test for set action method
     *
     * @throws Exception
     */
    public void testSetActionMethod() throws Exception {
        Assert.assertEquals("Set action fails for rollback action in transaction mediator.", ROLLBACK_ACTION,
                            rollbackActionMediator.getAction());
        Assert.assertEquals("Set action fails for fault-if-no-tx action in transaction mediator.", FAULT_NOTX_ACTION,
                            faultActionMediator.getAction());
    }

    /**
     * Test for creation of OMElement of Transaction mediator
     *
     * @throws Exception
     */
    public void testTransactionMediatorOMElementCreation() throws Exception {
        TransactionMediatorSerializer transactionMediatorSerializer = new TransactionMediatorSerializer();

        Assert.assertEquals("OMElement creation fails with rollback action in transaction mediator.",
                            BEGIN_MEDIATOR + QUOT_MARK + ROLLBACK_ACTION + QUOT_MARK + END_MEDIATOR,
                            transactionMediatorSerializer.serializeSpecificMediator(rollbackActionMediator).toString());
        Assert.assertEquals("OMElement creation fails with fault-if-no-tx action in transaction mediator.",
                            BEGIN_MEDIATOR + QUOT_MARK + FAULT_NOTX_ACTION + QUOT_MARK + END_MEDIATOR,
                            transactionMediatorSerializer.serializeSpecificMediator(faultActionMediator).toString());
    }
}
