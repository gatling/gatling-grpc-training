package io.gatling.grpc.training.server;

import io.grpc.*;
import io.jsonwebtoken.*;

public class JwtServerInterceptor implements ServerInterceptor {

    private static final String JWT_SIGNING_KEY = "QTnj/a8PzzYzzI5CLAEkH+cPXubuQPwOojq/YXwxLeA=";
    private static final String BEARER_TYPE = "Bearer";

    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");

    private JwtParser parser = Jwts.parser().setSigningKey(JWT_SIGNING_KEY);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        String value = metadata.get(AUTHORIZATION_METADATA_KEY);

        Status status = Status.OK;
        if (value == null) {
            status = Status.UNAUTHENTICATED.withDescription("Authorization token is missing");
        } else if (!value.startsWith(BEARER_TYPE)) {
            status = Status.UNAUTHENTICATED.withDescription("Unknown authorization type");
        } else {
            Jws<Claims> claims = null;
            // remove authorization type prefix
            String token = value.substring(BEARER_TYPE.length()).trim();
            try {
                // verify token signature and parse claims
                claims = parser.parseClaimsJws(token);
            } catch (JwtException e) {
                status = Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
            }
            if (claims != null) {
                // set client id into current context
                Context ctx = Context.current()
                        .withValue(CLIENT_ID_CONTEXT_KEY, claims.getBody().getSubject());
                return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
            }
        }

        serverCall.close(status, new Metadata());
        return new ServerCall.Listener<>() {
            // noop
        };
    }
}
