package com.wholebrain.mnistreader.infopanel;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

public class Infopanel implements Initializable {
    @FXML public BorderPane info_pane;
    @FXML public Label welcome_label;
    @FXML public VBox info_vbox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        final String welcomeText = "Thanks for using Mnist reader !",
                mnistText = "Make sure to download the MNIST dataset", mnistLink = "http://yann.lecun.com/exdb/mnist/",
                emnistText = "and also the much bigger EMNIST dataset", emnistLink="https://www.nist.gov/node/1298471/emnist-dataset",
                ideaText = "Made with IntelliJ IDEA 2019.1.3", ideaLink ="https://www.jetbrains.com/idea/",
                gitHubText = "My GitHub", gitHubLink ="https://github.com/Whole-Brain",
                contactText = "Contact me at", contactLink="g.wael@outlook.fr";

        welcome_label.setText(welcomeText);

        addTextWithHyperlink(ideaText,ideaLink,mnistText,mnistLink,emnistText,emnistLink,ideaText, ideaLink,gitHubText, gitHubLink,contactText, contactLink);
    }

    /**
     * Adds inside the main {@link VBox layout} a {@link TextFlow}.
     * @param args Even = Descritpion of the link ; Odd = Link that will be clickable to read it inside the default web browser.
     */
    private void addTextWithHyperlink(String... args ) {
        boolean isLink = true;
        String stringBuffer = null;
        for (String string : args){
            isLink=!isLink;
            if(!isLink)
                stringBuffer = string;
            else{
                Hyperlink hyperlink = new Hyperlink(string);
                makeHtmlClickable(hyperlink);
                TextFlow textFlow = new TextFlow(new Text(stringBuffer.concat(" : ")));
                textFlow.getChildren().add(hyperlink);
                textFlow.setTextAlignment(TextAlignment.CENTER);
                info_vbox.getChildren().add(textFlow);
            }

        }
    }

    /**
     * Makes a {@link Hyperlink hyper link} clickable.
     * When clicked, they will open the default web browser of the system.
     * @param hyperlink {@link Hyperlink} to be made clickable.
     */
    private void makeHtmlClickable(Hyperlink hyperlink) {
        hyperlink.setOnAction(e -> {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI(hyperlink.getText()));
                } catch (IOException | URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Calls the closure of the current main {@link Stage window}.
     */
    public void on_action() {
        ((Stage)info_pane.getScene().getWindow()).close();
    }
}
