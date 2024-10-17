package io.gatling.grpc.training;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.FeederBuilder;

import io.grpc.ChannelCredentials;
import io.grpc.TlsChannelCredentials;

public class Feeders {

  private static final List<ChannelCredentials> availableChannelCredentials = new ArrayList<>();

  static {
    try {
      for (int i = 1; i <= 5; i++) {
        ChannelCredentials channelCredentials = TlsChannelCredentials.newBuilder()
          .keyManager(
            ClassLoader.getSystemResourceAsStream("certs/client" + i + ".crt"),
            ClassLoader.getSystemResourceAsStream("certs/client" + i + ".key"))
          .trustManager(ClassLoader.getSystemResourceAsStream("certs/ca.crt"))
          .build();
        availableChannelCredentials.add(channelCredentials);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static FeederBuilder<Object> channelCredentials() {
    List<Map<String, Object>> records = availableChannelCredentials.stream()
      .map(channelCredentials -> Map.<String, Object>of("channelCredentials", channelCredentials))
      .toList();
    return CoreDsl.listFeeder(records);
  }
}
