package com.synapse.core.artifacts;

import com.synapse.core.synctx.MsgContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SequenceTest {

    @Test
    void testExecute() {
        Mediator mockMediator = context -> true;
        Sequence sequence = new Sequence(List.of(mockMediator), null, "TestSequence");

        MsgContext context = new MsgContext();
        boolean result = sequence.execute(context);

        assertTrue(result);
    }

    @Test
    void testExecuteWithFailure() {
        Mediator failingMediator = context -> false;
        Sequence sequence = new Sequence(List.of(failingMediator), null, "TestSequence");

        MsgContext context = new MsgContext();
        boolean result = sequence.execute(context);

        assertFalse(result);
    }
}