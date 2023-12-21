module app.client {
    requires javafx.controls;
    requires javafx.fxml;

    opens app.client to javafx.fxml;

    exports app.client;
    exports app.client.fx;
    opens app.client.fx to javafx.fxml;
}