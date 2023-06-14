package com.arthur.metrics.internal;

import com.arthur.metrics.config.ArthurMetricsProperties;
import com.arthur.metrics.meters.Counter;
import com.arthur.metrics.meters.MetricsTimer;
import com.arthur.metrics.service.Metrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.util.StringUtils;

public class MetricsServiceImpl implements Metrics {

  /*
   * Current set of tags are based off existing AWS CloudWatch metric dimensions to provide backwards compatibility.
   * Future tags could be different.
   */
  public static final String TAG_APPLICATION_NAME = "Application Name";
  public static final String TAG_INSTANCE_TYPE_NAME = "Instance Type";
  public static final String TAG_DEPLOYMENT_ENVIRONMENT = "Environment";
  // can have the following values `successful` or `failed`
  public static final String TAG_RESULT = "Result";
  // values for TAG_RESULT
  public static final String SUCCESS = "Success";
  public static final String FAILURE = "Failure";

  private final MeterRegistry meterRegistry;
  private final String applicationName;
  private final ArthurMetricsProperties parrotMetricsProperties;

  public MetricsServiceImpl(MeterRegistry meterRegistry, String applicationName, ArthurMetricsProperties parrotMetricsProperties) {
    this.meterRegistry = meterRegistry;
    this.parrotMetricsProperties = parrotMetricsProperties;
    this.applicationName = StringUtils.isNotBlank(parrotMetricsProperties.getAppName()) ? parrotMetricsProperties.getAppName() : applicationName;
  }

  @Override
  public Counter createOrGetCounter(String metricName, boolean isOperationSuccessful) {
    Tags tags = createTags()
        .and(TAG_RESULT, getResultTagValue(isOperationSuccessful));
    return CounterImpl.builder()
        .counter(meterRegistry.counter(metricName, tags))
        .build();
  }

  @Override
  public Counter createOrGetCounter(String metricName) {
    Tags tags = createTags();
    return CounterImpl.builder()
        .counter(meterRegistry.counter(metricName, tags))
        .build();
  }

  @Override
  public MetricsTimer createOrGetTimer(String metricName) {
    return MetricsTimerImpl.builder()
        .meterRegistry(meterRegistry)
        .metricName(metricName)
        .basicTags(createTags())
        .build();
  }
  
  private Tags createTags() {
    Tags tags = Tags.of(TAG_APPLICATION_NAME, applicationName)
        .and(TAG_DEPLOYMENT_ENVIRONMENT, parrotMetricsProperties.getEnvironment().getEnvironment());
    if (parrotMetricsProperties.getTags().getInstanceType() != null) {
      tags = tags.and(TAG_INSTANCE_TYPE_NAME, parrotMetricsProperties.getTags().getInstanceType().getInstanceTypeValue());
    }
    return tags;
  }

  // utility methods
  public static String getResultTagValue(boolean isOperationSuccessful) {
    return isOperationSuccessful ? SUCCESS : FAILURE;
  }
}
