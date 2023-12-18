I tried connecting a javascript client to my ChatServer, spoiler: I couldn’t :D.
The only message that was received was a request to upgrade to websocket… 
There is no way to connect the js client without significantly reworking my implementation.

The server implementation - https://github.com/BorislavN/javafx-demo/blob/main/unicast/src/main/java/app/unicast/nio/ChatServer.java

Looked around on the web and found a solution, that enables socket to websocket communication.
While it does work, it seems like black magic to say the least.

Code here - https://github.com/BorislavN/multi-chat/blob/main/java/server/src/main/java/app/server/WebSocketDemo.java

So, I decided to create this repository.
Here I will try to implement a cross-platform chat,
one that enables both javascript and javafx clients to connect to the server.
Using a java websocket library to simplify the server logic.
