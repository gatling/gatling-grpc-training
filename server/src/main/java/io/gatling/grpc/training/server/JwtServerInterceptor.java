package io.gatling.grpc.training.server;

import java.text.ParseException;
import java.util.Base64;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import io.grpc.*;

public class JwtServerInterceptor implements ServerInterceptor {

    private static final String SHARED_SECRET_BASE64 = "QTnj/a8PzzYzzI5CLAEkH+cPXubuQPwOojq/YXwxLeA=";
    private static final byte[] SHARED_SECRET = Base64.getDecoder().decode(SHARED_SECRET_BASE64);

    private static final String BEARER_TYPE = "Bearer";

    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");

    private static final JWSVerifier verifier;
    static {
        try {
            verifier = new MACVerifier(SHARED_SECRET);
        } catch (JOSEException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        String value = metadata.get(AUTHORIZATION_METADATA_KEY);

        Status status;
        if (value == null) {
            status = Status.UNAUTHENTICATED.withDescription("Authorization token is missing");
        } else if (!value.startsWith(BEARER_TYPE)) {
            status = Status.UNAUTHENTICATED.withDescription("Unknown authorization type");
        } else {
            // remove authorization type prefix
            String token = value.substring(BEARER_TYPE.length()).trim();
            try {
                // verify token signature and parse claims
                JWSObject jwsObject = JWSObject.parse(token);
                if (jwsObject.verify(verifier)) {
                    // set client id into current context
                    Context ctx = Context.current()
                      .withValue(CLIENT_ID_CONTEXT_KEY, jwsObject.getPayload().toString());
                    return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
                } else {
                    status = Status.UNAUTHENTICATED.withDescription("Invalid signature");
                }
            } catch (JOSEException | ParseException e ) {
                status = Status.UNAUTHENTICATED.withDescription(e.getMessage()).withCause(e);
            }
        }

        serverCall.close(status, new Metadata());
        return new ServerCall.Listener<>() {
            // noop
        };
    }
}
