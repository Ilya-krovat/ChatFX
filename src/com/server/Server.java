package com.server;

import com.server.message.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private ServerSocket serverSocket;
    private Thread mainThread;
    private Map<String, Connection> users = new ConcurrentHashMap<>();
    private Map<String, String> allUsers = new ConcurrentHashMap<>();
    private List<String> chatHistory = new CopyOnWriteArrayList<>();

    private final String MESSAGES_CLASS_PATH = "messages.txt";
    private final String USERS_CLASS_PATH = "users.txt";

    public void launch() {
        try {
            loadData();
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(ChatOptions.PORT));
            mainThread = new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        new ServerThread(socket).start();
                    } catch (Exception e) {
                        break;
                    }
                }
            });
            Thread dataSave = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(60000);
                        saveData();
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            mainThread.start();
            dataSave.start();
            System.out.println("server started");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToAllUsers(Message message) {
        chatHistory.add(0, message.getMessage());
        if(chatHistory.size()>ChatOptions.MAX_SIZE_OF_CHAT_HISTORY)
            chatHistory = chatHistory.subList(0, ChatOptions.MAX_SIZE_OF_CHAT_HISTORY);
        for (Map.Entry<String, Connection> user : users.entrySet()) {
            try {
                user.getValue().send(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String registerAndAddingUser(Connection connection) {
        while (true) {
            try {
                connection.send(new Message(MessageType.REGISTRATION_OR_LOGIN));
                Message registrationOrLogin = connection.read();
                connection.send(new Message(MessageType.REQUEST_USER_NAME));
                String userName = connection.read().getMessage();
                connection.send(new Message(MessageType.PASSWORD));
                String password = connection.read().getMessage();
                if (registrationOrLogin.getMessageType().equals(MessageType.REGISTRATION)) {
                    if (allUsers.containsKey(userName)) {
                        connection.send(new Message(MessageType.USERNAME_USED));
                        continue;
                    } else {
                        allUsers.put(userName, password);
                        users.put(userName, connection);
                        sendMessageToAllUsers(new Message(MessageType.USER_ADDED, "\n" + userName + MessageType.USER_ENTER + "\n"));
                        connection.send(new Message(MessageType.STATE_OF_LOGIN, MessageType.SUCCESSFULLY));
                        return userName;
                    }
                } else if (registrationOrLogin.getMessageType().equals(MessageType.LOGIN)) {
                    if (allUsers.containsKey(userName) && allUsers.get(userName).equals(password)) {
                        connection.send(new Message(MessageType.STATE_OF_LOGIN, MessageType.SUCCESSFULLY));
                        sendMessageToAllUsers(new Message(MessageType.USER_ADDED, "\n" + userName + MessageType.USER_ENTER + "\n"));
                        users.put(userName, connection);
                        return userName;
                    } else
                        connection.send(new Message(MessageType.INVALID_LOG_ERROR));
                }
            } catch (Exception e) {
                try {
                    connection.send(new Message(MessageType.ENTER_ERROR));
                } catch (Exception ex) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void messaging(Connection connection, String userName) {
        while (true) {
            try {
                Message message = connection.read();
                if (message.getMessageType().equals(MessageType.TEXT_MESSAGE)) {
                    sendMessageToAllUsers(new Message(MessageType.TEXT_MESSAGE, message.getMessage()));
                }
                if (message.getMessageType().equals(MessageType.DISABLE_USER)) {
                    users.remove(userName);
                    sendMessageToAllUsers(new Message(MessageType.DISABLE_USER, "\n" + userName + MessageType.USER_LEAVE + "\n"));
                    connection.close();
                    break;
                }
                if (message.getMessageType().equals(MessageType.REQUEST_CHAT_HISTORY)) {
                    for(int i = chatHistory.size()-1 ; i>=0 ; i--)
                    {
                        connection.send(new Message(MessageType.TEXT_MESSAGE, chatHistory.get(i)));
                    }

                }
            } catch (Exception e) {
                users.remove(userName);
                sendMessageToAllUsers(new Message(MessageType.DISABLE_USER, "\n" + userName + MessageType.USER_LEAVE + "\n"));
                try {
                    connection.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
                break;
            }
        }
    }

    private class ServerThread extends Thread {

        private final Socket socket;

        public ServerThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                Connection connection = new Connection(socket);
                String userName = registerAndAddingUser(connection);
                messaging(connection, userName);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadData() throws Exception {
        File file = new File(MESSAGES_CLASS_PATH);
        if (!file.exists())
            file.createNewFile();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while (reader.ready()) {
            chatHistory.add(reader.readLine());
        }
        reader.close();

        file = new File(USERS_CLASS_PATH);
        if (!file.exists())
            file.createNewFile();
        reader = new BufferedReader(new FileReader(file));
        String userName = "";
        boolean isUserData = true;
        while (reader.ready()) {
            String userData = reader.readLine();
            if (isUserData)
                userName = userData;
            else
                allUsers.put(userName, userData);
            isUserData = !isUserData;
        }
        reader.close();
    }

    private void saveData() throws IOException {
        File file = new File(MESSAGES_CLASS_PATH);
        FileWriter fileWriter = new FileWriter(MESSAGES_CLASS_PATH);
        for (String s : chatHistory)
            fileWriter.write(s + "\n");
        fileWriter.close();

        File file1 = new File(USERS_CLASS_PATH);
        fileWriter = new FileWriter(USERS_CLASS_PATH);
        for (Map.Entry<String, String> user : allUsers.entrySet()) {
            fileWriter.write(user.getKey() + "\n");
            fileWriter.write(user.getValue() + "\n");
        }
        fileWriter.close();
    }

    public void stop() {
        try {
            saveData();
            mainThread.interrupt();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}