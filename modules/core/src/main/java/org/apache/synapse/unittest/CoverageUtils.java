/*
 * Copyright (c) 2026, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.unittest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.base.SequenceMediator;

/**
 * Util class to handle coverage tracking during unit tests.
 */
public class CoverageUtils {

    private static final Log log = LogFactory.getLog(CoverageUtils.class);

    /**
     * Handle coverage tracking when executing a referenced sequence during unit tests.
     *
     * @param synCtx message context
     * @param mediator the mediator being executed (should be SequenceMediator)
     * @param sequenceKey the key used to reference the sequence (can be registry path or sequence name)
     * @return the original artifact key (to be restored after sequence execution), or null if not in unit test mode
     */
    public static String handleCoverageForReferencedSequence(MessageContext synCtx, 
                                                             Mediator mediator, 
                                                             String sequenceKey) {
        if (!isUnitTestMode(synCtx)) {
            return null;
        }

        // Save original artifact key
        String originalKey = (String) synCtx.getProperty(Constants.COVERAGE_ARTIFACT_KEY);

        if (mediator instanceof SequenceMediator) {
            SequenceMediator refSeq = (SequenceMediator) mediator;

            String refSeqName = sequenceKey != null ? sequenceKey : refSeq.getName();
            
            if (refSeqName != null && !refSeqName.isEmpty()) {
                registerSequenceIfNotRegistered(refSeq, refSeqName);
                
                // Set coverage artifact key for tracking execution in referenced sequence
                String artifactKey = "Sequence:" + refSeqName;
                synCtx.setProperty(Constants.COVERAGE_ARTIFACT_KEY, artifactKey);
            }
        }
        
        return originalKey;
    }

    /**
     * Handle coverage tracking for a template during unit tests.
     * Sets the COVERAGE_ARTIFACT_KEY to track execution inside the template.
     *
     * @param synCtx message context
     * @param templateName the name of the template being invoked
     * @return the original artifact key (to be restored after template execution), or null if not in unit test mode
     */
    public static String handleCoverageForTemplate(MessageContext synCtx, String templateName) {
        // Check if running in unit test mode
        if (!isUnitTestMode(synCtx)) {
            return null;
        }

        // Save original artifact key
        String originalKey = (String) synCtx.getProperty(Constants.COVERAGE_ARTIFACT_KEY);

        if (templateName != null && !templateName.isEmpty()) {
            String templateKey = "Template:" + templateName;
            synCtx.setProperty(Constants.COVERAGE_ARTIFACT_KEY, templateKey);
        }

        return originalKey;
    }

    /**
     * Restore the original coverage artifact key after executing a referenced sequence.
     *
     * @param synCtx message context
     * @param originalKey the original artifact key to restore (can be null)
     */
    public static void restoreCoverageArtifactKey(MessageContext synCtx, String originalKey) {
        if (isUnitTestMode(synCtx)) {
            synCtx.setProperty(Constants.COVERAGE_ARTIFACT_KEY, originalKey);
        }
    }

    /**
     * Check if currently running in unit test mode.
     *
     * @param synCtx message context
     * @return true if running in unit test mode, false otherwise
     */
    private static boolean isUnitTestMode(MessageContext synCtx) {
        return synCtx.getConfiguration() != null &&
               "true".equals(synCtx.getConfiguration().getProperty(Constants.IS_RUNNING_AS_UNIT_TEST));
    }

    /**
     * Register a sequence for coverage tracking if not already registered.
     * This is used for registry sequences that are referenced but not deployed as artifacts.
     *
     * @param sequence the sequence to register
     * @param sequenceKey the key to use for registration
     */
    private static void registerSequenceIfNotRegistered(SequenceMediator sequence, String sequenceKey) {
        MediatorRegistry registry = MediatorRegistry.getInstance();
        String artifactKey = "Sequence:" + sequenceKey;
        
        if (!registry.isArtifactRegistered(artifactKey)) {
            registry.registerSequence(sequence, sequenceKey);
            if (log.isDebugEnabled()) {
                log.debug("Registered referenced sequence for coverage: " + sequenceKey);
            }
        }
    }
}
