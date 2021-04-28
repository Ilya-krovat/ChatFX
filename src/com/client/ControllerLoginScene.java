package com.client;

import com.server.message.MessageType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ControllerLoginScene extends Client {

    @FXML
    private Button login_button;

    @FXML
    private PasswordField password_field;

    @FXML
    private TextField login_field;

    @FXML
    private Button register_button;

    @FXML
    private TextField error_field;

    @FXML
    void initialize() {
        try {
            tryConnectToServer();
        } catch (IOException e) {
            if (!tryReconnectToServer()) {
                showErrorScene();
            }
        }
        login_button.setOnAction(event -> {
            try {
                String state = tryLoginToServer(login_field.getText().trim(), password_field.getText().trim());
                if (state.equals(MessageType.INVALID_LOG_ERROR)) {
                    error_field.setVisible(true);
                    return;
                }

                Stage stage = (Stage) login_button.getScene().getWindow();
                stage.close();

                FXMLLoader loader1 = new FXMLLoader();
                loader1.setLocation(getClass().getResource("scenes/chatScene.fxml"));

                Parent root1 = null;
                try {
                    root1 = (Parent) loader1.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                stage = new Stage();
                stage.setScene(new Scene(root1));
                stage.show();
            } catch (Exception e) {
                showErrorScene();
            }
        });

        register_button.setOnAction(event -> {
            Stage stage = (Stage) register_button.getScene().getWindow();
            stage.close();

            FXMLLoader loader1 = new FXMLLoader();
            loader1.setLocation(getClass().getResource("scenes/registerScene.fxml"));

            Parent root1 = null;
            try {
                root1 = (Parent) loader1.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            stage = new Stage();
            stage.setScene(new Scene(root1));
            stage.show();
        });
    }
}