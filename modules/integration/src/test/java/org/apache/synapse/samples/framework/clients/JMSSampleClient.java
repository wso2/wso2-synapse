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

package org.apache.synapse.samples.framework.clients;

import javax.jms.*;
import javax.naming.InitialContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class JMSSampleClient {

    private QueueConnection connection;
    private QueueSession session;
    private QueueSender sender;

    public void connect(String destination) throws Exception {

        Properties env = new Properties();
        String connectionFactoryName = "ConnectionFactory";

        if (System.getProperty("java.naming.provider.url") == null) {
            env.put("java.naming.provider.url", "tcp://localhost:61616");
        }
        if (System.getProperty("java.naming.factory.initial") == null) {
            env.put("java.naming.factory.initial",
                "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        }
        env.put("transport.jms.ConnectionFactoryJNDIName", connectionFactoryName);

        InitialContext ic = new InitialContext(env);
        QueueConnectionFactory confac = (QueueConnectionFactory) ic.lookup(connectionFactoryName);
        connection = confac.createQueueConnection();
        session = connection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
        sender = session.createSender((Queue)ic.lookup(destination));
    }

    public void shutdown() throws Exception {
        sender.close();
        session.close();
        connection.close();
    }

    private void sendBytesMessage(byte[] payload) throws Exception {
        BytesMessage bm = session.createBytesMessage();
        bm.writeBytes(payload);
        sender.send(bm);
    }

    public void sendTextMessage(String payload) throws Exception {
        TextMessage tm = session.createTextMessage(payload);
        sender.send(tm);
    }

    public static byte[] getBytesFromFile(String fileName) throws IOException {

        File file = new File(fileName);
        InputStream is = new FileInputStream(file);
        long length = file.length();

        byte[] bytes = new byte[(int) length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
            && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        is.close();
        return bytes;
    }

    private double getRandom(double base, double variance, boolean positiveOnly) {
        double rand = Math.random();
        return (base + ((rand > 0.5 ? 1 : -1) * variance * base * rand))
            * (positiveOnly ? 1 : (rand > 0.5 ? 1 : -1));
    }

    public void sendAsPox(String symbol) throws Exception {
        sendTextMessage(
                "<m:placeOrder xmlns:m=\"http://services.samples\">\n" +
                "    <m:order>\n" +
                "        <m:price>" + getRandom(100, 0.9, true) + "</m:price>\n" +
                "        <m:quantity>" + (int) getRandom(10000, 1.0, true) + "</m:quantity>\n" +
                "        <m:symbol>" + symbol + "</m:symbol>\n" +
                "    </m:order>\n" +
                "</m:placeOrder>");
    }

}
