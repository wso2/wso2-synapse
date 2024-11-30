package org.apache.synapse.mediators.transform;

import org.apache.synapse.MessageContext;
import org.apache.synapse.TestMessageContextBuilder;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.builtin.JSONTransformMediator;
import org.junit.Assert;
import org.junit.Test;

public class JSONTransformMediatorTest {

    @Test
    public void testWithResourceKey() throws Exception {

        JSONTransformMediator jsonTransformMediator = new JSONTransformMediator();
        jsonTransformMediator.setSchemaKey(new Value("resources:xslt/sample.json"));

        String payload = "<jsonObject>\n" +
                "    <fruit>12345</fruit>\n" +
                "    <quantity>10</quantity>\n" +
                "</jsonObject>";

        MessageContext synCtx = new TestMessageContextBuilder().addFileEntry("gov:mi-resources/xslt/sample.json",
                        "../../repository/conf/sample/resources/transform/schema.json")
                .setBodyFromString(payload).setRequireAxis2MessageContext(true).build();

        try {
            jsonTransformMediator.mediate(synCtx);
        } catch (Exception ex) {
            Assert.assertTrue(ex.getMessage().contains("\"required\":[\"price\"]"));
        }
    }
}
