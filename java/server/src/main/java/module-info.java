module app.server {
    requires java.net.http;
    requires app.util;

    exports app.server.demo.endpoint;
    exports app.server.demo.client;
}