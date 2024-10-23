package io.gatling.grpc.training;

import io.gatling.grpc.training.calculator.*;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.grpc.GrpcProtocolBuilder;

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
        );

    ScenarioBuilder deadlines = scenario("Calculator w/ Deadlines")
            // TODO
            ;

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
