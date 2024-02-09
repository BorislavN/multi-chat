//package app.client.fx.service;
//
//import app.multicast.client.MulticastClient;
//import javafx.beans.property.ReadOnlyObjectProperty;
//import javafx.beans.property.ReadOnlyObjectWrapper;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.concurrent.Service;
//import javafx.concurrent.Task;
//
////"ReadOnlyObjectWrapper" creates a list property which cannot be reassigned
////The list itself is NOT unmodified
//public class ReceiverService extends Service<Void> {
//    private final MulticastClient client;
//    private final ReadOnlyObjectWrapper<ObservableList<String>> messageList;
//
//    public ReceiverService(MulticastClient client) {
//        this.client = client;
//        this.messageList = new ReadOnlyObjectWrapper<>(FXCollections.observableArrayList());
//    }
//
//    public ReadOnlyObjectProperty<ObservableList<String>> messageListProperty() {
//        return this.messageList.getReadOnlyProperty();
//    }
//
//    @Override
//    protected Task<Void> createTask() {
//        return new ReceiverTask(this.client, this.messageList.getValue());
//    }
//}