package org.apache.synapse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import java.lang.reflect.Field;

/**
 * Created by nadeeshaan on 7/2/15.
 */
public class AppenderWrapper extends PatternLayout {

    public AppenderWrapper() {
        super();
    }

    public AppenderWrapper(String pattern) {
        super(pattern);
    }

    @Override
    public String format(LoggingEvent event) {

        // only process String type messages
        if (event.getMessage() != null && event.getMessage() instanceof String) {

            String message = event.getMessage().toString();
            message = StringUtils.trim(SingletonLogSetter.getInstance().getLogAppenederContent() + message);

            // earlier versions of log4j don't provide any way to update messages,
            // so use reflections to do this
            try {
                Field field = LoggingEvent.class.getDeclaredField("message");
                field.setAccessible(true);
                field.set(event, message);
            } catch (Exception e) {
                // Dont log it as it will lead to infinite loop. Simply print the trace
                e.printStackTrace();
            }

        }

        return super.format(event);
    }

}
