package com.synapse.core.deployers;

import com.synapse.core.artifacts.Mediator;
import com.synapse.core.artifacts.utils.Position;
import org.apache.axiom.om.OMElement;

public interface MediatorDeployer {
    Mediator unmarshal(OMElement element, Position position) throws Exception;
}
