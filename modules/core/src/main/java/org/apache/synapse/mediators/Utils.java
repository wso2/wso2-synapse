package org.apache.synapse.mediators;

import java.io.File;

public class Utils {

    public static final String RESOURCES_IDENTIFIER = "resources:";
    public static final String CONVERTED_RESOURCES_IDENTIFIER = "gov:mi-resources" + File.separator;

    public static String transformFileKey(String fileKey) {

        if (fileKey.startsWith(RESOURCES_IDENTIFIER)) {
            return CONVERTED_RESOURCES_IDENTIFIER + fileKey.substring(RESOURCES_IDENTIFIER.length());
        }
        return fileKey;
    }
}
