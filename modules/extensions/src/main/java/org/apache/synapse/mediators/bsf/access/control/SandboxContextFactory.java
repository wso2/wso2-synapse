/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.bsf.access.control;

import org.apache.synapse.mediators.bsf.access.control.config.AccessControlConfig;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

/**
 * Represents the sandbox context factory - which is used with access control of the Script Mediator.
 */
public class SandboxContextFactory extends ContextFactory {
    private AccessControlConfig nativeObjectAccessControlConfig;

    public SandboxContextFactory(AccessControlConfig nativeObjectAccessControlConfig) {
        this.nativeObjectAccessControlConfig = nativeObjectAccessControlConfig;
    }

    @Override
    protected Context makeContext() {
        Context cx = super.makeContext();
        cx.setWrapFactory(new SandboxWrapFactory(nativeObjectAccessControlConfig));
        return cx;
    }
}

