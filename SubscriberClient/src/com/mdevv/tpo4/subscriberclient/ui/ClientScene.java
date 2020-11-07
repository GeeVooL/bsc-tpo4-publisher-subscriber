package com.mdevv.tpo4.subscriberclient.ui;

import com.google.protobuf.Any;
import com.mdevv.tpo4.common.protobuf.MessageWrapper;
import com.mdevv.tpo4.common.protobuf.ServerResponse;
import com.mdevv.tpo4.common.protobuf.SubscriberMessage;
import com.mdevv.tpo4.common.protobuf.UpdateMessage;
import com.mdevv.tpo4.subscriberclient.net.MessagingService;
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
import java.util.ResourceBundle;

public class ClientScene implements Initializable {
    @FXML
    private ListView<String> messagesList;
    @FXML
    private ChoiceBox<String> topicChoiceBox;

    MessagingService messagingService;
    private ObservableList<String> messages;
    private ObservableList<String> topics;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messages = FXCollections.observableArrayList();
        topics = FXCollections.observableArrayList();

        try {
            messagingService = new MessagingService(this::onMessage);
            messagingService.connect();
            messagingService.startReading();

            register();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        messagesList.setItems(messages);
        topicChoiceBox.setItems(topics);
    }

    private void register() throws IOException {
        SubscriberMessage.Type type = SubscriberMessage.Type.REGISTER;
        SubscriberMessage subscriberMessage = SubscriberMessage.newBuilder().
                setType(type).
                build();

        messagingService.send(subscriberMessage);
        toggleControls(false);
    }

    private void onMessage(MessageWrapper messageWrapper) {
        Platform.runLater(() -> {
            Any message = messageWrapper.getMessage();

            if (message.is(ServerResponse.class)) {
                System.out.println("Server response incoming");
                try {
                    handleResponse(message.unpack(ServerResponse.class));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (message.is(UpdateMessage.class)) {
                System.out.println("Update message incoming");
                try {
                    handleUpdateMessage(message.unpack(UpdateMessage.class));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Error");
            }
        });
    }

    private void handleResponse(ServerResponse message) {
        ServerResponse.Status status = message.getStatus();
        switch (status) {
            case OK:
                if (!message.getContentList().isEmpty()) {
                    topics.addAll(message.getContentList());
                }
                break;
            case NOT_FOUND:
                openErrorAlert("Item not found.");
                break;
            case INVALID_REQUEST:
                openErrorAlert("Request failed.");
                break;
        }

        toggleControls(true);
    }

    private void handleUpdateMessage(UpdateMessage message) {
        UpdateMessage.Type type = message.getType();
        switch (type) {
            case TOPIC_CREATION:
                topics.add(message.getTopic());
                break;
            case TOPIC_DELETION:
                topics.remove(message.getTopic());
                break;
            case MESSAGE:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append('[');
                stringBuilder.append(message.getTopic());
                stringBuilder.append("] ");
                stringBuilder.append(message.getMessage());
                messages.add(stringBuilder.toString());
                break;
        }
    }

    @FXML
    private void registerButtonClicked(Event event) {
        Platform.runLater(() -> {
            toggleControls(false);
            SubscriberMessage message = SubscriberMessage.newBuilder().
                    setType(SubscriberMessage.Type.SUBSCRIBE).
                    setTopic(topicChoiceBox.getValue()).
                    build();
            try {
                messagingService.send(message);
            } catch (Exception e) {
                openErrorAlert("Failed to send request.");
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void unregisterButtonClicked(Event event) {
        Platform.runLater(() -> {
            toggleControls(false);
            SubscriberMessage message = SubscriberMessage.newBuilder().
                    setType(SubscriberMessage.Type.UNSUBSCRIBE).
                    setTopic(topicChoiceBox.getValue()).
                    build();
            try {
                messagingService.send(message);
            } catch (Exception e) {
                openErrorAlert("Failed to send request.");
                e.printStackTrace();
            }
        });
    }

    private void toggleControls(boolean enabled) {
        messagesList.setDisable(!enabled);
        topicChoiceBox.setDisable(!enabled);
    }

    private void openErrorAlert(String message) {
        Alert alert = new Alert(AlertType.ERROR, message, ButtonType.CLOSE);
        alert.showAndWait();
    }
}