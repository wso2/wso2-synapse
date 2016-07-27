package org.apache.synapse.aspects.flow.statistics.log;

import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.CallbackDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

/**
 * This is the interface which contains methods required for message flow processor which will be implemented in
 * mediation layer
 */
public interface MessageFlowProcessorInterface {

    void openStatisticEntry(StatisticDataUnit statisticDataUnit);

    void closeStatisticEntry(BasicStatisticDataUnit dataUnit, int mode);

    void openParents(BasicStatisticDataUnit basicStatisticDataUnit);

    void addCallbacks(CallbackDataUnit callbackDataUnit);

    void removeCallback(CallbackDataUnit callbackDataUnit);

    void updateForReceivedCallback(CallbackDataUnit callbackDataUnit);

    void reportFault(BasicStatisticDataUnit basicStatisticDataUnit);

    void reportAsynchronousExecution(BasicStatisticDataUnit basicStatisticDataUnit);
}
