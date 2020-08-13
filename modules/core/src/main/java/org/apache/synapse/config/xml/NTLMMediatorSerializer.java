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

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.NTLMMediator;

/**
 * Serializer for {@link NTLMMediator} instances.
 *
 * <pre>
 * &lt;NTLM username="string" | password="string" | host="string" | domain="string" | ntlmVersion="string"&gt;
 * &lt;/NTLM&gt;
 * </pre>
 */
public class NTLMMediatorSerializer extends AbstractMediatorSerializer {

    @Override
    protected OMElement serializeSpecificMediator(Mediator m) {
        if (!(m instanceof NTLMMediator)) {
            handleException("Unsupported mediator passed in for serialization : " + m.getType());
        }

        NTLMMediator mediator = (NTLMMediator) m;
        OMElement ntlmMediator = fac.createOMElement("NTLM", synNS);
        saveTracingState(ntlmMediator, mediator);

        if (mediator.getUsername() != null) {
            ntlmMediator.addAttribute(fac.createOMAttribute("username", nullNS, mediator.getUsername()));
        }

        if (mediator.getPassword() != null) {
            ntlmMediator.addAttribute(fac.createOMAttribute("password", nullNS, mediator.getPassword()));
        }
        if (mediator.getHost() != null) {
            ntlmMediator.addAttribute(fac.createOMAttribute("host", nullNS, mediator.getHost()));
        }

        if (mediator.getDomain() != null) {
            ntlmMediator.addAttribute(fac.createOMAttribute("domain", nullNS, mediator.getDomain()));
        }

        if (mediator.getNtlmVersion() != null) {
            ntlmMediator.addAttribute(fac.createOMAttribute("ntlmVersion", nullNS, mediator.getNtlmVersion()));
        }

        return ntlmMediator;
    }

    @Override
    public String getMediatorClassName() {
        return NTLMMediator.class.getName();
    }
}
