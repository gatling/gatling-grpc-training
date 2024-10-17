package io.gatling.grpc.training;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import io.gatling.grpc.training.calculator.*;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.grpc.GrpcBidirectionalStreamingServiceBuilder;
import io.gatling.javaapi.grpc.GrpcClientStreamingServiceBuilder;
import io.gatling.javaapi.grpc.GrpcProtocolBuilder;
import io.gatling.javaapi.grpc.GrpcServerStreamingServiceBuilder;

import io.grpc.Metadata;
import io.grpc.Status;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.grpc.GrpcDsl.*;

public class CalculatorSimulation extends Simulation {

    GrpcProtocolBuilder baseGrpcProtocol = grpc.forAddress("localhost", 50052)
            .callCredentials(new JwtCredentials("{\"message\":\"salutations maximales\"}"))
            .channelCredentials("#{channelCredentials}")
            .overrideAuthority("gatling-grpc-training-test-server");

    ScenarioBuilder unary = scenario("Calculator Unary")
            .feed(Feeders.channelCredentials())
            .exec(grpc("Sum")
                    .unary(CalculatorServiceGrpc.getSumMethod())
                    .send(SumRequest.newBuilder()
                            .setFirstNumber(1)
                            .setSecondNumber(2)
                            .build())
                    .header(Metadata.Key.of("header", Metadata.ASCII_STRING_MARSHALLER))
                    .value("value")
                    // .asciiHeader("header")
                    // .value("value")
                    .header(Metadata.Key.of("header-bin", Metadata.BINARY_BYTE_MARSHALLER))
                    .value("value".getBytes(StandardCharsets.UTF_8))
                    // .deadlineAfter(Duration.ofMillis(100))
                    .check(
                            statusCode().is(Status.Code.OK),
                            response(SumResponse::getSumResult).is(3)));

    ScenarioBuilder deadlines = scenario("Calculator w/ Deadlines")
            .feed(Feeders.channelCredentials())
            .exec(grpc("Square Root")
                    .unary(CalculatorServiceGrpc.getSquareRootMethod())
                    .send(SquareRootRequest.newBuilder().setNumber(-2).build())
                    .check(statusCode().is(Status.Code.INVALID_ARGUMENT)));

    GrpcServerStreamingServiceBuilder<PrimeNumberDecompositionRequest, PrimeNumberDecompositionResponse> serverStream =
            grpc("Prime Number Decomposition")
                    .serverStream(CalculatorServiceGrpc.getPrimeNumberDecompositionMethod())
                    .messageResponseTimePolicy(MessageResponseTimePolicy.FromLastMessageReceived)
                    .check(
                            statusCode().is(Status.Code.OK),
                            response(PrimeNumberDecompositionResponse::getPrimeFactor)
                                    .transform(p -> p == 2L || p == 5L || p == 17L || p == 97L || p == 6669961L)
                                    .is(true)
                            // asciiTrailer("example-trailer").is("wrong value"),
                            // asciiHeader("example-header").is("wrong value")
                            );

    ScenarioBuilder serverStreaming = scenario("Calculator Server Streaming")
            .feed(Feeders.channelCredentials())
            .exec(
                    serverStream.send(PrimeNumberDecompositionRequest.newBuilder()
                            .setNumber(109987656890L)
                            .build()),
                    serverStream.awaitStreamEnd());

    GrpcClientStreamingServiceBuilder<ComputeAverageRequest, ComputeAverageResponse> clientStream =
            grpc("Compute Average")
                    .clientStream(CalculatorServiceGrpc.getComputeAverageMethod())
                    .messageResponseTimePolicy(MessageResponseTimePolicy.FromLastMessageSent)
                    .check(
                            statusCode().is(Status.Code.OK),
                            response(ComputeAverageResponse::getAverage).saveAs("average"));

    ScenarioBuilder clientStreaming = scenario("Calculator Client Streaming")
            .feed(Feeders.channelCredentials())
            .exec(
                    clientStream.start(),
                    repeat(10)
                            .on(
                                    clientStream.send(session -> {
                                        int number = ThreadLocalRandom.current().nextInt(0, 1000);
                                        return ComputeAverageRequest.newBuilder()
                                                .setNumber(number)
                                                .build();
                                    }),
                                    pause(Duration.ofMillis(500))),
                    clientStream.halfClose(),
                    clientStream.awaitStreamEnd((main, forked) -> {
                        double average = forked.getDouble("average");
                        return main.set("average", average);
                    }),
                    exec(session -> {
                        double average = session.getDouble("average");
                        System.out.println("average: " + average);
                        return session;
                    }));

    GrpcBidirectionalStreamingServiceBuilder<FindMaximumRequest, FindMaximumResponse> bidirectionalStream =
            grpc("Find Maximum")
                    .bidiStream(CalculatorServiceGrpc.getFindMaximumMethod())
                    .check(
                            statusCode().is(Status.Code.OK),
                            response(FindMaximumResponse::getMaximum).saveAs("maximum"));

    ScenarioBuilder bidirectionalStreaming = scenario("Calculator Bidirectional Streaming")
            .feed(Feeders.channelCredentials())
            .exec(
                    bidirectionalStream.start(),
                    repeat(10).on(bidirectionalStream.send(session -> {
                        int number = ThreadLocalRandom.current().nextInt(0, 1000);
                        return FindMaximumRequest.newBuilder().setNumber(number).build();
                    })),
                    bidirectionalStream.halfClose(),
                    bidirectionalStream.awaitStreamEnd((main, forked) -> {
                        int latestMaximum = forked.getInt("maximum");
                        return main.set("maximum", latestMaximum);
                    }),
                    exec(session -> {
                        int maximum = session.getInt("maximum");
                        System.out.println("maximum: " + maximum);
                        return session;
                    }));

    // spotless:off
    // ./mvnw gatling:test -Dgrpc.scenario=unary
    // ./mvnw gatling:test -Dgrpc.scenario=serverStreaming
    // ./mvnw gatling:test -Dgrpc.scenario=clientStreaming
    // ./mvnw gatling:test -Dgrpc.scenario=bidirectionalStreaming
    // ./mvnw gatling:test -Dgrpc.scenario=deadlines
    // spotless:on

    {
        String name = System.getProperty("grpc.scenario");
        ScenarioBuilder scn;
        if (name == null) {
            scn = unary;
        } else {
            scn = switch (name) {
                case "serverStreaming" -> serverStreaming;
                case "clientStreaming" -> clientStreaming;
                case "bidirectionalStreaming" -> bidirectionalStreaming;
                case "deadlines" -> deadlines;
                default -> unary;};
        }

        setUp(scn.injectOpen(atOnceUsers(1))).protocols(baseGrpcProtocol);
    }
}
