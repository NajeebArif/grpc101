package narif.poc.grpc101.greetings;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class GreetingsServer {

    private static final Logger logger = Logger.getLogger(GreetingsServer.class.getName());

    private Server server;

    private void start() throws IOException {
        int port = 50051;
        server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(new GreetingsService())
                .build()
                .start();
        logger.info("Server started listening on port: " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("**** shutting down grpc server as jvm is shutting down");
            try {
                GreetingsServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("**** server shut down");
        }));
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final var greetingsServer = new GreetingsServer();
        greetingsServer.start();
        greetingsServer.blockUntilShutdown();
    }
}
