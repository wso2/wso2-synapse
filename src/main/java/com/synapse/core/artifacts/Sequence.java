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

package com.synapse.core.artifacts;

import com.synapse.core.artifacts.utils.Position;
import com.synapse.core.synctx.MsgContext;

import java.util.List;

public class Sequence implements Mediator {
    private List<Mediator> mediatorList;
    private Position position;
    private String name;

    public Sequence(List<Mediator> mediatorList, Position position, String name) {
        this.mediatorList = mediatorList;
        this.position = position;
        this.name = name;
    }

    public Sequence() {

    }

    @Override
    public boolean execute(MsgContext context) {
        for (Mediator mediator : mediatorList) {
            try {
                boolean result = mediator.execute(context);
                if (!result) {
                    return false;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return true;
    }

    // Getters and Setters
    public List<Mediator> getMediatorList() {
        return mediatorList;
    }

    public void setMediatorList(List<Mediator> mediatorList) {
        this.mediatorList = mediatorList;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
