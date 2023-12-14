module app.chat {
    requires javafx.controls;
    requires javafx.fxml;

    opens app.chat to javafx.fxml;
    exports app.chat;
}