package com.arthur.metrics.config;

import com.arthur.metrics.internal.MetricsServiceImpl;
import com.arthur.metrics.internal.aop.CountMetricAspect;
import com.arthur.metrics.internal.aop.TimerMetricAspect;
import com.arthur.metrics.service.Metrics;
import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "com.arthur.metrics.internal.aop")
public class ArthurMetricsConfiguration {

  @Value("${spring.application.name}")
  private String applicationName;

  @Autowired
  private ArthurMetricsProperties parrotMetricsProperties;

  @Bean
  public CloudWatchAsyncClient cloudWatchAsyncClient() {
    return CloudWatchAsyncClient
        .builder()
        .region(Region.of(parrotMetricsProperties.getCloudWatchConfig().getAwsRegion()))
        .build();
  }

  @Bean
  public Metrics metrics(CloudWatchAsyncClient cloudWatchAsyncClient) {
    return new MetricsServiceImpl(createMeterRegistry(cloudWatchAsyncClient), applicationName, parrotMetricsProperties);
  }

  @Bean
  public CountMetricAspect countMetricAspect(Metrics metricsService) {
    return new CountMetricAspect(metricsService);
  }

  @Bean
  public TimerMetricAspect timerMetricAspect(Metrics metricsService) {
    return new TimerMetricAspect(metricsService);
  }

  private MeterRegistry createMeterRegistry(CloudWatchAsyncClient cloudWatchAsyncClient) {
    CloudWatchConfig cloudWatchConfig = setupCloudWatchConfig();
    return new CloudWatchMeterRegistry(cloudWatchConfig, Clock.SYSTEM, cloudWatchAsyncClient) {
    };
  }

  private CloudWatchConfig setupCloudWatchConfig() {
    return new CloudWatchConfig() {
      private Map<String, String> configuration = Map.of(
          "cloudwatch.namespace", parrotMetricsProperties.getCollectionGroup().getGroupName(),
          "cloudwatch.step", Duration.ofSeconds(parrotMetricsProperties.getMetricsPushFrequencyInSeconds()).toString());

      @Override
      public String get(String key) {
        return configuration.get(key);
      }
    };
  }
}
