package com.server.message;

import java.io.Serializable;

public class Message implements Serializable {
    private String message = "";
    private String messageType = "";

    public Message(String messageType) {
        this.messageType = messageType;
    }

    public Message(String messageType, String message) {
        this.messageType = messageType;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageType() {
        return messageType;
    }
}
