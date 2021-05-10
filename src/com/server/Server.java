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
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Server {
    private ServerSocket serverSocket;
    private Thread mainThread;
    private Map<String, Connection> users = new ConcurrentHashMap<>();
    private Map<String, String> allUsers = new ConcurrentHashMap<>();
    private List<String> chatHistory = new CopyOnWriteArrayList<>();

    private final String MESSAGES_CLASS_PATH = "messages.txt";
    private final String USERS_CLASS_PATH = "users.txt";

    private final String USER_ADDED = "   подключился к чату";
    private final String USER_LEFT = "   покинул чат";


    static Logger LOGGER;
    static {
        try{
            File file = new File("log.config");
            if (!file.exists())
                    file.createNewFile();
            FileInputStream ins = new FileInputStream("log.config");
            LogManager.getLogManager().readConfiguration(ins);
            LOGGER = Logger.getLogger(Server.class.getName());
        }catch (Exception ignore){
            ignore.printStackTrace();
        }
    }

    public void launch() {
        try {
            loadData();
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(ChatOptions.PORT));
            mainThread = new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        LOGGER.log(Level.INFO, "Найдено новое соединение");
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
                        LOGGER.log(Level.INFO,"Сохранение данных в файл");
                        saveData();
                    } catch (InterruptedException | IOException e) {
                        LOGGER.log(Level.WARNING,"Ошибка при попытки сохранения данных " + e.getMessage());
                    }
                }
            });

            mainThread.start();
            dataSave.start();
            LOGGER.log(Level.INFO,"сервер запустился");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage());
        }
    }

    private void sendMessageToAllUsers(Message message) {
        LOGGER.log(Level.INFO, "Отправка всем сообщения " + message.getMessage());
        chatHistory.add(0, message.getMessage());
        if(chatHistory.size()>ChatOptions.MAX_SIZE_OF_CHAT_HISTORY)
            chatHistory = chatHistory.subList(0, ChatOptions.MAX_SIZE_OF_CHAT_HISTORY);
        for (Map.Entry<String, Connection> user : users.entrySet()) {
            try {
                user.getValue().send(message);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Ошибка при попытке отправить всем сообщение " + message.getMessage() + "\n" + e.getMessage());
            }
        }
    }

    private String registerAndAddingUser(Connection connection) {
        LOGGER.log(Level.INFO, "Добавление нового пользователя");
        while (true) {
            try {
                connection.send(new Message(MessageType.REGISTRATION_OR_LOGIN));
                Message registrationOrLogin = connection.read();
                connection.send(new Message(MessageType.REQUEST_USER_NAME));
                String userName = connection.read().getMessage();
                connection.send(new Message(MessageType.REQUEST_USER_PASSWORD));
                String password = connection.read().getMessage();
                if (registrationOrLogin.getMessageType().equals(MessageType.REGISTRATION)) {
                    if (allUsers.containsKey(userName)) {
                        connection.send(new Message(MessageType.USERNAME_USED));
                        continue;
                    } else {
                        allUsers.put(userName, password);
                        users.put(userName, connection);
                        sendMessageToAllUsers(new Message(MessageType.USER_ADDED, "\n" + userName + USER_ADDED + "\n"));
                        connection.send(new Message(MessageType.STATE_OF_LOGIN));
                        LOGGER.log(Level.INFO,"Пользователь " + userName + " успешно добавлен");
                        return userName;
                    }
                } else if (registrationOrLogin.getMessageType().equals(MessageType.LOGIN)) {
                    if (allUsers.containsKey(userName) && allUsers.get(userName).equals(password)) {
                        connection.send(new Message(MessageType.STATE_OF_LOGIN));
                        sendMessageToAllUsers(new Message(MessageType.USER_ADDED, "\n" + userName + USER_ADDED + "\n"));
                        users.put(userName, connection);
                        LOGGER.log(Level.INFO,"Пользователь " + userName + " успешно добавлен");
                        return userName;
                    } else {
                        connection.send(new Message(MessageType.INVALID_LOG_ERROR));
                    }
                }
            } catch (Exception e) {
                try {
                    LOGGER.log(Level.WARNING, "Ошибка при добавлении пользователя " + e.getMessage());
                    connection.send(new Message(MessageType.ENTER_ERROR));
                    break;
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Критическая ошибка при добавлении пользователя (нет соединения) " + ex.getMessage());
                    break;
                }
            }
        }
        return "";
    }

    private void messaging(Connection connection, String userName) {
        LOGGER.log(Level.INFO, "Пользоваетль " + userName+ " добавлен в чат");
        while (true) {
            try {
                Message message = connection.read();
                if (message.getMessageType().equals(MessageType.TEXT_MESSAGE)) {
                    sendMessageToAllUsers(new Message(MessageType.TEXT_MESSAGE, message.getMessage()));
                }
                if (message.getMessageType().equals(MessageType.DISABLE_USER)) {
                    LOGGER.log(Level.INFO, "Пользователь " + userName + " отключается");
                    users.remove(userName);
                    sendMessageToAllUsers(new Message(MessageType.DISABLE_USER, "\n" + userName + USER_LEFT + "\n"));
                    connection.close();
                    break;
                }
                if (message.getMessageType().equals(MessageType.REQUEST_CHAT_HISTORY)) {
                    LOGGER.log(Level.INFO, "Пользователь " + userName + " запрашивает исотрию чата");
                    for(int i = chatHistory.size()-1 ; i>=0 ; i--)
                    {
                        connection.send(new Message(MessageType.TEXT_MESSAGE, chatHistory.get(i)));
                    }

                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Потеря связи с пользователем " + userName+ "\n"+ e.getMessage());
                users.remove(userName);
                sendMessageToAllUsers(new Message(MessageType.DISABLE_USER, "\n" + userName + USER_LEFT + "\n"));
                try {
                    connection.close();
                } catch (IOException ioException) {
                    LOGGER.log(Level.WARNING, "Ошибка при попытки закрытия соединения с пользователем " + userName+ "\n"+ e.getMessage());
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
                LOGGER.log(Level.WARNING, "Ошибка в потоке ServerThread" + e.getMessage());
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