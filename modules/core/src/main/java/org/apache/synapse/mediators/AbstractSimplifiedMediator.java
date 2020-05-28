/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.synapse.mediators;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.util.SimpleMessageContext;
import org.apache.synapse.mediators.util.exceptions.SimpleMessageContextException;

/**
 * A simplified version of AbstractMediator. This can be used to create new mediator classes to run in Class mediator
 * . AbstractExtendedMediator provides an abstract mediate method which provides a SimpleMessageContext object, which
 * can be used to perform data transformation tasks easily
 */
public abstract class AbstractSimplifiedMediator extends AbstractMediator {

    protected AbstractSimplifiedMediator() {

        super();
    }

    @Override
    public final boolean mediate(MessageContext messageContext) {

        try {
            mediate(new SimpleMessageContext(messageContext));
            return true;
        } catch (RuntimeException e) {
            getLog(messageContext).error(e);
            return false;
        }
    }

    /**
     * mediate method with more capabilities than the parent method. No need to return boolean feedback when there is
     * an error. Error tracking is done by an overloaded method.
     * Throw SimpleMessageContextException to stop mediation flow due to an error.
     *
     * @param messageContext SimpleMessageContext instance which provides methods for work with payloads and data
     *                       transformation.
     */
    public abstract void mediate(SimpleMessageContext messageContext);
}
