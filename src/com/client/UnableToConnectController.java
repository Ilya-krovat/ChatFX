package com.client;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class UnableToConnectController extends Client {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button ok_button;

    @FXML
    void initialize() {
        ok_button.setOnAction(event -> {
            Stage stage = (Stage) ok_button.getScene().getWindow();
            stage.close();
        });
    }
}
