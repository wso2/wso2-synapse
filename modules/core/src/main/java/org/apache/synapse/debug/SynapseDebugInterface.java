/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.debug;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.debug.constants.SynapseDebugInterfaceConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

/**
 * This is the main class that handles communication channels to the outside debugger. Mainly
 * handles connection creation to receive and send debug commands and to send debug related events
 * to the outside debugger. Channel creation happens asynchronously in a separate thread,
 * but initialization of ESB server runtime ( mediation initializer ) is paused until a successful
 * creation of communication channel.
 */
public class SynapseDebugInterface {

    private int listernPortNumber;
    private int sendPortNumber;
    private ServerSocket listenSocket;
    private ServerSocket sendSocket;
    private PrintWriter sendSocketWriter;
    private BufferedReader listenSocketReader;
    private PrintWriter listenSocketWriter;
    private static final Log log = LogFactory.getLog(SynapseDebugInterface.class);
    private static SynapseDebugInterface debugInterfaceInstance = null;
    private Semaphore runtimeSuspensionSem;
    private static final int SOCKET_TIMEOUT_INTERVAL = 60 * 1000;
    private volatile Exception uncaughtException;

    /**
     * Initializes the communication command and event channels asynchronously.
     *
     * @param listenPortParam command port number
     * @param sendPortParam   event port number
     */
    public void init(final int listenPortParam, final int sendPortParam)
            throws InterruptedException, IOException {
        log.info(SynapseDebugInterfaceConstants.LISTEN_ON_PORTS + " : Command "
                + listenPortParam + " - Event " + sendPortParam);
        this.runtimeSuspensionSem = new Semaphore(0);
        Thread channelCreator = new Thread(new AsynchronousChannelCreator
                (listenPortParam, sendPortParam, runtimeSuspensionSem));
        channelCreator.start();
        this.runtimeSuspensionSem.acquire();
        if (uncaughtException != null) {
            throw (IOException) uncaughtException;
        }
    }

    public void createDebugChannels(final int listenPortParam, final int sendPortParam)
            throws IOException {
        this.listernPortNumber = listenPortParam;
        this.sendPortNumber = sendPortParam;
        this.listenSocket = new ServerSocket(listernPortNumber);
        this.sendSocket = new ServerSocket(sendPortNumber);
        this.listenSocket.setSoTimeout(SOCKET_TIMEOUT_INTERVAL);
        this.sendSocket.setSoTimeout(SOCKET_TIMEOUT_INTERVAL);
        Socket listenClientSocket = this.listenSocket.accept();
        Socket sendClientSocket = this.sendSocket.accept();
        log.info(SynapseDebugInterfaceConstants.CONNECTION_CREATED);
        this.sendSocketWriter = new PrintWriter(sendClientSocket.getOutputStream());
        this.listenSocketReader = new BufferedReader(
                new InputStreamReader(listenClientSocket.getInputStream()));
        this.listenSocketWriter = new PrintWriter(listenClientSocket.getOutputStream());
    }

    public static SynapseDebugInterface getInstance() {
        if (debugInterfaceInstance == null) {
            debugInterfaceInstance = new SynapseDebugInterface();
        }
        return debugInterfaceInstance;
    }

    /**
     * Closes the communication and event channels
     */
    public void closeConnection() {
        try {
            if (getOpenedPortListen() != null && !getOpenedPortListen().isClosed()) {
                getOpenedPortListen().close();
            }
            if (getOpenedPortSend() != null && !getOpenedPortListen().isClosed()) {
                getOpenedPortSend().close();
            }
            log.info(SynapseDebugInterfaceConstants.CONNECTION_CLOSED);
        } catch (IOException e) {
            log.error("Failed close communication channels to the external debugger", e);
        }

    }

    public ServerSocket getOpenedPortListen() {
        return listenSocket;
    }

    public ServerSocket getOpenedPortSend() {
        return this.sendSocket;
    }

    public PrintWriter getPortSendWriter() {
        return this.sendSocketWriter;
    }

    public BufferedReader getPortListenReader() {
        return this.listenSocketReader;
    }

    public PrintWriter getPortListenWriter() {
        return this.listenSocketWriter;
    }

    public void setUncaughtException(Exception ex) {
        this.uncaughtException = ex;
    }

    class AsynchronousChannelCreator implements Runnable {

        private int listenPortParam;
        private int sendPortParam;
        private Semaphore runtimeSuspendSem;

        public AsynchronousChannelCreator(int listenPortParam, int sendPortParam,
                                          Semaphore runtimeSuspendSem) {
            this.listenPortParam = listenPortParam;
            this.sendPortParam = sendPortParam;
            this.runtimeSuspendSem = runtimeSuspendSem;
        }

        @Override
        public void run() {
            try {
                createDebugChannels(listenPortParam, sendPortParam);
            } catch (IOException ex) {
                setUncaughtException(ex);
                log.error("Failed create communication channels to the external debugger", ex);
            } finally {
                runtimeSuspendSem.release();
            }
        }
    }

}
