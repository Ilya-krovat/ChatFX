package com.client;

import com.server.message.*;
import com.server.Connection;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected static String serverAddress;
    protected static int serverPort;
    protected static Connection connection;
    protected static boolean isConnected = false;
    protected static String userName = "";
    protected static String passWord = "";

    public void tryConnectToServer() throws IOException {
        Socket socket = new Socket(serverAddress, serverPort);
        connection = new Connection(socket);
    }

    private boolean tryReconnectToServer() {
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            try {
                tryConnectToServer();
                return true;
            } catch (IOException ignored) {
            }
        }
        return false;
    }

    public void tryConnectAndReconnect() {
        try {
            tryConnectToServer();
            isConnected = true;
        } catch (Exception e) {
            if (!tryReconnectToServer()) {
                showErrorScene();
                isConnected = false;
            }
        }
    }

    public boolean tryReconnectToChat() {
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            try {
                tryConnectToServer();
                tryLoginToServer(userName, passWord);
                return true;
            } catch (IOException | ClassNotFoundException ignored) {
            }
        }
        return false;
    }

    public MessageType tryLoginToServer(String uName, String pWord) throws IOException, ClassNotFoundException {
        while (true) {
            Message message = connection.read();
            if (message.getMessageType().equals(MessageType.DISABLE_USER))
                connection.send(new Message(MessageType.LOGIN));
            if (message.getMessageType().equals(MessageType.REQUEST_USER_NAME)) {
                userName = uName;
                connection.send(new Message(MessageType.NAME, uName));
            }
            if (message.getMessageType().equals(MessageType.REQUEST_USER_PASSWORD)) {
                passWord = pWord;
                connection.send(new Message(MessageType.PASSWORD, pWord));
            }
            if (message.getMessageType().equals(MessageType.INVALID_LOG_ERROR))
                return message.getMessageType();
            if (message.getMessageType().equals(MessageType.STATE_OF_LOGIN)) {
                return MessageType.SUCCESSFULLY;
            }
        }
    }

    public MessageType tryRegisterToServer(String uName, String pWord) throws IOException, ClassNotFoundException {
        while (true) {
            Message message = connection.read();
            if (message.getMessageType().equals(MessageType.REGISTRATION_OR_LOGIN))
                connection.send(new Message(MessageType.REGISTRATION));
            if (message.getMessageType().equals(MessageType.REQUEST_USER_NAME)) {
                userName = uName;
                connection.send(new Message(MessageType.NAME, uName));
            }
            if (message.getMessageType().equals(MessageType.REQUEST_USER_PASSWORD)) {
                passWord = pWord;
                connection.send(new Message(MessageType.PASSWORD, pWord));
            }
            if (message.getMessageType().equals(MessageType.STATE_OF_LOGIN))
                return MessageType.SUCCESSFULLY;
            if (message.getMessageType().equals(MessageType.USERNAME_USED))
                return message.getMessageType();
        }
    }

    public void requestMessageHistory() throws IOException {
        connection.send(new Message(MessageType.REQUEST_CHAT_HISTORY));
    }

    public void sendMessageOnServer(String text) throws IOException {
        connection.send(new Message(MessageType.TEXT_MESSAGE, userName + ": " + text));
    }

    public String receiveMessageFromServer() throws IOException, ClassNotFoundException {
        Message message = connection.read();
        return message.getMessage();
    }

    public void disableClient() {
        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showErrorScene() {
        FXMLLoader loader11 = new FXMLLoader();
        loader11.setLocation(getClass().getResource("scenes/unableToConnectScene.fxml"));

        Parent root11 = null;
        try {
            root11 = (Parent) loader11.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Stage stage1 = new Stage();
        stage1.setScene(new Scene(root11));
        stage1.setAlwaysOnTop(true);
        stage1.show();
    }
}