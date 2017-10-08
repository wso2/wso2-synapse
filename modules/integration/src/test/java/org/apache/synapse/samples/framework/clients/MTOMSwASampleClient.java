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

import org.apache.axiom.attachments.Attachments;
import org.apache.axiom.om.*;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.SampleClientResult;
import org.apache.synapse.samples.framework.config.Axis2ClientConfiguration;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.namespace.QName;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MTOMSwASampleClient {

    private static final Log log = LogFactory.getLog(StockQuoteSampleClient.class);

    SampleClientResult clientResult;
    OMElement payload;
    ServiceClient serviceClient;
    Axis2ClientConfiguration configuration;

    public MTOMSwASampleClient(Axis2ClientConfiguration configuration) {
        this.configuration = configuration;
    }

    public SampleClientResult sendUsingMTOM(String fileName, String targetEPR) {
        clientResult = new SampleClientResult();
        try {
            OMFactory factory = OMAbstractFactory.getOMFactory();
            OMNamespace ns = factory.createOMNamespace("http://services.samples", "m0");
            payload = factory.createOMElement("uploadFileUsingMTOM", ns);
            OMElement request = factory.createOMElement("request", ns);
            OMElement image = factory.createOMElement("image", ns);

            log.info("Sending file : " + fileName + " as MTOM");
            FileDataSource fileDataSource = new FileDataSource(new File(fileName));
            DataHandler dataHandler = new DataHandler(fileDataSource);
            OMText textData = factory.createOMText(dataHandler, true);
            image.addChild(textData);
            request.addChild(image);
            payload.addChild(request);

            ConfigurationContext configContext =
                    ConfigurationContextFactory.
                            createConfigurationContextFromFileSystem(configuration.getClientRepo(),
                                    configuration.getAxis2Xml());

            serviceClient = new ServiceClient(configContext, null);

            Options options = new Options();
            options.setTo(new EndpointReference(targetEPR));
            options.setAction("urn:uploadFileUsingMTOM");
            options.setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);

            serviceClient.setOptions(options);
            OMElement response = serviceClient.sendReceive(payload);

            OMText binaryNode = (OMText) response.
                    getFirstChildWithName(new QName("http://services.samples", "response")).
                    getFirstChildWithName(new QName("http://services.samples", "image")).
                    getFirstOMChild();
            dataHandler = (DataHandler) binaryNode.getDataHandler();
            InputStream is = dataHandler.getInputStream();
            log.info("temp.dir: " + System.getProperty("java.io.tmpdir"));
            File tempFile = File.createTempFile("mtom-", ".gif");
            FileOutputStream fos = new FileOutputStream(tempFile);
            BufferedOutputStream dest = new BufferedOutputStream(fos, 2048);

            byte data[] = new byte[2048];
            int count;
            while ((count = is.read(data, 0, 2048)) != -1) {
                dest.write(data, 0, count);
            }

            dest.flush();
            dest.close();
            log.info("Saved response to file : " + tempFile.getAbsolutePath());
            clientResult.incrementResponseCount();
        } catch (Exception e) {
            log.error("Error invoking service", e);
            clientResult.setException(e);
        }

        return clientResult;

    }

    public SampleClientResult sendUsingSWA(String fileName, String targetEPR) {
        clientResult = new SampleClientResult();
        try {
            Options options = new Options();
            options.setTo(new EndpointReference(targetEPR));
            options.setAction("urn:uploadFileUsingSwA");
            options.setProperty(Constants.Configuration.ENABLE_SWA, Constants.VALUE_TRUE);

            ConfigurationContext configContext =
                    ConfigurationContextFactory.
                            createConfigurationContextFromFileSystem(configuration.getClientRepo(),
                                    configuration.getAxis2Xml());

            ServiceClient sender = new ServiceClient(configContext, null);

            sender.setOptions(options);
            OperationClient mepClient = sender.createClient(ServiceClient.ANON_OUT_IN_OP);

            MessageContext mc = new MessageContext();

            log.info("Sending file : " + fileName + " as SwA");
            FileDataSource fileDataSource = new FileDataSource(new File(fileName));
            DataHandler dataHandler = new DataHandler(fileDataSource);
            String attachmentID = mc.addAttachment(dataHandler);


            SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
            SOAPEnvelope env = factory.getDefaultEnvelope();
            OMNamespace ns = factory.createOMNamespace("http://services.samples", "m0");
            OMElement payload = factory.createOMElement("uploadFileUsingSwA", ns);
            OMElement request = factory.createOMElement("request", ns);
            OMElement imageId = factory.createOMElement("imageId", ns);
            imageId.setText(attachmentID);
            request.addChild(imageId);
            payload.addChild(request);
            env.getBody().addChild(payload);
            mc.setEnvelope(env);

            mepClient.addMessageContext(mc);
            mepClient.execute(true);
            MessageContext response = mepClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);

            SOAPBody body = response.getEnvelope().getBody();
            String imageContentId = body.
                    getFirstChildWithName(new QName("http://services.samples", "uploadFileUsingSwAResponse")).
                    getFirstChildWithName(new QName("http://services.samples", "response")).
                    getFirstChildWithName(new QName("http://services.samples", "imageId")).
                    getText();

            Attachments attachment = response.getAttachmentMap();
            dataHandler = attachment.getDataHandler(imageContentId);
            File tempFile = File.createTempFile("swa-", ".gif");
            FileOutputStream fos = new FileOutputStream(tempFile);
            dataHandler.writeTo(fos);
            fos.flush();
            fos.close();

            log.info("Saved response to file : " + tempFile.getAbsolutePath());
            clientResult.incrementResponseCount();
        } catch (Exception e) {
            log.error("Error invoking service", e);
            clientResult.setException(e);
        }

        return clientResult;

    }
}
