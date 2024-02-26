I tried connecting a javascript client to my ChatServer, spoiler: I couldn’t :D.
The only message that was received was a request to upgrade to websocket… 
There is no way to connect the js client without significantly reworking my implementation.

The server implementation - https://github.com/BorislavN/javafx-demo/blob/main/unicast/src/main/java/app/unicast/nio/ChatServer.java

So, I decided to create this repository.
Here I will try to implement a cross-platform chat,
one that enables both JavaScript and JavaFX clients to communicate using WebSocket protocol.
In the "minimal" packages you will find plain java implementations.
In the "proper" packages - implementations using the Jakarta WebSocket API.
