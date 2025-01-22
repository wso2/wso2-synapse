package org.apache.synapse.mediators.v2.ext;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;

/**
 * This is the super class for custom class mediators. The user can extend this class and implement a custom method with
 * the allowed argument types. The method parameters should be annotated with the @Arg annotation to specify the
 * argument name and the type. The method may return a value if needed. The return type should be one of the allowed.
 */
public class AbstractClassMediator extends AbstractMediator {

    @Override
    public boolean mediate(MessageContext synCtx) {

        // User implemented method will be invoked from the Class mediator
        return true;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({PARAMETER})
    public @interface Arg {

        String name();

        ArgumentType type();
    }

    public enum ArgumentType {
        STRING("String"),
        BOOLEAN("Boolean"),
        INTEGER("Integer"),
        LONG("Long"),
        DOUBLE("Double"),
        JSON("Json"),
        XML("Xml");

        private final String typeName;

        ArgumentType(String typeName) {

            this.typeName = typeName;
        }

        public String getTypeName() {

            return typeName;
        }

        @Override
        public String toString() {

            return typeName;
        }
    }
}
