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

package org.apache.synapse.debug;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * main class that handles communication channels instances to outside debugger
 */
public class SynapseDebugInterface {

    private int DEBUG_PORT_LISTEN;
    private int DEBUG_PORT_SEND;
    private ServerSocket PORT_LISTEN; //communication channels tcp ports
    private ServerSocket PORT_SEND;
    private PrintWriter PORT_SEND_WRITER;
    private BufferedReader PORT_LISTEN_READER;
    private PrintWriter PORT_LISTEN_WRITER;
    private static final Log log = LogFactory.getLog(SynapseDebugInterface.class);
    private static SynapseDebugInterface debugInterfaceInstance=null;

    /**
     *initializes the communication command and event channels using
     * @param DEBUG_PORT_LISTEN command port number
     * @param DEBUG_PORT_SEND event port number
     */
    public void init(int DEBUG_PORT_LISTEN,int DEBUG_PORT_SEND)  {
        log.info(SynapseDebugInterfaceConstants.LISTEN_ON_PORTS+" :"+DEBUG_PORT_LISTEN+" + "+DEBUG_PORT_SEND);
      try{
        this.DEBUG_PORT_LISTEN=DEBUG_PORT_LISTEN;
        this.DEBUG_PORT_SEND=DEBUG_PORT_SEND;
        PORT_LISTEN=new ServerSocket(DEBUG_PORT_LISTEN);
        PORT_SEND=new ServerSocket(DEBUG_PORT_SEND);
        Socket SOCKET_LISTEN = PORT_LISTEN.accept(); //socket timeout check
        Socket SOCKET_SEND = PORT_SEND.accept();
        log.info(SynapseDebugInterfaceConstants.CONNECTION_CREATED);
        PORT_SEND_WRITER = new PrintWriter(SOCKET_SEND.getOutputStream());
        PORT_LISTEN_READER = new BufferedReader(new InputStreamReader(SOCKET_LISTEN.getInputStream()));
        PORT_LISTEN_WRITER = new PrintWriter(SOCKET_LISTEN.getOutputStream());
      }catch (IOException e){

      }
    }

    public static SynapseDebugInterface getInstance() {
        if(debugInterfaceInstance == null) {
            debugInterfaceInstance = new SynapseDebugInterface();
        }
        return debugInterfaceInstance;
    }

    public void closeConnection(){
       try{
           if(getOpenedPortListen()!=null){
           getOpenedPortListen().close();
           }
           if(getOpenedPortSend()!=null){
            getOpenedPortSend().close();
           }
           log.info(SynapseDebugInterfaceConstants.CONNECTION_CLOSED);
       }catch(IOException e){
       }

    }
    public ServerSocket getOpenedPortListen(){
        return PORT_LISTEN;
    }
    public ServerSocket getOpenedPortSend(){
        return PORT_SEND;
    }
    public PrintWriter getPortSendWriter(){
        return PORT_SEND_WRITER;
    }
    public BufferedReader getPortListenReader(){
        return PORT_LISTEN_READER;
    }
    public PrintWriter getPortListenWriter(){
        return PORT_LISTEN_WRITER;
    }


}
