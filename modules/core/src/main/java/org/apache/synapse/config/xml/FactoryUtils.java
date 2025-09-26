/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;

import java.util.HashMap;
import java.util.Properties;

public class FactoryUtils {

    public static final String DOUBLE_UNDERSCORE = "__";
    public static final String TYPE_DATA_SERVICE = "DATA_SERVICE";
    public static final String TYPE_PROXY_SERVICE = "PROXY_SERVICE";

    /**
     * Constructs a fully qualified name for the artifact.
     *
     * @param properties Properties containing the artifact identifier
     * @param name       Name of the artifact
     * @return fully qualified name in the format "artifactIdentifier__name"
     */
    public static String getFullyQualifiedName(Properties properties, String name) throws SynapseException {

        return getFullyQualifiedName(properties, name, null);
    }

    /**
     * Constructs a fully qualified name for the artifact.
     *
     * @param properties Properties containing the artifact identifier
     * @param name       Name of the artifact
     * @param type       Type of the artifact
     * @return fully qualified name in the format "artifactIdentifier__name"
     */
    public static String getFullyQualifiedName(Properties properties, String name, String type) {

        if (isVersionedDeployment(properties)) {
            if (TYPE_DATA_SERVICE.equals(type) || TYPE_PROXY_SERVICE.equals(type)) {
                return getFullyQualifiedNameForServices(properties, name);
            }
            // IF the name contains '__', the artifact is from a dependency
            if (name.contains(DOUBLE_UNDERSCORE)) {
                String[] nameParts = name.split(DOUBLE_UNDERSCORE);
                if (nameParts.length < 3) {
                    throw new SynapseException("Invalid artifact reference name : " + name);
                }
                String artifactId = nameParts[0] + DOUBLE_UNDERSCORE + nameParts[1];
                HashMap<String, String> cAppDependencies = (HashMap<String, String>) properties.get(SynapseConstants.SYNAPSE_ARTIFACT_DEPENDENCIES);
                String version = cAppDependencies.get(artifactId);
                if (StringUtils.isNotBlank(version)) {
                    return artifactId + DOUBLE_UNDERSCORE + version + DOUBLE_UNDERSCORE + nameParts[2];
                } else {
                    throw new SynapseException("Cannot find the artifact version for : " + artifactId);
                }
            } else {
                // If the name does not contain '__', the artifact is from the current CApp
                String artifactIdentifier = (String) properties.get(SynapseConstants.SYNAPSE_ARTIFACT_IDENTIFIER);
                return StringUtils.isNotBlank(artifactIdentifier) ? artifactIdentifier + DOUBLE_UNDERSCORE + name : name;
            }
        }
        return name;
    }

    /**
     * Constructs a fully qualified name for DataServices and Proxy services.
     *
     * @param properties Properties containing the artifact identifier
     * @param name       Name of the artifact
     * @return fully qualified name in the format "artifactIdentifier/name"
     */
    private static String getFullyQualifiedNameForServices(Properties properties, String name) {

        // DataServices and proxy services will have the following reference name format
        // groupID__artifactID/ServiceName
        if (name.contains(DOUBLE_UNDERSCORE)) {
            String[] nameParts = name.split(DOUBLE_UNDERSCORE);
            if (nameParts.length < 2 || !nameParts[1].contains("/")) {
                throw new SynapseException("Invalid artifact name : " + name);
            }
            String artifactId = nameParts[0] + DOUBLE_UNDERSCORE + nameParts[1].split("/")[0];
            HashMap<String, String> cAppDependencies = (HashMap<String, String>) properties.get(SynapseConstants.SYNAPSE_ARTIFACT_DEPENDENCIES);
            String version = cAppDependencies.get(artifactId);
            if (StringUtils.isNotBlank(version)) {
                return artifactId + DOUBLE_UNDERSCORE + version + "/" + nameParts[1].split("/")[1];
            } else {
                throw new SynapseException("Cannot find the artifact version for : " + artifactId);
            }
        } else {
            // If the name does not contain '__', the artifact is from the current CApp
            String artifactIdentifier = (String) properties.get(SynapseConstants.SYNAPSE_ARTIFACT_IDENTIFIER);
            return StringUtils.isNotBlank(artifactIdentifier) ? artifactIdentifier + "/" + name : name;
        }
    }

    /**
     * Prepend the artifact identifier to the file name in the resource path if versioned deployment is
     * enabled.
     *
     * @param resourcePath The original resource path
     * @param properties   Properties containing the artifact identifier and dependencies
     * @return The modified resource path with the artifact identifier prepended to the file name
     */
    public static String prependArtifactIdentifierToFileName(String resourcePath, Properties properties) {

        if (isVersionedDeployment(properties)) {
            // IF the path contains '__', the artifact is from a dependency
            if (resourcePath.contains(DOUBLE_UNDERSCORE)) {
                int lastSlash = resourcePath.lastIndexOf('/');
                String dirPath = resourcePath.substring(0, lastSlash + 1);
                String resourceName = resourcePath.substring(lastSlash + 1);

                String[] nameParts = resourceName.split(DOUBLE_UNDERSCORE);
                if (nameParts.length < 3) {
                    throw new SynapseException("Invalid resource reference name : " + resourcePath);
                }
                String artifactId = nameParts[0] + DOUBLE_UNDERSCORE + nameParts[1];
                HashMap<String, String> cAppDependencies = (HashMap<String, String>) properties.get(SynapseConstants.SYNAPSE_ARTIFACT_DEPENDENCIES);
                String version = cAppDependencies.get(artifactId);
                if (StringUtils.isNotBlank(version)) {
                    return dirPath + artifactId + DOUBLE_UNDERSCORE + version + DOUBLE_UNDERSCORE + nameParts[2];
                } else {
                    throw new SynapseException("Cannot find the resource version for : " + artifactId);
                }
            } else {
                // If the path does not contain '__', the artifact is from the current CApp
                String artifactIdentifier = (String) properties.get(SynapseConstants.SYNAPSE_ARTIFACT_IDENTIFIER);
                int lastSlash = resourcePath.lastIndexOf('/');
                String path = resourcePath.substring(0, lastSlash + 1);
                String fileName = resourcePath.substring(lastSlash + 1);
                return path + artifactIdentifier + DOUBLE_UNDERSCORE + fileName;
            }
        }
        return resourcePath;
    }

    /**
     * Check whether versioned deployment is enabled.
     *
     * @param properties Properties containing the versioned deployment flag
     * @return true if versioned deployment is enabled, false otherwise
     */
    public static boolean isVersionedDeployment(Properties properties) {

        if (properties != null) {
            Boolean isVersionedDeployment = (Boolean) properties.get(SynapseConstants.SYNAPSE_ARTIFACT_VERSIONED_DEPLOYMENT);
            return Boolean.TRUE.equals(isVersionedDeployment);
        }
        return false;
    }
}
