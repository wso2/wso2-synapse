/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse;

/**
 * Interface For Extended Synapse Handlers
 * <p/>
 * Synapse Handlers are invoked when a message received to the mediation engine or a message sent out
 * from the engine.
 * <p/>
 * When a message received to the engine, handles are invoked just before the mediation flow
 * When a message is sent out from the engine, handlers are invoked just after the mediation flow
 * There are two requests coming into the engine and two responses going out from the engine.
 * For all four messages relevant method is invoked
 */
public interface ExtendedSynapseHandler extends SynapseHandler {

    /**
     * Handle server startup.
     */
    public boolean handleServerInit();

    /**
     * Handle server shut down.
     */
    public boolean handleServerShutDown();

    /**
     * Handle artifact deployment.
     *
     * @param artifactName name of the artifact deployed
     * @param artifactType type of the artifact deployed
     * @param startTime    artifact deployed time
     * @return whether mediation flow should continue
     */
    public boolean handleArtifactDeployment(String artifactName, String artifactType, String startTime);

    /**
     * Handle artifact undeployment.
     *
     * @param artifactName name of the artifact deployed
     * @param artifactType type of the artifact deployed
     * @param unDeployTime artifact undeployment time
     * @return whether mediation flow should continue
     */
    public boolean handleArtifactUnDeployment(String artifactName, String artifactType, String unDeployTime);

    /**
     * Handle error requests coming to the synapse engine.
     *
     * @param synCtx outgoing response message context
     * @return whether mediation flow should continue
     */
    public boolean handleError(MessageContext synCtx);
}
