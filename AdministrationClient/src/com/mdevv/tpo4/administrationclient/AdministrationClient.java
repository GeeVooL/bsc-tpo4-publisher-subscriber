package com.mdevv.tpo4.administrationclient;

import com.mdevv.tpo4.common.Configuration;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;

public class AdministrationClient extends Application {
    public static InetSocketAddress SERVER_ADDRESS;
    private static String SERVER_HOST_FIELD = "SERVER_HOST";
    private static String SERVER_PORT_FIELD = "SERVER_PORT";
    public static Configuration configuration;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("ui/ClientScene.fxml"));
        primaryStage.setTitle("Administration Client");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) throws IOException {
        AdministrationClient.configuration = Configuration.fromCommandLine(args, Arrays.asList(
                SERVER_HOST_FIELD,
                SERVER_PORT_FIELD
        ));

        SERVER_ADDRESS = new InetSocketAddress(
                configuration.get(SERVER_HOST_FIELD),
                configuration.getAsInt(SERVER_PORT_FIELD)
        );

        Application.launch(args);
    }
}
