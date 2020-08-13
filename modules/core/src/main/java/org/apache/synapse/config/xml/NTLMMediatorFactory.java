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
import org.apache.synapse.mediators.builtin.NTLMMediator;

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

    private static final QName TAG_NAME
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "NTLM");
    private static final QName ATT_USER_NAME = new QName("username");
    private static final QName ATT_PASSWORD = new QName("password");
    private static final QName ATT_DOMAIN = new QName("domain");
    private static final QName ATT_HOST = new QName("host");
    private static final QName ATT_NTLM_VERSION = new QName("ntlmVersion");

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
            ntlmMediator.setUsername(attUserName.getAttributeValue());
        }

        if (attPassword != null) {
            ntlmMediator.setPassword(attPassword.getAttributeValue());
        }

        if (attHost != null) {
            ntlmMediator.setHost(attHost.getAttributeValue());
        }

        if (attDomain != null) {
            ntlmMediator.setDomain(attDomain.getAttributeValue());
        }

        if (attVersion != null) {
            ntlmMediator.setNtlmVersion(attVersion.getAttributeValue());
        }
        return ntlmMediator;
    }

    @Override
    public QName getTagQName() {
        return TAG_NAME;
    }
}
