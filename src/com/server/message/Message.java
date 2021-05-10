package com.server.message;

import java.io.Serializable;

public class Message implements Serializable {
    private String message = "";
    private MessageType messageType;

    public Message(MessageType messageType) {
        this.messageType = messageType;
    }

    public Message(MessageType messageType, String message) {
        this.messageType = messageType;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public MessageType getMessageType() {
        return messageType;
    }
}
