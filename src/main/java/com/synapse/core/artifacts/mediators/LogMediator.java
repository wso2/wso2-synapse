/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.synapse.core.artifacts.mediators;

import com.synapse.core.artifacts.Mediator;
import com.synapse.core.artifacts.utils.Position;
import com.synapse.core.synctx.MsgContext;

public class LogMediator implements Mediator {
    private String category;
    private String message;
    private Position position;

    public LogMediator(String category, String message, Position position) {
        this.category = category;
        this.message = message;
        this.position = position;
    }

    public LogMediator() {

    }

    @Override
    public boolean execute(MsgContext context) {
        // Log the message
        System.out.println(category + " : " + message);
        return true;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}
