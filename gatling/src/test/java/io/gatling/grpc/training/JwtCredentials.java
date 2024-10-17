package io.gatling.grpc.training;

import java.util.Base64;
import java.util.concurrent.Executor;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

public class JwtCredentials extends CallCredentials {

    private static final String SHARED_SECRET_BASE64 = "QTnj/a8PzzYzzI5CLAEkH+cPXubuQPwOojq/YXwxLeA=";
    private static final byte[] SHARED_SECRET = Base64.getDecoder().decode(SHARED_SECRET_BASE64);

    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final String BEARER_TYPE = "Bearer";

    private final String payload;

    public JwtCredentials(String payload) {
        this.payload = payload;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor executor, MetadataApplier metadataApplier) {
        executor.execute(() -> {
            try {
                JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(payload));
                jwsObject.sign(new MACSigner(SHARED_SECRET));

                Metadata headers = new Metadata();
                headers.put(AUTHORIZATION_METADATA_KEY, String.format("%s %s", BEARER_TYPE, jwsObject.serialize()));
                metadataApplier.apply(headers);
            } catch (Throwable e) {
                metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
            }
        });
    }
}
