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

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.builtin.CommentMediator;

import javax.xml.namespace.QName;
import java.util.Properties;

/**
 * Factory for Comment Mediators.
 */
public class CommentMediatorFactory extends AbstractMediatorFactory {

    private static final QName Comment_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "comment");

    /**
     * Create Comment Mediator instance using provided XML Element and properties
     *
     * @param elem       configuration element describing the properties of the mediator
     * @param properties bag of properties to pass in any information to the factory
     *
     * @return CommentMediator instance created with the given comment text
     */
    @Override
    protected Mediator createSpecificMediator(OMElement elem, Properties properties) {
        CommentMediator commentMediator = new CommentMediator();
        commentMediator.setCommentText(elem.getText());
        return commentMediator;
    }

    /**
     * Returns Tag QName for the comment mediator
     *
     * @return QName of the mediator
     */
    public QName getTagQName() {
        return Comment_Q;
    }
}