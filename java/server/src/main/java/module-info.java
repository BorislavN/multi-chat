module app.server {
    requires java.net.http;
    requires app.util;

    exports app.server.minimal;
    exports app.server.minimal.entity;
    exports app.server.minimal.helper;
    exports app.server.minimal.exception;
}