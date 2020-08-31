/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.builtin.NTLMMediator;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.Properties;
import javax.xml.namespace.QName;

/**
 * Factory for {@link NTLMMediator} instances.
 *
 * <pre>
 * &lt;NTLM username="string" | password="string" | host="string" | domain="string" | ntlmVersion="string"&gt;
 * &lt;/NTLM&gt;
 * </pre>
 */
public class NTLMMediatorFactory extends AbstractMediatorFactory {

    private static final String USERNAME_ATTRIBUTE_NAME = "username";
    private static final String PASSWORD_ATTRIBUTE_NAME = "password";
    private static final String DOMAIN_ATTRIBUTE_NAME = "domain";
    private static final String HOST_ATTRIBUTE_NAME = "host";
    private static final String NTLM_VERSION_ATTRIBUTE_NAME = "ntlmVersion";

    private static final QName TAG_NAME
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "NTLM");
    private static final QName ATT_USER_NAME = new QName(USERNAME_ATTRIBUTE_NAME);
    private static final QName ATT_PASSWORD = new QName(PASSWORD_ATTRIBUTE_NAME);
    private static final QName ATT_DOMAIN = new QName(DOMAIN_ATTRIBUTE_NAME);
    private static final QName ATT_HOST = new QName(HOST_ATTRIBUTE_NAME);
    private static final QName ATT_NTLM_VERSION = new QName(NTLM_VERSION_ATTRIBUTE_NAME);

    @Override
    protected Mediator createSpecificMediator(OMElement elem, Properties properties) {

        NTLMMediator ntlmMediator = new NTLMMediator();
        processAuditStatus(ntlmMediator, elem);

        OMAttribute attUserName = elem.getAttribute(ATT_USER_NAME);
        OMAttribute attPassword = elem.getAttribute(ATT_PASSWORD);
        OMAttribute attHost = elem.getAttribute(ATT_HOST);
        OMAttribute attDomain = elem.getAttribute(ATT_DOMAIN);
        OMAttribute attVersion = elem.getAttribute(ATT_NTLM_VERSION);

        if (attUserName != null) {
            String userName = attUserName.getAttributeValue();
            ntlmMediator.setUsername(userName);
            //Check the NTLM username dynamic or not
            if (isDynamicAttribute(userName)) {
                ntlmMediator.setDynamicUsername(createValueObject(userName, USERNAME_ATTRIBUTE_NAME, elem));
            }
        }

        if (attPassword != null) {
            String password = attPassword.getAttributeValue();
            ntlmMediator.setPassword(password);
            //Check the NTLM password dynamic or not
            if (isDynamicAttribute(password)) {
                ntlmMediator.setDynamicPassword(createValueObject(password, PASSWORD_ATTRIBUTE_NAME, elem));
            }
        }

        if (attHost != null) {
            String host = attHost.getAttributeValue();
            ntlmMediator.setHost(host);
            //Check the NTLM password dynamic or not
            if (isDynamicAttribute(host)) {
                ntlmMediator.setDynamicHost(createValueObject(host, HOST_ATTRIBUTE_NAME, elem));
            }
        }

        if (attDomain != null) {
            String domain = attDomain.getAttributeValue();
            ntlmMediator.setDomain(domain);
            //Check the NTLM password dynamic or not
            if (isDynamicAttribute(domain)) {
                ntlmMediator.setDynamicDomain(createValueObject(domain, DOMAIN_ATTRIBUTE_NAME, elem));
            }
        }

        if (attVersion != null) {
            String ntlmVersion = attVersion.getAttributeValue();
            ntlmMediator.setNtlmVersion(ntlmVersion);
            //Check the NTLM password dynamic or not
            if (isDynamicAttribute(ntlmVersion)) {
                ntlmMediator.setDynamicNtmlVersion(createValueObject(ntlmVersion, NTLM_VERSION_ATTRIBUTE_NAME, elem));
            }
        }
        return ntlmMediator;
    }

    @Override
    public QName getTagQName() {
        return TAG_NAME;
    }

    /**
     * Validate the given attribute to identify whether it is static or dynamic key
     * If the name is in the {} format then it is dynamic key(XPath)
     * Otherwise just a static name
     *
     * @param attributeValue string to validate as the attribute
     * @return isDynamicAttribute representing the attribute type
     */
    private boolean isDynamicAttribute(String attributeValue) {
        if (attributeValue.length() < 2) {
            return false;
        }

        final char startExpression = '{';
        final char endExpression = '}';

        char firstChar = attributeValue.charAt(0);
        char lastChar = attributeValue.charAt(attributeValue.length() - 1);

        return (startExpression == firstChar && endExpression == lastChar);
    }

    /**
     * Creates a value object for the dynamic expression
     *
     * @param attributeValue the string value of the expression
     * @param attributeName  the name that need to give to the value object
     * @param element        the OMElement of the configuration
     */
    private Value createValueObject(String attributeValue, String attributeName, OMElement element) {
        try {
            String nameExpression = attributeValue.substring(1, attributeValue.length() - 1);
            if (nameExpression.startsWith("json-eval(")) {
                new SynapseJsonPath(nameExpression.substring(10, nameExpression.length() - 1));
            } else {
                new SynapseXPath(nameExpression);
            }
        } catch (JaxenException e) {
            String msg = "Invalid expression for attribute '" + attributeName + "' : " + attributeValue;
            log.error(msg);
            throw new SynapseException(msg);
        }
        // ValueFactory for creating dynamic Value
        ValueFactory nameValueFactory = new ValueFactory();
        // create dynamic Value based on OMElement
        return nameValueFactory.createValue(attributeName, element);
    }
}
