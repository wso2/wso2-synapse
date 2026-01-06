/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.unittest.testcase.data.classes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to hold mediator coverage information for a specific artifact.
 */
public class MediatorCoverage {

    private String artifactType;
    private String artifactName;
    private int executedMediators;
    private int totalMediators;
    private double coveragePercentage;
    private List<String> mediatorDetails;
    private Map<String, Boolean> mediatorExecutionStatus;

    public MediatorCoverage() {
        this.mediatorDetails = new ArrayList<>();
        this.mediatorExecutionStatus = new LinkedHashMap<>();
        this.executedMediators = 0;
        this.totalMediators = 0;
        this.coveragePercentage = 0.0;
    }

    public MediatorCoverage(String artifactType, String artifactName) {
        this();
        this.artifactType = artifactType;
        this.artifactName = artifactName;
    }

    /**
     * Get artifact type (API or Sequence).
     *
     * @return artifact type
     */
    public String getArtifactType() {
        return artifactType;
    }

    /**
     * Set artifact type.
     *
     * @param artifactType artifact type
     */
    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    /**
     * Get artifact name.
     *
     * @return artifact name
     */
    public String getArtifactName() {
        return artifactName;
    }

    /**
     * Set artifact name.
     *
     * @param artifactName artifact name
     */
    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    /**
     * Get number of executed mediators.
     *
     * @return executed mediators count
     */
    public int getExecutedMediators() {
        return executedMediators;
    }

    /**
     * Set number of executed mediators.
     *
     * @param executedMediators executed mediators count
     */
    public void setExecutedMediators(int executedMediators) {
        this.executedMediators = executedMediators;
    }

    /**
     * Get total number of mediators.
     *
     * @return total mediators count
     */
    public int getTotalMediators() {
        return totalMediators;
    }

    /**
     * Set total number of mediators.
     *
     * @param totalMediators total mediators count
     */
    public void setTotalMediators(int totalMediators) {
        this.totalMediators = totalMediators;
    }

    /**
     * Get coverage percentage.
     *
     * @return coverage percentage
     */
    public double getCoveragePercentage() {
        return coveragePercentage;
    }

    /**
     * Set coverage percentage.
     *
     * @param coveragePercentage coverage percentage
     */
    public void setCoveragePercentage(double coveragePercentage) {
        this.coveragePercentage = coveragePercentage;
    }

    /**
     * Get list of mediator details (identifiers).
     *
     * @return mediator details list
     */
    public List<String> getMediatorDetails() {
        return mediatorDetails;
    }

    /**
     * Set mediator details list.
     *
     * @param mediatorDetails mediator details list
     */
    public void setMediatorDetails(List<String> mediatorDetails) {
        this.mediatorDetails = mediatorDetails;
    }

    /**
     * Add a mediator detail identifier.
     *
     * @param mediatorIdentifier mediator identifier
     */
    public void addMediatorDetail(String mediatorIdentifier) {
        if (!this.mediatorDetails.contains(mediatorIdentifier)) {
            this.mediatorDetails.add(mediatorIdentifier);
        }
    }

    /**
     * Add mediator with execution status.
     *
     * @param mediatorIdentifier mediator identifier
     * @param executed whether the mediator was executed
     */
    public void addMediatorExecutionStatus(String mediatorIdentifier, boolean executed) {
        this.mediatorExecutionStatus.put(mediatorIdentifier, executed);
    }

    /**
     * Get mediator execution status map.
     *
     * @return map of mediator identifier to execution status
     */
    public Map<String, Boolean> getMediatorExecutionStatus() {
        return mediatorExecutionStatus;
    }

    /**
     * Calculate coverage percentage based on executed and total mediators.
     */
    public void calculateCoveragePercentage() {
        if (totalMediators > 0) {
            this.coveragePercentage = ((double) executedMediators / totalMediators) * 100.0;
        } else {
            this.coveragePercentage = 0.0;
        }
    }

    /**
     * Convert to JSON object.
     *
     * @return JSON object representation
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("artifactType", artifactType);
        json.addProperty("artifactName", artifactName);
        json.addProperty("executedMediators", executedMediators);
        json.addProperty("totalMediators", totalMediators);
        json.addProperty("coveragePercentage", String.format("%.2f", coveragePercentage));

        // Add mediator details with execution status
        JsonArray detailsArray = new JsonArray();
        for (Map.Entry<String, Boolean> entry : mediatorExecutionStatus.entrySet()) {
            JsonObject mediatorObj = new JsonObject();
            mediatorObj.addProperty("mediatorId", entry.getKey());
            mediatorObj.addProperty("executed", entry.getValue());
            detailsArray.add(mediatorObj);
        }
        json.add("mediatorDetails", detailsArray);

        return json;
    }
}
