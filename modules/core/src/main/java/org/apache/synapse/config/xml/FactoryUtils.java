package org.apache.synapse.config.xml;

import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;

import java.util.HashMap;
import java.util.Properties;

public class FactoryUtils {

    public static final String TYPE_DATA_SERVICE = "DATA_SERVICE";
    public static final String TYPE_PROXY_SERVICE = "PROXY_SERVICE";

    /**
     * Constructs a fully qualified name for the artifact
     *
     * @param properties Properties containing the artifact identifier
     * @param name       Name of the artifact
     * @return fully qualified name in the format "artifactIdentifier/name"
     */
    public static String getFullyQualifiedName(Properties properties, String name) throws SynapseException {

        return getFullyQualifiedName(properties, name, null);
    }

    public static String getFullyQualifiedName(Properties properties, String name, String type) {

        if (isVersionedDeployment(properties)) {
            if (TYPE_DATA_SERVICE.equals(type) || TYPE_PROXY_SERVICE.equals(type)) {
                return getFullyQualifiedNameForServices(properties, name);
            }
            // IF the name contains '__', the artifact is from a dependency
            if (name.contains("__")) {
                String[] nameParts = name.split("__");
                if (nameParts.length < 3) {
                    throw new SynapseException("Invalid artifact reference name : " + name);
                }
                String artifactId = nameParts[0] + "__" + nameParts[1];
                HashMap<String, String> cAppDependencies = (HashMap<String, String>) properties.get(SynapseConstants.SYNAPSE_ARTIFACT_DEPENDENCIES);
                String version = cAppDependencies.get(artifactId);
                if (StringUtils.isNotBlank(version)) {
                    return artifactId + "__" + version + "__" + nameParts[2];
                } else {
                    throw new SynapseException("Cannot find the artifact version for : " + artifactId);
                }
            } else {
                // If the name does not contain '__', the artifact is from the current CApp
                String artifactIdentifier = (String) properties.get(SynapseConstants.SYNAPSE_ARTIFACT_IDENTIFIER);
                return StringUtils.isNotBlank(artifactIdentifier) ? artifactIdentifier + "__" + name : name;
            }
        }
        return name;
    }

    private static String getFullyQualifiedNameForServices(Properties properties, String name) {

        // DataServices and proxy services will have the following reference name format
        // groupID__artifactID/ServiceName
        if (name.contains("__")) {
            String[] nameParts = name.split("__");
            if (nameParts.length < 2 || !nameParts[1].contains("/")) {
                throw new SynapseException("Invalid artifact name : " + name);
            }
            String artifactId = nameParts[0] + "__" + nameParts[1].split("/")[0];
            HashMap<String, String> cAppDependencies = (HashMap<String, String>) properties.get(SynapseConstants.SYNAPSE_ARTIFACT_DEPENDENCIES);
            String version = cAppDependencies.get(artifactId);
            if (StringUtils.isNotBlank(version)) {
                return artifactId + "__" + version + "/" + nameParts[1].split("/")[1];
            } else {
                throw new SynapseException("Cannot find the artifact version for : " + artifactId);
            }
        } else {
            // If the name does not contain '__', the artifact is from the current CApp
            String artifactIdentifier = (String) properties.get(SynapseConstants.SYNAPSE_ARTIFACT_IDENTIFIER);
            return StringUtils.isNotBlank(artifactIdentifier) ? artifactIdentifier + "/" + name : name;
        }
    }

    public static String prependArtifactIdentifierToFileName(String resourcePath, String artifactIdentifier) {

        int lastSlash = resourcePath.lastIndexOf('/');
        if (lastSlash == -1) {
            // No directory structure, just prepend to filename
            return artifactIdentifier + "__" + resourcePath;
        }

        String path = resourcePath.substring(0, lastSlash + 1);   // directory part
        String fileName = resourcePath.substring(lastSlash + 1);  // file name part

        return path + artifactIdentifier + "__" + fileName;
    }

    public static boolean isVersionedDeployment(Properties properties) {

        Boolean isVersionedDeployment = (Boolean) properties.get(SynapseConstants.SYNAPSE_ARTIFACT_VERSIONED_DEPLOYMENT);
        return Boolean.TRUE.equals(isVersionedDeployment);
    }
}
