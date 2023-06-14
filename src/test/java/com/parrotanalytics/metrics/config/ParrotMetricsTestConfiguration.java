package com.arthur.metrics.config;

import java.net.URI;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

@TestConfiguration
@Log4j2
public class ArthurMetricsTestConfiguration {

  @Bean
  public LocalStackContainer localStackContainer() {
    return LocalStackContainerWrapper.getInstance();
  }

  @Bean
  public CloudWatchClient amazonCloudWatchClient(LocalStackContainer localStackContainer) {
    return CloudWatchClient.builder()
        .endpointOverride(getCloudWatchEndpoint(localStackContainer))
        .region(Region.of(localStackContainer.getRegion()))
        .credentialsProvider(getAwsCredentialsProvider(localStackContainer))
        .build();
  }

  @Bean
  @Primary
  public CloudWatchAsyncClient cloudWatchAsyncClient() {
    LocalStackContainer localStackContainer = localStackContainer();
    URI mockCloudWatchUri = getCloudWatchEndpoint(localStackContainer);
    log.info("Connecting to CloudWatch instance at {}", mockCloudWatchUri);
    return CloudWatchAsyncClient
        .builder()
        .endpointOverride(mockCloudWatchUri)
        .region(Region.of(localStackContainer.getRegion()))
        .credentialsProvider(getAwsCredentialsProvider(localStackContainer))
        .build();
  }

  private URI getCloudWatchEndpoint(LocalStackContainer localStackContainer) {
    return localStackContainer.getEndpointOverride(Service.CLOUDWATCH);
  }

  private AwsCredentialsProvider getAwsCredentialsProvider(LocalStackContainer localStackContainer) {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(localStackContainer.getAccessKey(), localStackContainer.getSecretKey())
    );
  }
}
