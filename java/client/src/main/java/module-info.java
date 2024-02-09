module app.client {
    requires java.net.http;
    requires app.util;

    requires javafx.controls;
    requires javafx.fxml;

    opens app.client to javafx.fxml;
    opens app.client.fx to javafx.fxml;

    exports app.client;
    exports app.client.fx;
}