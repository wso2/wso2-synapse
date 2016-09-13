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

package org.apache.synapse.debug.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.SwitchCase;
import org.apache.synapse.debug.constructs.EnclosedInlinedSequence;
import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.mediators.builtin.CommentMediator;
import org.apache.synapse.mediators.builtin.ForEachMediator;
import org.apache.synapse.mediators.eip.aggregator.AggregateMediator;
import org.apache.synapse.mediators.eip.splitter.CloneMediator;
import org.apache.synapse.mediators.eip.splitter.IterateMediator;
import org.apache.synapse.mediators.filters.FilterMediator;
import org.apache.synapse.mediators.filters.SwitchMediator;
import org.apache.synapse.mediators.template.InvokeMediator;

/**
 * Helper class to locate a mediator while traversing the mediator tree given relative child position array
 * starting from parent root of tree to the particular mediator.
 */
public class MediatorTreeTraverseUtil {

    private static final Log log = LogFactory.getLog(MediatorTreeTraverseUtil.class);
    /**
     * Returns mediator referece associated with position while traversing the mediator tree.
     *
     * @param synCfg      synapse configuration reference
     * @param seqMediator sequence mediator which traverse happens
     * @param position    array of tree nodes specifying position integer with respect to it's parent node
     *                    starting from the root parent.
     * @return Mediator reference
     */
    public static Mediator getMediatorReference(SynapseConfiguration synCfg,
                                                Mediator seqMediator,
                                                int[] position) {
        Mediator current_mediator = null;


        for (int counter = 0; counter < position.length; counter++) {
            if (counter == 0) {
                int mediatorCount = ((AbstractListMediator) seqMediator).getList().size();
                int correctedPosition = getCorrectedPossition((AbstractListMediator) seqMediator, position[counter]);
                if (mediatorCount > correctedPosition) {
                    current_mediator = ((AbstractListMediator) seqMediator).getChild(correctedPosition);
                } else {
                    log.warn("Mediator position requested is larger than last index : " + position[counter]);
                }
            }
            if (current_mediator != null && counter != 0) {
                if (current_mediator instanceof InvokeMediator) {
                    current_mediator = synCfg.
                            getSequenceTemplate(((InvokeMediator) current_mediator).getTargetTemplate());
                } else if (current_mediator instanceof FilterMediator) {
                    if (position[counter] == 0) {
                        if (((FilterMediator) current_mediator).getElseMediator() != null) {
                            current_mediator = ((FilterMediator) current_mediator).getElseMediator();
                        } else if (((FilterMediator) current_mediator).getElseKey() != null) {
                            current_mediator = synCfg
                                    .getSequence(((FilterMediator) current_mediator).getElseKey());
                        }
                        continue;
                    } else if (position[counter] == 1) {
                        if (((FilterMediator) current_mediator).getThenKey() != null) {
                            current_mediator = synCfg
                                    .getSequence(((FilterMediator) current_mediator).getThenKey());
                        } else {
                            counter = counter + 1;
                            if (counter < position.length) {
                                int mediatorCount = ((AbstractListMediator) current_mediator).getList().size();
                                int correctedPosition = getCorrectedPossition((AbstractListMediator) current_mediator, position[counter]);
                                if (mediatorCount > correctedPosition) {
                                    current_mediator = ((AbstractListMediator) current_mediator)
                                            .getChild(correctedPosition);
                                } else {
                                    log.warn("Mediator position requested is larger than last index : "
                                            + position[counter]);
                                }
                            }
                        }
                        continue;
                    }
                } else if (current_mediator instanceof SwitchMediator) {
                    if (position[counter] == 0) {
                        SwitchCase switchCase = ((SwitchMediator) current_mediator).getDefaultCase();
                        if (switchCase != null) {
                            current_mediator = switchCase.getCaseMediator();
                        } else {
                            current_mediator = null;
                        }
                    } else {
                        SwitchCase switchCase = ((SwitchMediator) current_mediator).getCases()
                                .get(position[counter] - 1);
                        if (switchCase != null) {
                            current_mediator = switchCase.getCaseMediator();
                        } else {
                            current_mediator = null;
                        }
                    }
                    continue;
                } else if (current_mediator instanceof AggregateMediator) {
                    if (((AggregateMediator) current_mediator).getOnCompleteSequence() != null) {
                        current_mediator = ((AggregateMediator) current_mediator).getOnCompleteSequence();
                    } else if (((AggregateMediator) current_mediator).getOnCompleteSequenceRef() != null) {
                        current_mediator = synCfg
                                .getSequence(((AggregateMediator) current_mediator).getOnCompleteSequenceRef());
                    }
                } else if (current_mediator instanceof ForEachMediator) {
                    if (((ForEachMediator) current_mediator).getSequence() != null) {
                        current_mediator = ((ForEachMediator) current_mediator).getSequence();
                    } else if (((ForEachMediator) current_mediator).getSequenceRef() != null) {
                        current_mediator = synCfg
                                .getSequence(((ForEachMediator) current_mediator).getSequenceRef());
                    }
                } else if (current_mediator instanceof IterateMediator) {
                    if (((IterateMediator) current_mediator).getTarget().getSequence() != null) {
                        current_mediator = ((IterateMediator) current_mediator).getTarget().getSequence();
                    } else if (((IterateMediator) current_mediator).getTarget().getSequenceRef() != null) {
                        current_mediator = synCfg.getSequence(((IterateMediator) current_mediator)
                                .getTarget().getSequenceRef());
                    }
                } else if (current_mediator instanceof CloneMediator) {
                    if (((CloneMediator) current_mediator).getTargets().get(position[counter]).getSequence() != null) {
                        current_mediator = ((CloneMediator) current_mediator).getTargets().get(position[counter]).getSequence();
                    } else if (((CloneMediator) current_mediator).getTargets().get(position[counter]).getSequenceRef() != null) {
                        current_mediator = synCfg.getSequence(((CloneMediator) current_mediator)
                                .getTargets().get(position[counter]).getSequenceRef());
                    }
                    continue;
                } else if (current_mediator.getType().equals("ThrottleMediator")) {
                    current_mediator = ((EnclosedInlinedSequence) current_mediator)
                            .getInlineSequence(synCfg, position[counter]);
                    continue;
                } else if (current_mediator.getType().equals("EntitlementMediator")) {
                    current_mediator = ((EnclosedInlinedSequence) current_mediator)
                            .getInlineSequence(synCfg, position[counter]);
                    continue;
                } else if (current_mediator.getType().equals("CacheMediator")) {
                    current_mediator = ((EnclosedInlinedSequence) current_mediator)
                            .getInlineSequence(synCfg, 0);
                }
                if (current_mediator != null && (current_mediator instanceof AbstractListMediator)) {
                    int mediatorCount = ((AbstractListMediator) current_mediator).getList().size();
                    int correctedPosition = getCorrectedPossition((AbstractListMediator) current_mediator, position[counter]);
                    if (mediatorCount > correctedPosition) {
                        current_mediator = ((AbstractListMediator) current_mediator).getChild(correctedPosition);
                    } else {
                        log.warn("Mediator position requested is larger than last index : " + position[counter]);
                    }
                } else {
                    current_mediator = null;
                    break;
                }
            }
        }
        return current_mediator;
    }

    /**
     * Developer Studio will send mediator positions without considering "Comment Mediators".
     * Due to that reason, if there are comments in the source view, mediator positions become incorrect.
     * This method will return the corrected mediator position considering "Comment Mediators" as well.
     *
     * @param seqMediator
     * @param position
     * @return correctedPossition considering comment mediators
     */
    private static int getCorrectedPossition(AbstractListMediator seqMediator, int position) {
        int positionWithComments = 0;
        int positionWithoutComments = 0;
        for (Mediator mediator : seqMediator.getList()) {
            if (!(mediator instanceof CommentMediator)) {
                if (positionWithoutComments == position) {
                    return positionWithComments;
                }
                ++positionWithoutComments;
            }
            ++positionWithComments;
        }
        return position;
    }

}
