package app.server;

import app.server.minimal.JavaServer;
import app.server.proper.JakartaServer;

import java.util.Scanner;

public class ServerLauncher {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Hello, to start the Java server implementation enter \"1\".");
        System.out.println("To start the Jakarta implementation enter \"2\".");
        System.out.println("To abort the server initialization enter \"stop\".");

        String type;

        while (!"stop".equals(type = scanner.nextLine())) {
            if (type.isBlank()) {
                System.out.println("Server type cannot be blank!");
                continue;
            }

            WebsocketServer server = null;

            if ("1".equals(type)) {
                server = new JavaServer(80);
            }

            if ("2".equals(type)) {
                server = new JakartaServer(80);
            }

            if (server == null) {
                System.out.println("Invalid type!");
                continue;
            }

            System.out.println("The server will shutdown automatically, when there are no users left...");

            server.start();
            scanner.close();

            break;
        }
    }
}