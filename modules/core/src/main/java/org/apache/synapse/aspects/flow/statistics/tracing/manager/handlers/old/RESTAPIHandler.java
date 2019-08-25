package org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.old;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Span;
import org.apache.synapse.Mediator;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.SendMediator;
import org.apache.synapse.rest.API;
import org.apache.synapse.rest.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RESTAPIHandler /*extends JaegerSpanHandler*/ {
//    private int expectedStartCount = 0;
//    private int expectedEndCount = 0;
//    private int startCounter = 0;
//    private int endCounter = 0;
//
//    public RESTAPIHandler(JaegerTracer tracer, SpanStore spanStore) {
//        super(tracer, spanStore);
//    }
//
//    public void handleOpenEvent(StatisticDataUnit statisticDataUnit, Span parentSpan) {
//        if (isStartAllowed(statisticDataUnit)) {
//            beginSpan(statisticDataUnit, parentSpan);
//        }
//        // Else skip
//    }
//
//    private boolean isStartAllowed(StatisticDataUnit statisticDataUnit) {
//        if (isCautiousComponentType(statisticDataUnit)) {
//            return startCounter == 0;
//        }
//        return true;
//    }
//
//    public void handleCloseEvent(BasicStatisticDataUnit basicStatisticDataUnit) {
//        if (spanStore.getActiveSpans().size() > 2) {
//            endSpan(basicStatisticDataUnit);
//        } else {
//            // Last 2 components: API and Resource
//            System.out.println("");
//            System.out.println("About to finish a cautious component");
//            System.out.println("");
//            if (isEverythingOver()) {
//                System.out.println("");
//                System.out.println("Allowed to finish cautious component. Finishing");
//                System.out.println("");
//                endSpan(basicStatisticDataUnit);
//            } else {
//                System.out.println("");
//                System.out.println("Not allowed to finish. Skipping");
//                System.out.println("");
//            }
//        }
//    }
//
//    private boolean isEndAllowed(BasicStatisticDataUnit basicStatisticDataUnit) {
//        if (basicStatisticDataUnit instanceof StatisticDataUnit) {
//            // Not a forceful close due to fault sequence
//            if (isCautiousComponentType((StatisticDataUnit) basicStatisticDataUnit)) {
//                return endCounter== 3 || endCounter == 4;
//            }
//            return true;
//        }
//        return true; // TODO check
//    }
//
//    private boolean setExpectedCounts(StatisticDataUnit statisticDataUnit) {
//        String apiName = getAPIName();
//        API api = statisticDataUnit.getSynapseEnvironment().getServerContextInformation().getSynapseConfiguration()
//                .getAPI(apiName);
//        Resource resource = getResource(api);
//
//        if (hasInSequence(resource) && hasOutSequence(resource) && hasSendMediator(resource.getInSequence())) {
//            expectedStartCount = 2;
//            expectedEndCount = 2;
//        }
//
//        return false; // todo do
//    }
//
//    private String getAPIName() {
//        return "";
//    }
//
//    private Resource getResource(API api) {
//        return api.getResource("59f18df2806812ea8bf1461f0ff597ac73509349f4fb729c");
//    }
//
//    private boolean hasInSequence(Resource resource) {
//        return resource.getInSequence() != null;
//    }
//
//    private boolean hasOutSequence(Resource resource) {
//        return resource.getOutSequence() != null;
//    }
//
//    private boolean hasFaultSequence(Resource resource) {
//        return resource.getFaultSequence() != null;
//    }
//
//    private boolean hasSendMediator(SequenceMediator inSequence) {
//        List<Mediator> mediators = inSequence.getList();
//        for (Mediator mediator : mediators) {
//            if (mediator instanceof SendMediator) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private boolean isCautiousComponentType(StatisticDataUnit statisticDataUnit) {
//        return isAPI(statisticDataUnit.getComponentType()) || isResource(statisticDataUnit.getComponentType());
//    }
//
//    private boolean isAPI(ComponentType componentType) {
//        return componentType == ComponentType.API;
//    }
//
//    private boolean isResource(ComponentType componentType) {
//        return componentType == ComponentType.RESOURCE;
//    }
//
//    private void remapActiveSpanFor(StatisticDataUnit statisticDataUnit) {
//        ComponentType componentType = statisticDataUnit.getComponentType();
//        if (componentType == ComponentType.API) {
//            String id = String.valueOf(statisticDataUnit.getCurrentIndex());
//            Span activeSpan = getActiveSpanOfAPI();
//            spanStore.addActiveSpan(id, activeSpan);
//        } else if (componentType == ComponentType.RESOURCE) {
//            String id = String.valueOf(statisticDataUnit.getCurrentIndex());
//            Span activeSpan = getActiveSpanOfResource();
//            spanStore.addActiveSpan(id, activeSpan);
//        }
//    }
//
//    private Span getActiveSpanOfAPI() {
//        return getSortedActiveSpans().get(0);
//    }
//
//    private Span getActiveSpanOfResource() {
//        return getSortedActiveSpans().get(1);
//    }
//
//    private List<Span> getSortedActiveSpans() {
//        List<Integer> keys = new ArrayList<>();
//        for (String key : spanStore.getActiveSpans().keySet()) {
//            keys.add(Integer.valueOf(key));
//        }
//        Collections.sort(keys);
//        List<Span> spans = new ArrayList<>();
//        for (Integer key : keys) {
//            spans.add(spanStore.getActiveSpans().get(String.valueOf(key)));
//        }
//        return spans;
//    }
//
//    private boolean isEverythingOver() {
//        return org.apache.axis2.context.MessageContext.getCurrentMessageContext() == null;
//    }
}
