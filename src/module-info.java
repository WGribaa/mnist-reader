module MnistReader {
    requires javafx.base;
    requires javafx.swt;
    requires javafx.media;
    requires javafx.fxml;
    requires javafx.controls;
    requires javafx.graphics;
    requires java.desktop;
    requires javafx.web;
    requires javafx.swing;

    exports com.wholebrain.mnistreader.infopanel;

    opens com.wholebrain.mnistreader;
}