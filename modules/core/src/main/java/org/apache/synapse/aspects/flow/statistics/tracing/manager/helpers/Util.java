package org.apache.synapse.aspects.flow.statistics.tracing.manager.helpers;

import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;

public class Util {
    public static String extractId(BasicStatisticDataUnit basicStatisticDataUnit) {
        return String.valueOf(basicStatisticDataUnit.getCurrentIndex());
    }

    public static String getObjectReference(Object object) {
        return String.valueOf(System.identityHashCode(object));
    }
}
