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

package org.apache.synapse.mediators.transaction;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.transaction.TranscationManger;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import javax.transaction.TransactionManager;
import java.util.Hashtable;

/**
 * The Mediator for commit, rollback, suspend, resume jta transactions
 */
public class TransactionMediator extends AbstractMediator {

    public static final String ACTION_COMMIT = "commit";
    public static final String ACTION_ROLLBACK = "rollback";
    public static final String ACTION_NEW = "new";
    public static final String ACTION_USE_EXISTING_OR_NEW = "use-existing-or-new";
    public static final String ACTION_FAULT_IF_NO_TX = "fault-if-no-tx";
    private static final String USER_TX_LOOKUP_STR = "java:comp/UserTransaction";
    private static final String USER_TX_LOOKUP_STR1 = "java:comp/TransactionManager";
    private Context txContext;

    private String action = "";



    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

       // UserTransaction tx = null;
        final SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Transaction mediator (" + action + ")");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }
        initContext(synCtx);
        try {
            //tx = (UserTransaction) txContext.lookup(USER_TX_LOOKUP_STR);
            TranscationManger.lookUp(txContext);
        } catch (Exception e) {
            handleException("Cloud not get the context name " + USER_TX_LOOKUP_STR, e, synCtx);
        }

        if (action.equals(ACTION_COMMIT)) {

            try {
                //tx.commit();
            	long key = Thread.currentThread().getId();
            	TranscationManger.endTransaction(true, key);
            } catch (Exception e) {
                handleException("Unable to commit transaction", e, synCtx);
            }

        } else if (action.equals(ACTION_ROLLBACK)) {

            try {
                //tx.rollback();
            	long key = Thread.currentThread().getId();
            	TranscationManger.rollbackTransaction(true, key);
            } catch (Exception e) {
                handleException("Unable to rollback transaction", e, synCtx);
            }

        } else if (action.equals(ACTION_NEW)) {

            int status = Status.STATUS_UNKNOWN;
            try {
               // status = tx.getStatus();
            	status = TranscationManger.getStatus();
            } catch (Exception e) {
                handleException("Unable to query transaction status", e, synCtx);
            }

            if (!(status == Status.STATUS_NO_TRANSACTION || status == Status.STATUS_UNKNOWN)) {
                throw new SynapseException("Require to begin a new transaction, " +
                        "but a transaction already exist");
            }

            try {
                //tx.begin();
            	TranscationManger.beginTransaction();
                org.apache.axis2.context.MessageContext axis2MsgCtx =
                        ((Axis2MessageContext)synCtx).getAxis2MessageContext();
                axis2MsgCtx.setProperty(NhttpConstants.DISTRIBUTED_TRANSACTION,TranscationManger.getTransaction());
                axis2MsgCtx.setProperty(NhttpConstants.DISTRIBUTED_TRANSACTION_MANAGER,TranscationManger.getTransactionManager());
            } catch (Exception e) {
                handleException("Unable to begin a new transaction", e, synCtx);
            }

        } else if (action.equals(ACTION_USE_EXISTING_OR_NEW)) {

            int status = Status.STATUS_UNKNOWN;
            try {
               // status = tx.getStatus();
            	status = TranscationManger.getStatus();
            } catch (Exception e) {
                handleException("Unable to query transaction status", e, synCtx);
            }

            try {
                if (status == Status.STATUS_NO_TRANSACTION) {
                   // tx.begin();
                }
            } catch (Exception e) {
                handleException("Unable to begin a new transaction", e, synCtx);
            }

        } else if (action.equals(ACTION_FAULT_IF_NO_TX)) {

            int status = Status.STATUS_UNKNOWN;
            try {
                //status = tx.getStatus();
            	status = TranscationManger.getStatus();
            } catch (Exception e) {
                handleException("Unable to query transaction status", e, synCtx);
            }

            if (status != Status.STATUS_ACTIVE)
                throw new SynapseException("No active transaction. Require an active transaction");

        } else {
            handleException("Invalid transaction mediator action : " + action, synCtx);
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("End : Transaction mediator");
        }

        return true;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    private void initContext(MessageContext synCtx) {
        try {
            txContext = new InitialContext();
        } catch (NamingException e) {
            handleException("Cloud not create initial context", e, synCtx);
        }
    }

    @Override
    public boolean isContentAware() {
        return false;
    }
}
