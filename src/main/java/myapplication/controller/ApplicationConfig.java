package myapplication.controller;

import com.khoi.proto.PriceServiceGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

  private String priceServiceEndpoint = "localhost:8085";

  @Bean
  Channel channel() {
    return ManagedChannelBuilder.forTarget(priceServiceEndpoint).usePlaintext().build();
  }

  @Bean
  PriceServiceGrpc.PriceServiceBlockingStub priceServiceBlockingStub(Channel channel) {
    return PriceServiceGrpc.newBlockingStub(channel);
  }
}
