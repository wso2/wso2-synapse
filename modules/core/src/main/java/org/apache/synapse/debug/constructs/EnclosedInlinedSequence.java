/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.debug.constructs;

import org.apache.synapse.Mediator;
import org.apache.synapse.config.SynapseConfiguration;

/**
 * This interface has been introduced due to following reasons. There are couple of mediators
 * which are resides in Synapse Extension ans Carbon Mediation packages. Those packages have explicit
 * dependency on Synapse Core. That will create rather impossible to access those mediators inside
 * Synapse core since we don't have any explicit dependency from other packages to Synapse Core.
 * ( that will create cyclic dependency ) Since the debug level implementations resides in Synapse core.
 * This interface can be used to in such cases where we want to explicitly specify a behavior we when we
 * reference those mediators at runtime from Synapse Core Package. Currently there is NO ABSTRACT LEVEL
 * behavior defined for mediators which have multiple inline sequence that will create issues when
 * leveraging debugging capabilities those mediator's inlined sequences. The methods defined here are kept away
 * from AbstractMediator for reason. That inlined sequence definitions are not common to all mediators. In other
 * cases current implementation of mediators do not follow unified method of implementation of these
 * mediators which have inlined mediators. That creates MediatorTreeTraverseUtil class implementation complex
 * since there is no generic implementation have to include each every mediator custom way that will add
 * unnecessary complexity.
 */
public interface EnclosedInlinedSequence {

    /**
     * Return the Inlined Sequence Associated with Identifier in that interface implemented Mediator.
     *
     * @param inlineSeqIndentifier developer may specify the Identifier associated with each Inlined Sequence
     *                             in case of multiple inlined sequences
     * @return Inlined Sequence Mediator associated with identifier
     */
    Mediator getInlineSequence(SynapseConfiguration synCfg, int inlineSeqIndentifier);

}
