package org.apache.synapse.aspects.flow.statistics.log;

import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.CallbackDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;

/**
 * Created by rajith on 7/20/16.
 */
public interface StatisticEventProcessorInterface {

    void openStatisticEntry(StatisticDataUnit statisticDataUnit);

    void closeStatisticEntry(BasicStatisticDataUnit dataUnit, int mode);

    void openParents(BasicStatisticDataUnit basicStatisticDataUnit);

    void addCallbacks(CallbackDataUnit callbackDataUnit);

    void removeCallback(CallbackDataUnit callbackDataUnit);

    void updateForReceivedCallback(CallbackDataUnit callbackDataUnit);

    void reportFault(BasicStatisticDataUnit basicStatisticDataUnit);

    void reportAsynchronousExecution(BasicStatisticDataUnit basicStatisticDataUnit);
}
