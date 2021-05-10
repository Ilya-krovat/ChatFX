package com.client;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ControllerChangeServerScene extends Client {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private TextField ip_field;

    @FXML
    private TextField port_field;

    @FXML
    private Button ok_button;

    @FXML
    void initialize() {
        ok_button.setOnAction(event -> {
            serverAddress = ip_field.getText().trim();
            try {
                serverPort = Integer.parseInt(port_field.getText().trim());
            } catch (NumberFormatException ignored) {
            }
            Stage stage = (Stage) ok_button.getScene().getWindow();
            stage.close();
        });
    }
}
