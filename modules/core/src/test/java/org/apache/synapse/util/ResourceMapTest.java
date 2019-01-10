/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.util;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.util.resolver.ResourceMap;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.xml.sax.InputSource;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Testing resolving capability of ResourceMap class
 */
public class ResourceMapTest extends TestCase {
    @Mock
    SynapseConfiguration synapseConfiguration;

    @Mock
    MessageContext messageContext;

    @Mock
    OMElement omElement;

    @Mock
    Value value;

    @Mock
    SynapsePath synapsePath;

    private final String helloLocation = "/helloLocation";
    private final String helloKey = "conf:/hello.xsd";

    public void setUp() throws Exception {
        initMocks(this);
    }

    public void testResolveFromMap() throws Exception {
        setUp();

        ResourceMap resourceMap = new ResourceMap();
        resourceMap.addResource(new Value(helloLocation), new Value(helloKey));
        PowerMockito.when(synapseConfiguration.getEntry(anyString())).thenReturn(omElement);
        InputSource inputSource = resourceMap.resolve(synapseConfiguration, helloLocation, null);
        Assert.assertNotNull("Stream should not be null", inputSource.getByteStream());
        Assert.assertEquals("Values should be equal",
                            inputSource.getSystemId(),
                            "synapse-reg:///" + helloKey);
    }

    public void testResolveFromMapExpression() {
        ResourceMap resourceMap = new ResourceMap();
        resourceMap.addResource(new Value(helloLocation), new Value(synapsePath));
        PowerMockito.when(synapsePath.getExpression()).thenReturn(helloKey);
        PowerMockito.when(synapseConfiguration.getEntry(anyString())).thenReturn(omElement);
        InputSource inputSource = resourceMap.resolve(synapseConfiguration, helloLocation, null);
        Assert.assertNotNull("Stream should not be null", inputSource.getByteStream());
        Assert.assertEquals("Values should be equal",
                            inputSource.getSystemId(),
                            "synapse-reg:///" + helloKey);
    }

    public void testResolveFromMessageContext() {
        ResourceMap resourceMap = new ResourceMap();
        resourceMap.addResource(new Value(helloLocation), value);
        PowerMockito.when(synapseConfiguration.getEntry(anyString())).thenReturn(omElement);
        PowerMockito.when(value.evaluateValue(((MessageContext) anyObject()))).thenReturn(helloKey);
        InputSource inputSource = resourceMap.resolve(synapseConfiguration, helloLocation, messageContext);
        Assert.assertNotNull("Stream should not be null", inputSource.getByteStream());
        Assert.assertEquals("Values should be equal",
                            inputSource.getSystemId(),
                            "synapse-reg:///" + helloKey);
    }
}