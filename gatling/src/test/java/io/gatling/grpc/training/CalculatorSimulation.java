package io.gatling.grpc.training;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import io.gatling.grpc.training.calculator.*;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.grpc.GrpcBidirectionalStreamingServiceBuilder;
import io.gatling.javaapi.grpc.GrpcProtocolBuilder;
import io.gatling.javaapi.grpc.GrpcServerStreamingServiceBuilder;

import io.grpc.Metadata;
import io.grpc.Status;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.grpc.GrpcDsl.*;

public class CalculatorSimulation extends Simulation {

    GrpcProtocolBuilder baseGrpcProtocol = grpc
        .forAddress("localhost", 50052)
        ;

    ScenarioBuilder unary = scenario("Calculator Unary")
        .exec(
            grpc("Sum")
                .unary(CalculatorServiceGrpc.getSumMethod())
                .send(
                    SumRequest.newBuilder()
                        .setFirstNumber(1)
                        .setSecondNumber(2)
                        .build()
                )
                .deadlineAfter(Duration.ofMillis(100))
                .header(Metadata.Key.of("example-header", Metadata.ASCII_STRING_MARSHALLER))
                .value("example header value")
                .check(
                    statusCode().is(Status.Code.OK),
                    response(SumResponse::getSumResult).is(3)
                )
        );

    ScenarioBuilder deadlines = scenario("Calculator w/ Deadlines")
        .exec(
            grpc("SquareRoot")
                .unary(CalculatorServiceGrpc.getSquareRootMethod())
                .send(
                    SquareRootRequest.newBuilder()
                        .setNumber(-1)
                        .build()
                )
                .check(
                    statusCode().is(Status.Code.INVALID_ARGUMENT)
                )
        );

    GrpcServerStreamingServiceBuilder<PrimeNumberDecompositionRequest, PrimeNumberDecompositionResponse> serverStream =
        grpc("Prime Number Decomposition")
            .serverStream(CalculatorServiceGrpc.getPrimeNumberDecompositionMethod())
            .messageResponseTimePolicy(MessageResponseTimePolicy.FromLastMessageReceived)
            .check(
                statusCode().is(Status.Code.OK),
                response(PrimeNumberDecompositionResponse::getPrimeFactor)
                    .transform(p -> p == 2L || p == 5L || p == 17L || p == 97L || p == 6669961L)
                    .is(true)
                //asciiTrailer("example-trailer").is("wrong value"),
                //asciiHeader("example-header").is("wrong value")
            );

    // Lifecycle: send + awaitStreamEnd
    ScenarioBuilder serverStreaming = scenario("Calculator Server Streaming")
        .exec(
            serverStream.send(
                 PrimeNumberDecompositionRequest.newBuilder()
                     .setNumber(109987656890L)
                     .build()),
            serverStream.awaitStreamEnd()
        );

    ScenarioBuilder clientStreaming = scenario("Calculator Client Streaming")
            // TODO
            ;

    GrpcBidirectionalStreamingServiceBuilder<FindMaximumRequest, FindMaximumResponse> bidirectionalStream =
        grpc("Find Maximum")
            .bidiStream(CalculatorServiceGrpc.getFindMaximumMethod())
            ;

    ScenarioBuilder bidirectionalStreaming = scenario("Calculator Bidirectional Streaming")
        .feed(csv("numbers.csv"))
        .exec(
            bidirectionalStream.start(),
            repeat(10).on(
                bidirectionalStream.send(session -> {
                    int number = ThreadLocalRandom.current().nextInt(0, 1000);
                    return FindMaximumRequest.newBuilder()
                        .setNumber(number)
                        .build();
                })
            ),
            bidirectionalStream.halfClose()
        );

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
