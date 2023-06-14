package com.arthur.metrics.config;

import lombok.Getter;

/**
 * High level grouping for metrics. This will be used as the <b>namespace</b> for CloudWatch.
 *
 * @see https://us-east-1.console.aws.amazon.com/cloudwatch/home?region=us-east-1#metricsV2:graph=~() for a full list of namespaces.
 */
public enum CollectionGroup {
  /**
   * Content supply data collector applications
   */
  CATALOG_DATA_COLLECTOR("Data Sources"),
  DIRECT_API_SERVICE("Direct API Service");

  @Getter
  private final String groupName;

  CollectionGroup(String groupName) {
    this.groupName = groupName;
  }
}
