package com.synapse.core.artifacts.mediators;

import com.synapse.core.artifacts.Mediator;
import com.synapse.core.artifacts.utils.Position;
import com.synapse.core.synctx.MsgContext;

public class LogMediator implements Mediator {
    private String category;
    private String message;
    private Position position;

    public LogMediator(String category, String message, Position position) {
        this.category = category;
        this.message = message;
        this.position = position;
    }

    public LogMediator() {

    }

    @Override
    public boolean execute(MsgContext context) {
        // Log the message
        System.out.println(category + " : " + message);
        return true;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}
