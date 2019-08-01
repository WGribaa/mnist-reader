module MnistReader {
    requires javafx.fxml;
    requires javafx.controls;
    requires javafx.graphics;
    requires java.desktop;
    requires javafx.web;

    exports com.wholebrain.mnistreader.infopanel;

    opens com.wholebrain.mnistreader;
}