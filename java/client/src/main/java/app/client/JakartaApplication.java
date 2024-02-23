package app.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class JakartaApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
      Helper.initializeStage(stage,2);
    }

    public static void main(String[] args) {
        launch();
    }
}