/*
 Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.unittest;

import javafx.util.Pair;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;

import static org.apache.synapse.unittest.Constants.*;

public class TestCasesMediator {

    private static Logger logger = Logger.getLogger(UnitTestingExecutor.class.getName());

    static Pair<Boolean, MessageContext> sequenceMediate(String inputXmlPayload, SynapseConfiguration synConfig, String key){
        Mediator sequenceMediator = synConfig.getSequence(key);
        MessageContext msgCtxt = createSynapseMessageContext(inputXmlPayload, synConfig);

        boolean mediationResult = sequenceMediator.mediate(msgCtxt);

        return new Pair<>(mediationResult, msgCtxt);
    }

    static String proxyServiceExecutor(String inputXmlPayload, String key){
        String responseOfRevoke = null;
        try {

            String url = "http://localhost:8280/services/" + key;
            logger.info("Invoking URI - "+url);

            HttpClient client = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader(HTTP.CONTENT_TYPE, "text/xml");
            StringEntity postStringEntity = new StringEntity(inputXmlPayload);
            httpPost.setEntity(postStringEntity);
            HttpResponse response = client.execute(httpPost);

            int responseCode = response.getStatusLine().getStatusCode();
            if(responseCode == 200){
                responseOfRevoke = EntityUtils.toString(response.getEntity(), "UTF-8");
            }else{
                responseOfRevoke = "failed";
            }
        }

        catch (Exception e) {
            logger.error("Exception in invoking the proxy service", e);
        }

        return responseOfRevoke;
    }

    static String apiResourceExecutor(String inputXmlPayload, String context, String resourceMethod){
        String responseOfRevoke = null;
        try {

            String url = "http://localhost:8280" + context;
            logger.info("Invoking URI - "+url);

            HttpClient clientConnector = new DefaultHttpClient();
            HttpResponse response;

            switch (resourceMethod.toUpperCase()) {
                case GET_METHOD :
                    HttpGet httpGet = new HttpGet(url);
                    httpGet.setHeader(HTTP.CONTENT_TYPE, "text/xml");
                    response = clientConnector.execute(httpGet);
                    break;

                case POST_METHOD :
                    HttpPost httpPost = new HttpPost(url);
                    StringEntity postEntity = new StringEntity(inputXmlPayload);
                    httpPost.setEntity(postEntity);
                    response = clientConnector.execute(httpPost);
                    break;

                case PUT_METHOD :
                    HttpPut httpPut = new HttpPut(url);
                    StringEntity putEntity = new StringEntity(inputXmlPayload);
                    httpPut.setEntity(putEntity);
                    response = clientConnector.execute(httpPut);
                    break;

                case DELETE_METHOD :
                    HttpDelete httpDelete = new HttpDelete(url);
                    response = clientConnector.execute(httpDelete);
                    break;

                 default :
                     httpGet = new HttpGet(url);
                     httpGet.setHeader(HTTP.CONTENT_TYPE, "text/xml");
                     response = clientConnector.execute(httpGet);
                     break;
            }

            int responseCode = response.getStatusLine().getStatusCode();
            if(responseCode == 200){
                responseOfRevoke = EntityUtils.toString(response.getEntity(), "UTF-8");
            }else{
                responseOfRevoke = "failed";
            }

        } catch (Exception e) {
            logger.error("Exception in invoking the api resource", e);
        }

        return responseOfRevoke;
    }

    private static MessageContext createSynapseMessageContext(String payload, SynapseConfiguration config){

        MessageContext synMc = null;
        try{
            org.apache.axis2.context.MessageContext mc =
                    new org.apache.axis2.context.MessageContext();
            AxisConfiguration axisConfig = config.getAxisConfiguration();
            if (axisConfig == null) {
                axisConfig = new AxisConfiguration();
                config.setAxisConfiguration(axisConfig);
            }
            ConfigurationContext cfgCtx = new ConfigurationContext(axisConfig);
            SynapseEnvironment env = new Axis2SynapseEnvironment(cfgCtx, config);

//            mc.setConfigurationContext(cfgCtx);
//            mc.setOperationContext(new OperationContext());
//            mc.setAxisOperation(new InOutAxisOperation(SynapseConstants.SYNAPSE_OPERATION_NAME));

            synMc = new Axis2MessageContext(mc, config, env);
            SOAPEnvelope envelope =
                    OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
            OMDocument omDoc =
                    OMAbstractFactory.getSOAP11Factory().createOMDocument();
            omDoc.addChild(envelope);

            envelope.getBody().addChild(createOMElement(payload));

            synMc.setEnvelope(envelope);
        }catch (Exception e){
            logger.error(e);
        }

        return synMc;
    }

    public static OMElement createOMElement(String xml) {
        return SynapseConfigUtils.stringToOM(xml);
    }

}
