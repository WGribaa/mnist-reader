package com.wholebrain.mnistreader;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

@SuppressWarnings("WeakerAccess")
public class ProgressDialog extends Stage{
    private Label processLabel = new Label();
    private ProgressBar progressBar = new ProgressBar(0);

    ProgressDialog(String processName, int elementsCount, Task task) {
        processLabel.setText(processName + " 0 / " + elementsCount);

        task.progressProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
            progressBar.setProgress(newValue.doubleValue());
            processLabel.setText(processName+" "+(newValue.intValue()*elementsCount)+" / "+elementsCount);
        }));
        task.setOnSucceeded(wse -> this.close());

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        HBox.setMargin(hBox,new Insets(10));
        hBox.getChildren().addAll(processLabel,progressBar);
        Scene scene = new Scene(hBox);
        this.setTitle("Please wait...");
        this.setScene(scene);
        this.setResizable(false);
        this.initModality(Modality.APPLICATION_MODAL);
        new Thread(task).start();
        this.showAndWait();
    }
}
