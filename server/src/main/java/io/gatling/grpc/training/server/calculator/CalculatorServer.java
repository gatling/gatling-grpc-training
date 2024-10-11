package io.gatling.grpc.training.server.calculator;

import java.io.IOException;

import io.gatling.grpc.training.server.Configuration;
import io.gatling.grpc.training.server.JwtServerInterceptor;

import io.grpc.*;

public class CalculatorServer {
    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50052;
        boolean plaintext = Boolean.getBoolean("plaintext");

        System.out.println("Starting server on port :" + port);

        ServerBuilder<?> serverBuilder;
        if (plaintext) {
            System.out.println("-> Plaintext mode");
            serverBuilder = ServerBuilder.forPort(port);
        } else {
            boolean mutualAuth = Boolean.getBoolean("mutualauth");
            if (mutualAuth) {
                System.out.println("-> TLS mode w/ mutual auth \033[1menabled\033[0m");
            } else {
                System.out.println("-> TLS mode w/ mutual auth \033[1mdisabled\033[0m");
            }
            ServerCredentials credentials = Configuration.credentials(mutualAuth);
            serverBuilder = Grpc.newServerBuilderForPort(port, credentials);
        }

        serverBuilder.addService(new CalculatorServiceImpl()).intercept(new CalculatorServerInterceptor());

        if (Boolean.getBoolean("jwt")) {
            System.out.println("-> JWT \033[1menabled\033[0m");
            serverBuilder.intercept(new JwtServerInterceptor());
        }

        Server server = serverBuilder.build();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Received shutdown request");
            server.shutdown();
            System.out.println("Successfully stopped the server");
        }));
        server.awaitTermination();
    }
}
