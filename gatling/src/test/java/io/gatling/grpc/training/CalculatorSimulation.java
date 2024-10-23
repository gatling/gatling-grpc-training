package io.gatling.grpc.training;

import io.gatling.grpc.training.calculator.*;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.grpc.GrpcProtocolBuilder;

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

    ScenarioBuilder serverStreaming = scenario("Calculator Server Streaming")
            // TODO
            ;

    ScenarioBuilder clientStreaming = scenario("Calculator Client Streaming")
            // TODO
            ;

    ScenarioBuilder bidirectionalStreaming = scenario("Calculator Bidirectional Streaming")
            // TODO
            ;

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
