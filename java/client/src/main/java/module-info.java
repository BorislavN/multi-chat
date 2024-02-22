module app.client {
    requires app.util;

    requires java.net.http;
    requires tyrus.standalone.client;

    requires javafx.controls;
    requires javafx.fxml;

    opens app.client to javafx.fxml;

    exports app.client;
    exports app.client.websocket.proper;
}