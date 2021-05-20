package com.client;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.server.message.MessageType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ControllerRegistrationScene extends Client {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button registration_button;

    @FXML
    private PasswordField password_field;

    @FXML
    private TextField login_field;

    @FXML
    private TextField error_text_field;

    @FXML
    void initialize() {
        registration_button.setOnAction(event -> {
            String login = login_field.getText().trim();
            String password = password_field.getText().trim();
            if(!isConnected){
                try {
                    tryConnectToServer();
                } catch (IOException e) {
                    showErrorScene();
                }
            }
            try {
                if(!checkPassword(password))
                    return;
                MessageType state = tryRegisterToServer(login,password);
                if (state.equals(MessageType.USERNAME_USED)) {
                    error_text_field.setVisible(true);
                    return;
                }
            } catch (IOException e) {
                showErrorScene();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }


            Stage stage = (Stage) registration_button.getScene().getWindow();
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
        });
    }

    private boolean checkPassword(String password){
        if(password.length()<=4) {
            //show "password is too short"
            return false;
        }
        if(password.length()>10) {
            //show "password is too long"
            return false;
        }
        return true;
    }
}
