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

package org.apache.synapse.config.xml.comments;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.AbstractListMediatorSerializer;
import org.apache.synapse.mediators.comment.CommentMediator;

public class CommentMediatorSerializer extends AbstractListMediatorSerializer {

    /**
     * This is a dummy serializer where it never used. Comment Mediator serialization happens at
     * AbstractListMediatorSerializer's serializeMediator method.
     * @param m mediator to be serialized
     * @return null
     */
    public OMElement serializeSpecificMediator(Mediator m) {
       return null;
    }

    /**
     * Returns Comment Mediator Class name
     * @return Comment Mediator Class name
     */
    public String getMediatorClassName() {
        return CommentMediator.class.getName();
    }

}
