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
package org.apache.synapse.transport.http.conn;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.http.nio.reactor.IOSession;

class Wire {

    private IOSession session;

    private final Log log;

    public Wire(final Log log) {
        super();
        this.log = log;
    }

    private void wire(final String header, final byte[] b, int pos, int off) {
        StringBuilder buffer = new StringBuilder();
        StringBuilder tmpBuffer = new StringBuilder();
        StringBuilder synapseBuffer = new StringBuilder();
        for (int i = 0; i < off; i++) {
            int ch = b[pos + i] & 0xFF;
            if (ch == 13) {
                buffer.append("[\\r]");
            } else if (ch == 10) {
                    tmpBuffer.setLength(0);
                    tmpBuffer.append(buffer.toString());
                    tmpBuffer.insert(0, header);
                    buffer.append("[\\n]\"");
                    buffer.insert(0, "\"");
                    buffer.insert(0, header);
                    if (isEnabled()) {
                        this.log.debug(Thread.currentThread().getName() + " " + buffer.toString());
                    }
//                    this.log.debug(buffer.toString());
                    synapseBuffer.append(tmpBuffer.toString());
                    synapseBuffer.append(System.lineSeparator());
                    buffer.setLength(0);
            } else if ((ch < 32) || (ch > 127)) {
                buffer.append("[0x");
                buffer.append(Integer.toHexString(ch));
                buffer.append("]");
            } else {
                buffer.append((char) ch);
            }
        } 
        if (buffer.length() > 0) {
            buffer.append('\"');
            buffer.insert(0, '\"');
            buffer.insert(0, header);
            if (isEnabled()) {
                this.log.debug(Thread.currentThread().getName() + " " + buffer.toString());
            }
            tmpBuffer.setLength(0);
            tmpBuffer.append(buffer.toString());
            synapseBuffer.append(tmpBuffer.toString());
        }
        if (synapseBuffer.length() > 0 && SynapseDebugInfoHolder.getInstance().isDebuggerEnabled()) {
            //an IOsession get create for new request when there is already a request getting debugged
            SynapseWireLogHolder logHolder;
            Object holder = this.session.getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY);
            if (holder == null) {
                logHolder = new SynapseWireLogHolder();
            } else {
                logHolder = (SynapseWireLogHolder) holder;
                if (logHolder.getPhase().equals(SynapseWireLogHolder.PHASE.SOURCE_RESPONSE_DONE)) {
                    logHolder.clear();
                }
            }
            if (logHolder.getPhase().equals(SynapseWireLogHolder.PHASE.SOURCE_REQUEST_READY)) { //this means this is initial request
                this.log.debug("source request wire Log added to the log holder, phase - " + logHolder.getPhase().toString());
                logHolder.appendRequestWireLog(synapseBuffer.toString());
            } else if (logHolder.getPhase().equals(SynapseWireLogHolder.PHASE.TARGET_REQUEST_READY)) { //this means this is a back end call
                String mediatorId = (String)this.session.getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_MEDIATOR_ID_PROPERTY);
                if (mediatorId == null || mediatorId.isEmpty()) {
                    mediatorId = SynapseDebugInfoHolder.DUMMY_MEDIATOR_ID;
                }
                this.log.debug("wire Log added to the log holder, phase - " + logHolder.getPhase().toString() + ", mediatorId - " + mediatorId);
                logHolder.appendBackEndWireLog(SynapseWireLogHolder.RequestType.REQUEST, synapseBuffer.toString(), mediatorId);
            } else if (logHolder.getPhase().equals(SynapseWireLogHolder.PHASE.TARGET_RESPONSE_READY)) { //this means this is a response back from back end
                String mediatorId = (String)this.session.getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_MEDIATOR_ID_PROPERTY);
                if (mediatorId == null || mediatorId.isEmpty()) {
                    mediatorId = SynapseDebugInfoHolder.DUMMY_MEDIATOR_ID;
                }
                this.log.debug("wire Log added to the log holder, phase - " + logHolder.getPhase().toString() + ", mediatorId - " + mediatorId);
                logHolder.appendBackEndWireLog(SynapseWireLogHolder.RequestType.RESPONSE, synapseBuffer.toString(), mediatorId);
            } else if (logHolder.getPhase().equals(SynapseWireLogHolder.PHASE.SOURCE_RESPONSE_READY)) { //this means this is the final response to client
                this.log.debug("source response wire Log added to the log holder, phase - " + logHolder.getPhase().toString());
                logHolder.appendResponseWireLog(synapseBuffer.toString());
            }
            this.session.setAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY, logHolder);
        }
    }


    public boolean isEnabled() {
        return this.log.isDebugEnabled();
    }    
    
    public void output(final byte[] b, int pos, int off) {
        wire("<< ", b, pos, off);
    }

    public void input(final byte[] b, int pos, int off) {
        wire(">> ", b, pos, off);
    }

    public void output(byte[] b) {
        output(b, 0, b.length);
    }

    public void input(byte[] b) {
        input(b, 0, b.length);
    }

    public void output(int b) {
        output(new byte[] {(byte) b});
    }

    public void input(int b) {
        input(new byte[] {(byte) b});
    }

    public void output(final ByteBuffer b) {
        if (b.hasArray()) {
            output(b.array(), b.arrayOffset() + b.position(), b.remaining());
        } else {
            byte[] tmp = new byte[b.remaining()];
            b.get(tmp);
            output(tmp);
        }
    }

    public void input(final ByteBuffer b) {
        if (b.hasArray()) {
            input(b.array(), b.arrayOffset() + b.position(), b.remaining());
        } else {
            byte[] tmp = new byte[b.remaining()];
            b.get(tmp);
            input(tmp);
        }
    }

    public void setSession(IOSession session) {
        this.session = session;
    }
}
