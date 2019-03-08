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
import org.apache.axiom.om.OMElement;
import org.apache.log4j.Logger;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.json.JSONObject;

import javax.xml.namespace.QName;
import java.util.ArrayList;

import static org.apache.synapse.unittest.Constants.*;

/**
 * Class responsible for receiving artifact data and test data
 * Initiate TCP server
 * Connects with unit test client
 */
public class TestingAgent{

    private static Logger logger = Logger.getLogger(TestingAgent.class.getName());
    private static SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
    private static String artifactType = null;
    private static String key = null;
    private static String context = null;
    private static String resourceMethod = null;
    /**
     * Start method of test agent
     */
    public void startServer(){
        TCPServer tcpConnection = new TCPServer();
        tcpConnection.initialize(7777);
    }

    public static boolean processArtifact(JSONObject receivedMessage){
        artifactType = MessageDecoder.getArtifactType(receivedMessage);
        String artifactName = MessageDecoder.getArtifactName(receivedMessage);
        OMElement artifact = MessageDecoder.getConfigurationArtifact(receivedMessage);
        boolean isArtifactDeployed = false;

        if(artifactType.equals(TYPE_SEQUENCE)){
            try{
                ConfigurationDeployer config = new ConfigurationDeployer();
                Pair<SynapseConfiguration, String> pair = config.deploySequenceArtifact(artifact, artifactName);
                synapseConfiguration = pair.getKey();
                key = pair.getValue();

            }catch (Exception e){
                logger.error("Sequence deployment failed");
            }

            try {
                if(key.equals(artifactName)){
                    isArtifactDeployed = true;
                    logger.info("Sequence artifact deployed successfully");
                }else{
                    logger.error("Sequence deployment failed");
                }
            }catch (Exception e){
                logger.error(e);
            }

        }else if(artifactType.equals(TYPE_PROXY)){
            try{
                ConfigurationDeployer config = new ConfigurationDeployer();
                Pair<SynapseConfiguration, String> pair = config.deployProxyArtifact(artifact, artifactName);
                synapseConfiguration = pair.getKey();
                key = pair.getValue();

            }catch (Exception e){
                logger.error("Proxy deployment failed");
            }

            try {
                if(key.equals(artifactName)){
                    isArtifactDeployed = true;
                    logger.info("Proxy artifact deployed successfully");
                }else{
                    logger.error("Proxy deployment failed");
                }
            }catch (Exception e){
                logger.error("Proxy deployment failed",e);
            }
        }else if(artifactType.equals(TYPE_API)){
            try {
                ConfigurationDeployer config = new ConfigurationDeployer();
                Pair<SynapseConfiguration, String> pair = config.deployApiArtifact(artifact, artifactName);
                synapseConfiguration = pair.getKey();
                key = pair.getValue();
                context = artifact.getAttributeValue(new QName(API_CONTEXT));
                resourceMethod = artifact.getFirstElement().getAttributeValue(new QName(RESOURCE_METHODS));

                try {
                    if (key.equals(artifactName)) {
                        isArtifactDeployed = true;
                        logger.info("API artifact deployed successfully");
                    } else{
                        logger.error("API deployment failed");
                    }
                }catch (Exception e){
                    logger.error(e);
                }

            }catch (Exception e){
                logger.error("API deployment failed", e);
            }
        }else{
            logger.error("Undefined operation type in unit testing agent");
        }

        return isArtifactDeployed;
    }

    public static JSONObject processTestCases(JSONObject receivedMessage){
        boolean isAssert = false;
        JSONObject resultOfTestCases = new JSONObject();
        int testCaseCount = MessageDecoder.getTestCasesCount(receivedMessage);
        ArrayList<ArrayList<String>> testCasesData;
        testCasesData = MessageDecoder.receivedMessage(receivedMessage);

        logger.info(testCaseCount+" Test case(s) ready to execute");

        //execute test cases with synapse configurations and test data
        for(int i=0; i < testCaseCount; i++) {

            if(artifactType.equals(TYPE_SEQUENCE)){
                Pair<Boolean, MessageContext> mediateResult =
                        TestCasesMediator.sequenceMediate(testCasesData.get(i).get(0), synapseConfiguration, key);
                Boolean mediationResult = mediateResult.getKey();
                MessageContext resultedMessageContext = mediateResult.getValue();

                //check whether mediation is success or not
                if(mediationResult){
                    isAssert = Assert.doAssertionSequence
                            (testCasesData.get(i).get(1), testCasesData.get(i).get(2), resultedMessageContext, i+1);

                }else{
                    logger.error("Sequence mediation failed");
                }

            }else if(artifactType.equals(TYPE_PROXY)){
                String invokedResult = TestCasesMediator.proxyServiceExecutor(testCasesData.get(i).get(0), key);

                if(!invokedResult.equals("failed")){
                    isAssert = Assert.doAssertionService(testCasesData.get(i).get(1), invokedResult, i+1);

                }else{
                    logger.error("Proxy service invoke failed");
                }

            }else if(artifactType.equals(TYPE_API)){

                String invokedResult = TestCasesMediator.apiResourceExecutor(testCasesData.get(i).get(0), context, resourceMethod);

                if(!invokedResult.equals("failed")){
                    isAssert = Assert.doAssertionService(testCasesData.get(i).get(1), invokedResult, i+1);

                }else{
                    logger.error("API resource invoke failed");
                }
            }else{
                break;
            }

            resultOfTestCases.put("test-case "+(i+1), isAssert);
        }

        return resultOfTestCases;
    }
}
