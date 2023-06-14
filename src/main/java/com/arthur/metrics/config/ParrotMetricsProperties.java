package com.arthur.metrics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "parrot.metrics", ignoreInvalidFields = false)
@Data
public class ArthurMetricsProperties {

  private CloudWatchConfig cloudWatchConfig = new CloudWatchConfig();
  private TagsConfig tags = new TagsConfig();

  private int metricsPushFrequencyInSeconds = 30;
  private CollectionGroup collectionGroup;

  /**
   * Optional override for {@code spring.application.name} to change the default "application-name" tag value.
   */
  private String appName;
  /**
   * Optional development environment where the metrics are being collected from. By default it will be for <code>dev</code>.
   */
  private DeploymentEnvironment environment = DeploymentEnvironment.DEVELOPMENT;

  @Data
  public static class CloudWatchConfig {

    /**
     * @see software.amazon.awssdk.regions.Region for valid set of region values
     */
    private String awsRegion = "us-east-1";
  }

  @Data
  public static class TagsConfig {

    private InstanceTypeTagValue instanceType;
  }

  public enum InstanceTypeTagValue {
    DATA_COLLECTOR("data_collector");

    private final String instanceTypeValue;

    InstanceTypeTagValue(String instanceTypeValue) {
      this.instanceTypeValue = instanceTypeValue;
    }

    public String getInstanceTypeValue() {
      return instanceTypeValue;
    }
  }
}
