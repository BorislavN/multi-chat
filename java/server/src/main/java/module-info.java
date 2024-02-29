module app.server {
    requires org.glassfish.tyrus.server;

    requires java.net.http;
    requires app.util;

    exports app.server;
    exports app.server.proper;
}