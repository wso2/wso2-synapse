/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.v2.ext.AbstractClassMediator;

public class TestClassMediatorV2Impl extends AbstractClassMediator {

    public boolean mediate(MessageContext context,
                           @Arg(name = "username", type = ArgumentType.STRING) String username,
                           @Arg(name = "age", type = ArgumentType.INTEGER) Integer age,
                           @Arg(name = "confirmed", type = ArgumentType.BOOLEAN) Boolean confirmed) {
        // TODO Implement your mediation logic here
        return true;
    }
}
