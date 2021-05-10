package com.client;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.server.ChatOptions;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ControllerChatScene extends Client {
    private boolean isInterrupted = false;

    private final String CONNECTION_LOST = "   System: Соединение прервано, попытка повторного подключения";
    private final String CONNECTION_RETURNED = "   System: Соединение восстановлено";

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private TextArea text_area;

    @FXML
    private TextField text_field;

    @FXML
    private Button send_button;

    @FXML
    private Button exit_button;

    @FXML
    void initialize() {
        send_button.setOnAction(event -> {
            try {
                sendMessageOnServer(text_field.getText().trim());
            } catch (IOException e) {
                e.printStackTrace();
            }
            text_field.clear();
        });

        Thread chat = new Thread(() -> {
            boolean isChatHistoryLoaded = false;
            try {
                List<String> chatHistory = new ArrayList<>();
                while (true) {
                    if (!isInterrupted) {
                        if (!isChatHistoryLoaded) {
                            requestMessageHistory();
                            isChatHistoryLoaded = true;
                        }
                        String message = receiveMessageFromServer();
                        chatHistory.add(0, message);
                        if (chatHistory.size() > ChatOptions.MAX_SIZE_OF_CHAT_HISTORY)
                            chatHistory = chatHistory.subList(0, ChatOptions.MAX_SIZE_OF_CHAT_HISTORY);
                        StringBuilder messages = new StringBuilder();
                        for (String s : chatHistory) {
                            messages.append(s).append("\n");
                        }
                        text_area.setText(messages.toString());
                    } else
                        break;
                }
            } catch (IOException e) {
                if (!isInterrupted) {
                    text_area.setText(CONNECTION_LOST + "\n" + text_area.getText().trim());
                    if (tryReconnectToChat())
                        text_area.setText(CONNECTION_RETURNED + "\n" + text_area.getText().trim());
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
        chat.start();

        exit_button.setOnAction(event -> {
            disableClient();
            isInterrupted = true;
            chat.interrupt();
            Stage stage = (Stage) exit_button.getScene().getWindow();
            stage.close();
        });
    }
}
