package app.client;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class JavaApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Helper.initializeStage(stage, 1);
    }

    public static void main(String[] args) {
        launch();
    }
}