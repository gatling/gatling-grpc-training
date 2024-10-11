package io.gatling.grpc.training.server.calculator;

import java.util.Map;

import io.grpc.*;

public class CalculatorServerInterceptor implements ServerInterceptor {

    private final Map<String, String> customHeaders;
    private final Map<String, String> customTrailers;

    public CalculatorServerInterceptor() {
        this.customHeaders = Map.of("example-header", "example value");
        this.customTrailers = Map.of("example-trailer", "example value");
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return next.startCall(
                new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                    @Override
                    public void sendHeaders(Metadata headers) {
                        customHeaders.forEach((key, value) ->
                                headers.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value));
                        super.sendHeaders(headers);
                    }

                    @Override
                    public void close(Status status, Metadata trailers) {
                        customTrailers.forEach((key, value) ->
                                trailers.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value));
                        super.close(status, trailers);
                    }
                },
                headers);
    }
}
