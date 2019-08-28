package com.wholebrain.mnistreader;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));

        Parent root = loader.load();
        primaryStage.setTitle("Datasets Images Reader");
        primaryStage.setScene(new Scene(root,420,420));
        primaryStage.setMinWidth(324); // before the CustomCanvas update :  294
        primaryStage.setMinHeight(465);
        primaryStage.centerOnScreen();
        primaryStage.setOnCloseRequest(event -> System.exit(0));
        primaryStage.show();

        Controller controller = loader.getController();
        controller.setStage(primaryStage);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
