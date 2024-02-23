package app.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Helper {
    public static void initializeStage(Stage stage, int type) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(JavaApplication.class.getResource("chat-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        URL cssUrl = JavaApplication.class.getResource("style.css");

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        ChatController controller = fxmlLoader.getController();
        stage.setOnCloseRequest((event -> controller.onClose(event, stage)));
        controller.configureClient(type);

        String title="Chat Client";

        if (1==type){
            title="Java Client";
        }

        if (2==type){
            title="Jakarta Client";
        }

        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }
}
