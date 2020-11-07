package com.mdevv.tpo4.administrationclient.ui;

import com.mdevv.tpo4.administrationclient.net.MessagingService;
import com.mdevv.tpo4.common.protobuf.ServerResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class ClientScene implements Initializable {
    private final static String CREATE_REQUEST = "Create";
    private final static String DELETE_REQUEST = "Delete";
    private final static String MESSAGE_REQUEST = "Send message";

    @FXML
    private TextArea messageTextArea;
    @FXML
    private ChoiceBox<String> requestChoiceBox;
    @FXML
    private TextField topicTextField;
    @FXML
    private Button sendButton;

    MessagingService messagingService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            messagingService = new MessagingService();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        requestChoiceBox.setItems(FXCollections.observableArrayList(Arrays.asList(
                CREATE_REQUEST,
                DELETE_REQUEST,
                MESSAGE_REQUEST
        )));
    }

    @FXML
    public void sendButtonClicked(Event e) {
        Platform.runLater(() -> {
            toggleControls(false);
            sendRequest();
            toggleControls(true);
        });
    }

    private void toggleControls(boolean enabled) {
        messageTextArea.setDisable(!enabled);
        requestChoiceBox.setDisable(!enabled);
        topicTextField.setDisable(!enabled);
        sendButton.setDisable(!enabled);
    }

    private void sendRequest() {
        ServerResponse.Status status;

        switch (requestChoiceBox.getValue()) {
            case CREATE_REQUEST:
                status = messagingService.createTopic(topicTextField.getText());
                break;
            case DELETE_REQUEST:
                status = messagingService.deleteTopic(topicTextField.getText());
                break;
            case MESSAGE_REQUEST:
                status = messagingService.createMessage(topicTextField.getText(), messageTextArea.getText());
                break;
            default:
                status = ServerResponse.Status.INVALID_REQUEST;
        }

        switch (status) {
            case INVALID_REQUEST:
                openErrorAlert("Request failed.");
                break;
            case NOT_FOUND:
                openErrorAlert("Value not found.");
                break;
        }
    }

    private void openErrorAlert(String message) {
        Alert alert = new Alert(AlertType.ERROR, message, ButtonType.CLOSE);
        alert.showAndWait();
    }
}