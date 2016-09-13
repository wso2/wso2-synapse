/**
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.aspects.flow.statistics.data.artifact;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.aspects.flow.statistics.structuring.StructuringElement;

import java.util.ArrayList;
import java.util.Stack;

/**
 * This class is to create object which holds properties related to a single artifact
 */
public class ArtifactHolder {

    private Log log = LogFactory.getLog(ArtifactHolder.class);

    private int id = 0;

    private int hashCode = 0;

    private String parent;

    private ArrayList<StructuringElement> list = new ArrayList<>();

    private Stack<StructuringElement> stack = new Stack<>();

    private String lastParent;

    private boolean exitFromBox = false;

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getIdString() {
        return String.valueOf(id++);
    }

    public String getHashCodeAsString() {
        if (log.isDebugEnabled()) {
            log.debug("Hash Code Given to the component is :" + hashCode);
        }
        return String.valueOf(hashCode);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    public int getHashCode() {
        return hashCode;
    }

    public String getParent() {
        return parent;
    }

    public ArrayList<StructuringElement> getList() {
        return list;
    }

    public void setList(
            ArrayList<StructuringElement> list) {
        this.list = list;
    }

    public Stack<StructuringElement> getStack() {
        return stack;
    }

    public void setStack(Stack<StructuringElement> stack) {
        this.stack = stack;
    }

    public String getLastParent() {
        return lastParent;
    }

    public void setLastParent(String lastParent) {
        this.lastParent = lastParent;
    }

    public boolean getExitFromBox() {
        return exitFromBox;
    }

    public void setExitFromBox(boolean exitFromBox) {
        this.exitFromBox = exitFromBox;
    }
}
