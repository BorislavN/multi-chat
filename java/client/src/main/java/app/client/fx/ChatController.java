package app.client.fx;

import app.client.websocket.minimal.Client;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class ChatController {
    @FXML
    private VBox usernamePage, mainPage;
    @FXML
    private Label errorMessage;
    @FXML
    private TextField usernameInput;
    @FXML
    private Button joinBtn;
    @FXML
    private Label announcement;
    @FXML
    private TextArea textArea;
    @FXML
    private TextField messageInput;
    @FXML
    private Button sendBtn;
    private String username;
    private Client client;
//    private SenderService senderService;
//    private ReceiverService receiverService;

    public ChatController() {
        this.username = "";
        this.client = null;
//        this.senderService = null;
//        this.receiverService = null;
    }

    @FXML
    public void onJoinClick(ActionEvent actionEvent) {
//        actionEvent.consume();
//
//        String username = this.usernameInput.getText();
//
//        if (username.isBlank()) {
//            this.errorMessage.setText("Username cannot be blank!");
//            this.errorMessage.setVisible(true);
//            return;
//        }
//
//        if (username.length() > MulticastClient.USERNAME_LIMIT) {
//            this.errorMessage.setText(String.format("Username too long, limit %d B", MulticastClient.USERNAME_LIMIT));
//            this.errorMessage.setVisible(true);
//            return;
//        }
//
//        if ("".equals(this.username)) {
//            if (this.receiverService == null) {
//                this.receiverService = new ReceiverService(this.client);
//
//                this.receiverService.messageListProperty().getValue().addListener(this.getChangeHandler());
//            } else {
//                this.receiverService.cancel();
//                this.receiverService.reset();
//            }
//
//            this.receiverService.start();
//
//            this.senderService.sendMessage(String.format("%s joined the chat!", username));
//
//        } else if (!username.equals(this.username)) {
//            this.senderService.sendMessage(String.format("%s changed their name to %s", this.username, username));
//        }
//
//        this.username = username;
//
//        this.errorMessage.setVisible(false);
//        this.usernamePage.setManaged(false);
//        this.usernamePage.setVisible(false);
//
//        this.announcement.setText(String.format("Welcome, %s!", this.username));
//        this.announcement.setStyle("-fx-background-color: #515254");
//
//        this.mainPage.setManaged(true);
//        this.mainPage.setVisible(true);
//
//        this.resize();
    }

    @FXML
    public void onSend(ActionEvent actionEvent) {
//        actionEvent.consume();
//
//        String message = this.messageInput.getText();
//
//        if (message.isBlank()) {
//            this.messageInput.setStyle("-fx-border-color: red");
//            this.announcement.setText("Message cannot be blank!");
//            this.announcement.setStyle("-fx-background-color: #eb4d42");
//            return;
//        }
//
//        if (message.length() > MulticastClient.MESSAGE_LIMIT) {
//            this.messageInput.setStyle("-fx-border-color: red");
//            this.announcement.setText(String.format("Message too long, limit %d B", MulticastClient.MESSAGE_LIMIT));
//            this.announcement.setStyle("-fx-background-color: #eb4d42");
//            return;
//        }
//
//        this.senderService.sendMessage(this.username, message);
//
//        this.announcement.setText(String.format("Welcome, %s!", this.username));
//        this.announcement.setStyle("-fx-background-color: #515254");
//
//        this.messageInput.setStyle("");
//        this.messageInput.clear();
    }

    @FXML
    public void onEnter(ActionEvent actionEvent) {
        actionEvent.consume();

        String targetId = ((Node) actionEvent.getTarget()).getId();

        if ("usernameInput".equals(targetId)) {
            this.joinBtn.fire();
        }

        if ("messageInput".equals(targetId)) {
            this.sendBtn.fire();
        }
    }

    @FXML
    public void onChangeName(ActionEvent actionEvent) {
        actionEvent.consume();

        this.mainPage.setManaged(false);
        this.mainPage.setVisible(false);

        this.errorMessage.setVisible(false);
        this.usernamePage.setManaged(true);
        this.usernamePage.setVisible(true);

        this.resize();
    }

    public void onClose(WindowEvent event, Stage stage) {
//        event.consume();
//
//        if (!"".equals(this.username)) {
//            //We send it directly through the client, because the service
//            //uses demon threads and they are terminated when the stage closes
//            //Thus before the message is delivered
//            this.client.sendMessage(String.format("%s left the chat...", this.username));
//        }
//
//        if (this.client != null) {
//            this.client.closeChannel();
//        }
//
//        stage.close();
    }

    public void onShowSettings(ActionEvent event) {
//        event.consume();
//
//        if (!"".equals(this.username)) {
//            this.senderService.sendMessage(String.format("%s disconnected...", this.username));
//        }
//
//        boolean wasChanged = SettingsInitializer.showSettings(this.client);
//
//        if (wasChanged) {
//            this.textArea.clear();
//            this.username = "";
//
//            return;
//        }
//
//        if (!"".equals(this.username)) {
//            this.senderService.sendMessage(String.format("%s reconnected...", this.username));
//        }
    }

    public void configureClient() {
//        try {
//            this.client = new MulticastClient("eth2");
//            this.senderService = new SenderService(this.client);
//
//        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
//            System.err.println("Client failed to initialize - " + e.getMessage());
//
//            this.joinBtn.setDisable(true);
//            this.errorMessage.setText("Client failed to initialize!");
//            this.errorMessage.setVisible(true);
//        }
    }

    private ListChangeListener<String> getChangeHandler() {
        return change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    this.textArea.appendText(change.getList().get(change.getFrom()));
                }
            }
        };
    }

    //There may be better ways to get the stage
    //This code should avoid NullPointerException, because it is only called in button-click handlers
    private void resize() {
        this.usernamePage.getScene().getWindow().sizeToScene();
    }
}