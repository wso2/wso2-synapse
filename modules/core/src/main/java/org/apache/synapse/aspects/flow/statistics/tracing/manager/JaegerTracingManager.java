package org.apache.synapse.aspects.flow.statistics.tracing.manager;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Span;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.BasicStatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.DefaultHandlerNew;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.handlers.JaegerTracingHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.manager.subhandlers.SubHandler;
import org.apache.synapse.aspects.flow.statistics.tracing.store.SpanStore;

import java.util.HashMap;
import java.util.Map;

public class JaegerTracingManager implements OpenTracingManager {
    private JaegerTracer tracer;
    private SpanStore spanStore = new SpanStore();
    private JaegerTracingHandler handler;

    // TODO experimental
    private Map<Object, SubHandler> subHandlers;

    public JaegerTracingManager() {
        initializeTracer();
        resolveHandler();
        subHandlers = new HashMap<>();
    }

    @Override
    public void initializeTracer() {
        String serviceName = "test-synapse-service"; // TODO get from a global place

        Configuration.SamplerConfiguration sampler = new Configuration.SamplerConfiguration()
                .withType(ConstSampler.TYPE)
                .withParam(1)
                .withManagerHostPort("localhost:5778");
        Configuration.SenderConfiguration sender = new Configuration.SenderConfiguration()
                .withAgentHost("localhost")
                .withAgentPort(6831);
        Configuration.ReporterConfiguration reporter = new Configuration.ReporterConfiguration()
                .withLogSpans(false)
                .withSender(sender)
                .withMaxQueueSize(123)
                .withFlushInterval(12345);

        this.tracer = new Configuration(serviceName)
                .withSampler(sampler)
                .withReporter(reporter)
                .getTracer();
    }

    @Override
    public void resolveHandler() {
        this.handler = new DefaultHandlerNew(tracer, spanStore); // TODO Make this to resolve programmatically
    }

    @Override
    public void addSubHandler(Object referrer, SubHandler subHandler) {
        this.subHandlers.put(referrer, subHandler);
    }

    @Override
    public void removeSubHandler(Object referrer) {
        this.subHandlers.remove(referrer);
    }

    @Override
    public void handleOpenEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx, Span parentSpan) {
        printEvent(statisticDataUnit);
//        handler.handleOpenEvent(statisticDataUnit, synCtx, parentSpan);
    }

    @Override
    public void handleOpenEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        handler.handleOpenEntryEvent(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenChildEntryEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        handler.handleOpenChildEntryEvent(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowContinuableEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        handler.handleOpenFlowContinuableEvent(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowSplittingEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        handler.handleOpenFlowSplittingEvent(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowAggregateEvent(String absoluteId, StatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        handler.handleOpenFlowAggregateEvent(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenFlowAsynchronousEvent(String absoluteId, BasicStatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        handler.handleOpenFlowAsynchronousEvent(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleOpenContinuationEvents(String absoluteId, BasicStatisticDataUnit statisticDataUnit, MessageContext synCtx) {
        handler.handleOpenContinuationEvents(absoluteId, statisticDataUnit, synCtx);
    }

    @Override
    public void handleCloseEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        printEvent(basicStatisticDataUnit);
//        handler.handleCloseEvent(basicStatisticDataUnit, synCtx);
    }

    @Override
    public void handleCloseEntryEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        handler.handleCloseEntryEvent(basicStatisticDataUnit, synCtx);
    }

    @Override
    public void handleCloseFlowForcefully(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        handler.handleCloseFlowForcefully(basicStatisticDataUnit, synCtx);
    }

    @Override
    public void handleTryEndFlow(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
        handler.handleTryEndFlow(basicStatisticDataUnit, synCtx);
    }

    @Override
    public void handleAddCallback(MessageContext messageContext, String callbackId) {
        handler.handleAddCallback(messageContext, callbackId);
    }

    @Override
    public void handleCallbackCompletionEvent(MessageContext oldMssageContext, String callbackId) {
        handler.handleCallbackCompletionEvent(oldMssageContext, callbackId);
    }

    @Override
    public void handleUpdateParentsForCallback(MessageContext oldMessageContext, String callbackId) {
        handler.handleUpdateParentsForCallback(oldMessageContext, callbackId);
    }

    @Override
    public void handleReportCallbackHandlingCompletion(MessageContext synapseOutMsgCtx, String callbackId) {
        handler.handleReportCallbackHandlingCompletion(synapseOutMsgCtx, callbackId);
    }


    @Override
    public void handleStackInsertion(MessageContext synCtx) {
        handler.handleStateStackInsertion(synCtx);
    }

    @Override
    public synchronized void handleStackRemoval(MessageContext synCtx) {
        handler.handleStateStackRemoval(synCtx);
    }

    @Override
    public synchronized void handleStackClearance(MessageContext synCtx) {
        handler.handleStateStackClearance(synCtx);
    }

    @Override
    public void closeTracer() {
        this.tracer.close();
    }

    private void printEvent(BasicStatisticDataUnit statisticDataUnit) {
//        if (statisticDataUnit instanceof StatisticDataUnit) {
//            // Open Event
//            System.out.println("Open Event - CurrentIndex: " + statisticDataUnit.getCurrentIndex() +
//                    ", ComponentName: " + ((StatisticDataUnit) statisticDataUnit).getComponentName() +
//                    ", ComponentType: " + ((StatisticDataUnit) statisticDataUnit).getComponentType() +
//                    ", StatisticsId: " + statisticDataUnit.getStatisticId() +
//                    ", ShouldTrackParent: " + ((StatisticDataUnit) statisticDataUnit).isShouldTrackParent());
//        } else {
//            // Close Event
//            System.out.println("Close Event - CurrentIndex: " + statisticDataUnit.getCurrentIndex() +
//                    ", StatisticsId: " + statisticDataUnit.getStatisticId());
//        }
    }
}


















    // TODO methods before change [BEGIN]
    //    @Override
//    public void handleOpenEvent(StatisticDataUnit statisticDataUnit, MessageContext synCtx, Span parentSpan) {
//        if (!isCallMediatorSequence(statisticDataUnit)) {
//            beginSpan(statisticDataUnit, synCtx, parentSpan);
//        } else {
//            // Will begin during addSeqContinuationState
//            markCallMediatorSequenceIn(statisticDataUnit);
//        }
//        printStateInfoForDebug(synCtx);
//    }
//
//    @Override
//    public void handleCloseEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
//        if (!isCallMediatorSequence(basicStatisticDataUnit)) {
//            endSpan(basicStatisticDataUnit, synCtx);
//        }
//        // Else: Will end during pop from stack
//        printStateInfoForDebug(synCtx);
//    }
//
//    public void handleStackInsertion(MessageContext synCtx) {
//        StackedSequenceInfo stackedSequenceInfo = getNextUnstartedCallMediatorSequenceReference();
//        if (stackedSequenceInfo != null) {
//            String spanId = stackedSequenceInfo.getSpanReferenceId();
//            Span span;
//
//            Span chosenParentSpan = getLatestActiveSpanAsParent();
//            span = tracer.buildSpan(stackedSequenceInfo.getComponentName()).asChildOf(chosenParentSpan).start();
//
//            // TODO set span tags
//
//            spanStore.addActiveSpan(spanId, span, synCtx);
//            stackedSequenceInfo.setStarted(true);
//
//            System.out.println("");
//            System.out.println("Started Call Mediator Sequence Span - ComponentName: " +
//                    stackedSequenceInfo.getComponentName());
//            System.out.println("");
//        } else {
//            // TODO look carefully
//            System.out.println("EXCEPTIONAL: Next Unstarted Call Mediator Sequence Reference is null");
//        }
//        printStateInfoForDebug(synCtx);
//    }
//
//    public synchronized void handleStackRemoval(MessageContext synCtx) {
//        StackedSequenceInfo stackedSequenceInfo = spanStore.popActiveCallMediatorSequences();
//        if (stackedSequenceInfo != null) {
//            finishSpanForStackedSequenceInfo(synCtx, stackedSequenceInfo);
//        } else {
//            // TODO look carefully
//            System.out.println("EXCEPTIONAL: Popped StackedSequenceInfo is null");
//        }
//        printStateInfoForDebug(synCtx);
//    }
//
//    public synchronized void handleStackClearance(MessageContext synCtx) { // Clears the whole stack
//        StackedSequenceInfo stackedSequenceInfo = spanStore.popActiveCallMediatorSequences();
//        while (stackedSequenceInfo != null && !spanStore.getActiveStackedSequences().isEmpty()) {
//            finishSpanForStackedSequenceInfo(synCtx, stackedSequenceInfo);
//
//            printStateInfoForDebug(synCtx);
//
//            stackedSequenceInfo = spanStore.popActiveCallMediatorSequences();
//        }
//    }

//    private boolean isCallMediatorSequence(BasicStatisticDataUnit basicStatisticDataUnit) {
//        boolean isCallMediator = true; // TODO calculate this
//        if (basicStatisticDataUnit instanceof StatisticDataUnit) {
//            // Came in when starting
//            return isCallMediator &&
//                    ((StatisticDataUnit) basicStatisticDataUnit).getComponentType() == ComponentType.SEQUENCE;
//        }
//        // Came in when finishing
//        return isCallMediator &&
//                spanStore.containsActiveCallMediatorSequenceWithId(
//                        String.valueOf(basicStatisticDataUnit.getCurrentIndex()));
//    }
//
//    private void markCallMediatorSequenceIn(StatisticDataUnit statisticDataUnit) {
//        StackedSequenceInfo stackedSequenceInfo =
//                new StackedSequenceInfo(getSpanWrapperIdOf(statisticDataUnit), statisticDataUnit.getComponentName());
//        spanStore.pushToActiveCallMediatorSequences(stackedSequenceInfo);
//    }
//
//    private synchronized void finishSpanForStackedSequenceInfo(MessageContext synCtx, StackedSequenceInfo stackedSequenceInfo) {
//        String spanWrapperId = stackedSequenceInfo.getSpanReferenceId();
//        spanStore.finishActiveSpan(spanWrapperId);
//        System.out.println("");
//        System.out.println("Finished Call Mediator Sequence Span - ComponentName: " +
//                stackedSequenceInfo.getComponentName());
//        System.out.println("");
//    }
//
//    private void beginSpan(StatisticDataUnit statisticDataUnit, MessageContext synCtx, Span rootSpan) {
//        String spanId = getSpanWrapperIdOf(statisticDataUnit);
//        Span parentSpan = getLatestActiveSpanAsParent();
//        Span span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(parentSpan).start();
//        setSpanTags(statisticDataUnit, span);
//
//        spanStore.addActiveSpan(spanId, span, synCtx);
//
//        System.out.println("");
//        System.out.println("Started Span - currentIndex: " + statisticDataUnit.getCurrentIndex() +
//                ", componentId: " + statisticDataUnit.getComponentId() +
//                ", statisticsId: " + statisticDataUnit.getStatisticId());
//        System.out.println("");

        // TODO Remove old logic when confirmed. (Get parent from EI-analytics numbering)

//        String spanId = getSpanWrapperIdOf(statisticDataUnit);
//        Span span;
//        Span newParentSpan = resolveParentSpan(statisticDataUnit);
//
//        if (newParentSpan != null) {
//            span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(newParentSpan).start();
//        } else {
//            Span chosenParentSpan = getLatestActiveSpanAsParent();
//            span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(chosenParentSpan).start();
//        }
//        setSpanTags(statisticDataUnit, span);
//
//        spanStore.addActiveSpan(spanId, span, synCtx);
//
//        System.out.println("");
//        System.out.println("Started Span - currentIndex: " + statisticDataUnit.getCurrentIndex() +
//                ", componentId: " + statisticDataUnit.getComponentId() +
//                ", statisticsId: " + statisticDataUnit.getStatisticId());
//        System.out.println("");
//    }

//    public void setSpanTags(StatisticDataUnit statisticDataUnit, Span span) {
//        span.setBaggageItem("componentType", statisticDataUnit.getComponentType().toString());
//        span.setBaggageItem("componentId", statisticDataUnit.getComponentId());
//        span.setTag("threadId", Thread.currentThread().getId());
//        span.setTag("sampleTag", "statisticDataUnit.something"); // TODO all other info
//    }
//
//    private void endSpan(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext synCtx) {
//        String spanWrapperId = getSpanWrapperIdOf(basicStatisticDataUnit);
//        spanStore.finishActiveSpan(spanWrapperId);
//
//        System.out.println("");
//        System.out.println("Finished Span - currentIndex: " + basicStatisticDataUnit.getCurrentIndex() +
//                ", statisticsId: " + basicStatisticDataUnit.getStatisticId());
//        System.out.println("");
//
//        System.out.println("\tEnd span event. Not a stack event");
//    }
//
//    private synchronized StackedSequenceInfo getNextUnstartedCallMediatorSequenceReference() {
//        for (StackedSequenceInfo stackedSequenceInfo : spanStore.getActiveStackedSequences()) {
//            if (!stackedSequenceInfo.isStarted()) {
//                return stackedSequenceInfo;
//            }
//        }
//        return null;
//    }
//
//    private static String getSpanWrapperIdOf(BasicStatisticDataUnit basicStatisticDataUnit) {
//        return String.valueOf(basicStatisticDataUnit.getCurrentIndex()); // TODO return as int if works
//    }
//
//    private Span resolveParentSpan(StatisticDataUnit statisticDataUnit) {
//        String parentId = String.valueOf(statisticDataUnit.getParentIndex());
//        if (isSpanActive(parentId)) {
//            System.out.println("");
//            System.out.println(statisticDataUnit.getCurrentIndex() + "'s parent is " + parentId);
//            System.out.println("");
//            return spanStore.getActiveSpans().get(parentId).getSpan();
//        }
//        return null;
//    }
//
//    private boolean isSpanActive(String spanId) {
//        return spanStore.getActiveSpans().containsKey(spanId);
//    }
//
//    private Span getLatestActiveSpanAsParent() {
//        List<Integer> ids = new ArrayList<>();
//        for (String id : spanStore.getActiveSpans().keySet()) {
//            ids.add(Integer.valueOf(id));
//        }
//        if (!ids.isEmpty()) {
//            Collections.sort(ids);
//            int spanId = ids.get(ids.size() - 1);
//            return spanStore.getActiveSpans().get(String.valueOf(spanId)).getSpan();
//        }
//        return null;
//    }
//
//    // TODO remove
//    private static synchronized void printStack(MessageContext synCtx) {
//        Stack<ContinuationState> continuationStatesStack = synCtx.getContinuationStateStack();
//        if (continuationStatesStack != null) {
//            System.out.println("\tStack:");
//            System.out.print("\t\t[");
//            for (ContinuationState continuationState : continuationStatesStack) {
//                if (continuationState instanceof SeqContinuationState) {
//                    SeqContinuationState seqContinuationState = (SeqContinuationState) continuationState;
//                    System.out.print(seqContinuationState.getPosition() + ". Name:" + seqContinuationState.getSeqName() + "(Type: " + seqContinuationState.getSeqType() + ")");
//                } else if (continuationState instanceof ReliantContinuationState) {
//                    ReliantContinuationState reliantContinuationState = (ReliantContinuationState) continuationState;
//                    System.out.print(reliantContinuationState.getPosition() + ". SubBranch: " + reliantContinuationState.getSubBranch());
//                }
//            }
//            System.out.println("]");
//        }
//    }
//
//    // TODO remove
//    private synchronized void printStateInfoForDebug(MessageContext synCtx) {
//        printStack(synCtx);
//        spanStore.printActiveSpans();
//        spanStore.printactiveCallMediatorSequences();
//    }
    // TODO methods before change [END]




// TODO old below. Remove when finalized

//public class JaegerTracingManager implements OpenTracingManager {
//    private JaegerTracer tracer;
//    private SpanStore spanStore = new SpanStore();
//    private JaegerSpanHandler spanHandler;
//
//    public JaegerTracingManager() {
//        initializeTracer();
//        resolveSpanHandler(); // TODO this should happen frequently. This is not the right place
//    }
//
//    @Override
//    public void initializeTracer() {
//        String serviceName = "test-synapse-service"; // TODO get from a global place
//
//        Configuration.SamplerConfiguration sampler = new Configuration.SamplerConfiguration()
//                .withType(ConstSampler.TYPE)
//                .withParam(1)
//                .withManagerHostPort("localhost:5778");
//        Configuration.SenderConfiguration sender = new Configuration.SenderConfiguration()
//                .withAgentHost("localhost")
//                .withAgentPort(6831);
//        Configuration.ReporterConfiguration reporter = new Configuration.ReporterConfiguration()
//                .withLogSpans(false)
//                .withSender(sender)
//                .withMaxQueueSize(123)
//                .withFlushInterval(12345);
//
//        this.tracer = new Configuration(serviceName)
//                .withSampler(sampler)
//                .withReporter(reporter)
//                .getTracer();
//    }
//
//    @Override
//    public void closeTracer() {
//        this.tracer.close();
//    }
//
//    @Override
//    public void handleOpenEvent(StatisticDataUnit statisticDataUnit, Span parentSpan) { // TODO remove 'Span parentSpan'
//        beginSpan(statisticDataUnit, parentSpan);
////        if (isAPIComponent()) {
////            System.out.println("");
////            System.out.println("About to start: " + statisticDataUnit.getComponentId());
////            System.out.println("");
////            if (isAPI(statisticDataUnit.getComponentType()) ||
////                    isResource(statisticDataUnit.getComponentType())) {
////                System.out.println("Cautious component type");
////                if (spanStore.getActiveSpans().size() < 2) {
////                    System.out.println("");
////                    System.out.println("Allowed to start. Starting");
////                    System.out.println("");
////                    beginSpan(statisticDataUnit, parentSpan);
////                } else {
////                    // else don't begin (existing two spans are remaining)
////                    System.out.println("");
////                    System.out.println("Not allowed to start. Remapping Span");
////                    System.out.println("");
////                    remapActiveSpanFor(statisticDataUnit);
////                }
////            } else {
////                beginSpan(statisticDataUnit, parentSpan);
////            }
////        }
//    }
//
//    @Override
//    public void handleCloseEvent(BasicStatisticDataUnit basicStatisticDataUnit, MessageContext messageContext) {
//        endSpan(basicStatisticDataUnit);
////        if (isAPIComponent()) {
////            if (spanStore.getActiveSpans().size() > 2) {
////                endSpan(basicStatisticDataUnit);
////            } else {
////                // Last 2 components: API and Resource
////                System.out.println("");
////                System.out.println("About to finish a cautious component");
////                System.out.println("");
////                if (isEverythingOver()) {
////                    System.out.println("");
////                    System.out.println("Allowed to finish cautious component. Finishing");
////                    System.out.println("");
////                    endSpan(basicStatisticDataUnit);
////                } else {
////                    System.out.println("");
////                    System.out.println("Not allowed to finish. Skipping");
////                    System.out.println("");
////                }
////            }
////        }
//    }
//
//    private void beginSpan(StatisticDataUnit statisticDataUnit, Span rootSpan) {
//        String spanId = getIdOf(statisticDataUnit);
//        Span span;
//        Span newParentSpan = resolveParentSpan(statisticDataUnit);
//
//        // if (parentSpan != null) {
//        if (newParentSpan != null) {
//            span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(newParentSpan).start();
//        } else {
//            Span chosenParentSpan = getLatestActiveSpanAsParent();
//            span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(chosenParentSpan).start();
////            span = tracer.buildSpan(statisticDataUnit.getComponentName()).asChildOf(spanStore.getRootSpan()).start();
//        }
//        setSpanTags(statisticDataUnit, span);
//        spanStore.addActiveSpan(spanId, span);
//        System.out.println("");
//        System.out.println("Started Span - currentIndex: " + statisticDataUnit.getCurrentIndex() +
//                ", componentId: " + statisticDataUnit.getComponentId() +
//                ", statisticsId: " + statisticDataUnit.getStatisticId());
//        System.out.println("");
//    }
//
//    private boolean isAPIComponent() {
//        return true; // TODO put a nice handler
//    }
//
//    public void setSpanTags(StatisticDataUnit statisticDataUnit, Span span) {
//        span.setBaggageItem("startTime", String.valueOf(statisticDataUnit.getTime()));
//        span.setBaggageItem("componentType", statisticDataUnit.getComponentType().toString());
//        span.setBaggageItem("componentId", statisticDataUnit.getComponentId());
//        span.setTag("threadId", Thread.currentThread().getId());
//        span.setTag("sampleTag", "statisticDataUnit.something"); // TODO all
//    }
//
//    private void endSpan(BasicStatisticDataUnit basicStatisticDataUnit) {
//        String spanId = getIdOf(basicStatisticDataUnit);
////        spanStore.finishActiveSpan(spanId);
//        spanStore.finishActiveSpanWithEndTime(spanId, basicStatisticDataUnit.getTime());
//        System.out.println("");
//        System.out.println("Finished Span - currentIndex: " + basicStatisticDataUnit.getCurrentIndex() +
//                ", statisticsId: " + basicStatisticDataUnit.getStatisticId());
//        System.out.println("");
//    }
//
//    public void startRootSpan() {
//        Span rootSpan = tracer.buildSpan("rootSpan").start();
//        // TODO get the proper name from somewhere, or assign the initial (outer level) span as root
//        rootSpan.setTag("rootSpan", "rootSpan");
//        spanStore.setRootSpan(rootSpan);
//    }
//
//    public void endRootSpan() {
//        spanStore.getRootSpan().finish();
//        spanStore.setRootSpan(null);
//    }
//
//    private void resolveSpanHandler() {
//        if (isAPIComponent()) {
//            spanHandler = new RESTAPIHandler(tracer, spanStore);
//        }
//    }
//
//    private static String getIdOf(BasicStatisticDataUnit basicStatisticDataUnit) {
//        return String.valueOf(basicStatisticDataUnit.getCurrentIndex()); // TODO return as int if works
//    }
//
//    private Span resolveParentSpan(StatisticDataUnit statisticDataUnit) {
//        if (statisticDataUnit.getCurrentIndex() == 6) {
//            Object o = null;
//        }
//        String parentId = String.valueOf(statisticDataUnit.getParentIndex());
//        if (isSpanActive(parentId)) {
//            System.out.println("");
//            System.out.println(statisticDataUnit.getCurrentIndex() + "'s parent is " + parentId);
//            System.out.println("");
//            return spanStore.getActiveSpans().get(parentId);
//        }
//        return null;
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
//    private boolean isSpanActive(String spanId) {
//        return spanStore.getActiveSpans().containsKey(spanId);
//    }
//
//    private Span getLatestActiveSpanAsParent() {
//        List<Integer> ids = new ArrayList<>();
//        for (String id : spanStore.getActiveSpans().keySet()) {
//            ids.add(Integer.valueOf(id));
//        }
//        if (!ids.isEmpty()) {
//            Collections.sort(ids);
//            int spanId = ids.get(ids.size() - 1);
//            return spanStore.getActiveSpans().get(String.valueOf(spanId));
//        }
//        return null;
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
//    ////////////////
//
//
//}
