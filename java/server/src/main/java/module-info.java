module app.server {
    requires java.net.http;

    exports app.server.demo;
    exports app.server.demo.endpoint;
    exports app.server.demo.client;
}