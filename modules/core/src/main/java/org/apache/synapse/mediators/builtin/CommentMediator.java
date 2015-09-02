/*
* Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.synapse.mediators.builtin;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * Mediator to handle XML comment nodes in Synapse Configurations
 */
public class CommentMediator extends AbstractMediator {

    private String commentText;

    /**
     * Mediation method. This will not do any tasks in CommentMediator
     *
     * @param synCtx the current message for mediation     *
     * @return true since remaining mediators should be executed
     */
    public boolean mediate(MessageContext synCtx) {
        return true;
    }

    /**
     * Set Comment Text
     *
     * @param commentText Text value of the comment to be set for the mediator
     */
    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    /**
     * Returns Comment text
     *
     * @return Comment text value associated with the mediator
     */
    public String getCommentText() {
        return commentText;
    }

    @Override
    public boolean isContentAware() {
        return false;
    }
}