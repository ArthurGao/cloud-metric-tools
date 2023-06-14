package com.arthur.metrics;

import static com.arthur.metrics.internal.MetricsServiceImpl.TAG_APPLICATION_NAME;
import static com.arthur.metrics.internal.MetricsServiceImpl.TAG_DEPLOYMENT_ENVIRONMENT;
import static com.arthur.metrics.internal.MetricsServiceImpl.TAG_INSTANCE_TYPE_NAME;
import static com.arthur.metrics.utils.TestUtils.APPLICATION_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arthur.metrics.config.CollectionGroup;
import com.arthur.metrics.config.DeploymentEnvironment;
import com.arthur.metrics.config.ArthurMetricsProperties.InstanceTypeTagValue;
import com.arthur.metrics.meters.MetricsTimer;
import com.arthur.metrics.service.Metrics;
import com.arthur.metrics.utils.TestUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.DimensionFilter;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsRequest;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

@ExtendWith(SpringExtension.class)
@Log4j2
public abstract class AbstractITest {

  private static final int MAX_WAIT_TIME_IN_MILLIS = 10000;
  @Getter
  @Autowired
  private CloudWatchClient cloudWatchClient;

  @Getter
  @Autowired
  private Metrics metrics;

  protected final void waitForMetricsToPublish(String metricName, int minExpectedMetricsCount, CollectionGroup collectionGroup,
      List<Pair<String, String>> tags) {
    TestUtils.wait((Void) -> getSumStatsForMetrics(metricName, collectionGroup, tags).sumOfMetricValues >= minExpectedMetricsCount, MAX_WAIT_TIME_IN_MILLIS);
  }


  protected final void verifyNoMetricsPublished(String metricName, CollectionGroup collectionGroup, Pair<String, String> tags) throws InterruptedException {
    Thread.sleep(MAX_WAIT_TIME_IN_MILLIS / 2 + 1);
    assertEquals(0, getMetricsByName(metricName, collectionGroup, (tags == null) ? null : Collections.singletonList(tags)).size());
  }

  private void addTagToDimension(List<Dimension> dimensions, List<Pair<String, String>> tag) {
    if (CollectionUtils.isNotEmpty(tag)) {
      tag.forEach(t -> dimensions.add(Dimension.builder().name(t.getLeft()).value(t.getRight()).build()));
    }
  }

  protected final MetricResult getSumStatsForMetrics(String metricName, CollectionGroup collectionGroup, List<Pair<String, String>> tags) {
    Dimension dimension = Dimension.builder()
        .name(TAG_APPLICATION_NAME)
        .value(APPLICATION_NAME)
        .build();
    List<Dimension> dimensions = new java.util.ArrayList<>(Collections.singletonList(dimension));
    addTagToDimension(dimensions, tags);
    Instant end = Instant.now();
    Instant start = end.minus(10, ChronoUnit.MINUTES);

    GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
        .metricName(metricName)
        .namespace(collectionGroup.getGroupName())
        .statistics(Statistic.SUM)
        .period(1)
        .startTime(start.truncatedTo(ChronoUnit.MILLIS))
        .endTime(end.truncatedTo(ChronoUnit.MILLIS))
        .dimensions(dimensions.toArray(new Dimension[0]))
        .build();
    cloudWatchClient.getMetricStatistics(request);
    GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
    int totalPublishedCount = 0;
    double sumOfMetricValues = 0;
    for (Datapoint dp : response.datapoints()) {
      if (dp.sum() > 0) {
        totalPublishedCount++;
        sumOfMetricValues += dp.sum();
      }
    }
    log.info("Found {}: {} data points [totalPublishedCount: {}, sumOfMetricValues: {}].", metricName, response.datapoints().size(), totalPublishedCount,
        sumOfMetricValues);
    return MetricResult.builder()
        .totalPublishedCount(totalPublishedCount)
        .sumOfMetricValues(sumOfMetricValues)
        .build();
  }

  private List<DimensionFilter> populateDimensionFilter(List<Pair<String, String>> tags) {
    if (CollectionUtils.isNotEmpty(tags)) {
      return tags.stream()
          .map(tag -> DimensionFilter.builder().name(tag.getLeft()).value(tag.getRight()).build())
          .collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
  }

  protected final List<Metric> getMetricsByName(String metricName, CollectionGroup collectionGroup, List<Pair<String, String>> tags) {
    List<DimensionFilter> dimensionFilters = populateDimensionFilter(tags);
    ListMetricsRequest request = ListMetricsRequest.builder()
        .metricName(metricName)
        .dimensions(dimensionFilters)
        .namespace(collectionGroup.getGroupName()).build();
    return cloudWatchClient.listMetrics(request)
        .metrics()
        .stream()
        .filter(m ->
            StringUtils.equals(m.metricName(), metricName)
        )
        .collect(Collectors.toList());
  }

  protected final void validateTimerMetric(TimerMetricName metricName, int totalCombinedDurationInMillis, int totalEventsCount, int maxDurationValueInMillis,
      CollectionGroup collectionGroup, InstanceTypeTagValue instanceTypeTagValue, DeploymentEnvironment environmentTagValue) {
    // NOTE: With timers, events could be published in chunks due to wait delays in tests. Therefore, we only check that at minimum 1 batch was published
    // validate sum
    MetricResult results = getSumStatsForMetrics(metricName.awsSumMetricName, collectionGroup, null);
    validateMetricsWithMinPublishedCount(results, 1, totalCombinedDurationInMillis);
    // validate average
    results = getSumStatsForMetrics(metricName.awsAverageMetricName, collectionGroup, null);
    validateMetricsWithMinPublishedCount(results, 1, (totalCombinedDurationInMillis * 1.0) / totalEventsCount);
    // validate maximum
    results = getSumStatsForMetrics(metricName.awsMaximumMetricName, collectionGroup, null);
    validateMetricsWithMinPublishedCount(results, 1, maxDurationValueInMillis);

    validateTimerMetricCount(metricName, totalEventsCount, collectionGroup, null);
    validateTimerMetricDimensions(metricName, collectionGroup, instanceTypeTagValue, environmentTagValue, null);
  }

  protected final void validateTimerMetricDuration(TimerMetricName metricName, int totalEventsCount, long minTotalCombinedTimeDurationInMillis,
      long maxDurationLowerBoundValueInMillis, CollectionGroup collectionGroup, InstanceTypeTagValue instanceTypeTagValue, List<Pair<String, String>> tags) {
    // NOTE: With timers, events could be published in chunks due to wait delays in tests. Therefore, we only check that at minimum 1 batch was published
    // validate sum greater than lower bound
    MetricResult results = getSumStatsForMetrics(metricName.awsSumMetricName, collectionGroup, tags);
    assertTrue(results.getTotalPublishedCount() >= 1, String.format("Expected at least 1 published count, but was %d.", results.getTotalPublishedCount()));
    assertTrue(results.getSumOfMetricValues() >= minTotalCombinedTimeDurationInMillis,
        String.format("Expected sum to be >= to %d, but was %f", minTotalCombinedTimeDurationInMillis, results.getSumOfMetricValues()));
    // validate average greater than lower bound
    final double averageDurationLowerBoundInMillis = (minTotalCombinedTimeDurationInMillis * 1.0) / totalEventsCount;
    results = getSumStatsForMetrics(metricName.awsAverageMetricName, collectionGroup, tags);
    assertTrue(results.getTotalPublishedCount() >= 1, String.format("Expected at least 1 published count, but was %d.", results.getTotalPublishedCount()));
    assertTrue(results.getSumOfMetricValues() >= averageDurationLowerBoundInMillis,
        String.format("Expected average to be >= to %f, but was %f", averageDurationLowerBoundInMillis, results.getSumOfMetricValues()));
    // validate max greater than lower bound
    results = getSumStatsForMetrics(metricName.awsMaximumMetricName, collectionGroup, tags);
    assertTrue(results.getTotalPublishedCount() >= 1, String.format("Expected at least 1 published count, but was %d.", results.getTotalPublishedCount()));
    assertTrue(results.getSumOfMetricValues() >= maxDurationLowerBoundValueInMillis,
        String.format("Expected max to be >= to %d, but was %f", maxDurationLowerBoundValueInMillis, results.getSumOfMetricValues()));

    validateTimerMetricCount(metricName, totalEventsCount, collectionGroup, tags);
  }

  protected final void validateTimerMetricDurationAndDimensions(TimerMetricName metricName, int totalEventsCount, long minTotalCombinedTimeDurationInMillis,
      long maxDurationLowerBoundValueInMillis, CollectionGroup collectionGroup, InstanceTypeTagValue instanceTypeTagValue, List<Pair<String, String>> tags) {
    validateTimerMetricDurationAndDimensions(metricName, totalEventsCount, minTotalCombinedTimeDurationInMillis, maxDurationLowerBoundValueInMillis,
        collectionGroup, instanceTypeTagValue, DeploymentEnvironment.DEVELOPMENT, tags);
  }

  protected final void validateTimerMetricDurationAndDimensions(TimerMetricName metricName, int totalEventsCount, long minTotalCombinedTimeDurationInMillis,
      long maxDurationLowerBoundValueInMillis, CollectionGroup collectionGroup, InstanceTypeTagValue instanceTypeTagValue,
      DeploymentEnvironment environmentTagValue, List<Pair<String, String>> tags) {
    validateTimerMetricDuration(metricName, totalEventsCount, minTotalCombinedTimeDurationInMillis, maxDurationLowerBoundValueInMillis, collectionGroup,
        instanceTypeTagValue, tags);
    validateTimerMetricDimensions(metricName, collectionGroup, instanceTypeTagValue, environmentTagValue, tags);
  }

  protected final void validateMetrics(MetricResult results, int expectedTotalPublishedCount, int expectedSumValueOfMetricValues) {
    validateMetrics(results.getTotalPublishedCount(), results.getSumOfMetricValues(), expectedTotalPublishedCount, expectedSumValueOfMetricValues);
  }

  protected final void validateMetrics(int actualTotalPublishedCount, double actualSumOfMetricValues, int expectedTotalPublishedCount,
      int expectedSumValueOfMetricValues) {
    assertEquals(expectedTotalPublishedCount, actualTotalPublishedCount);
    assertEquals(expectedSumValueOfMetricValues, actualSumOfMetricValues, 0.0);
  }

  protected void validateMetricProperties(String awsMetricName, CollectionGroup collectionGroup, InstanceTypeTagValue instanceTypeTagValue) {
    validateMetricProperties(awsMetricName, collectionGroup, instanceTypeTagValue, null, DeploymentEnvironment.DEVELOPMENT);
  }

  private void validateMetricProperties(String awsMetricName, CollectionGroup collectionGroup, InstanceTypeTagValue instanceTypeTagValue,
      List<Pair<String, String>> tags, DeploymentEnvironment environmentTagValue) {
    List<Metric> metrics = getMetricsByName(awsMetricName, collectionGroup, tags);
    assertTrue(metrics.size() >= 1);
    int expectedDimensions = Optional.ofNullable(tags).orElse(Collections.emptyList()).size() + 3;
    for (Metric metric : metrics) {
      assertEquals(collectionGroup.getGroupName(), metric.namespace());
      assertEquals(expectedDimensions, metric.dimensions().size());
      validateMetricDimension(metric.dimensions(), TAG_APPLICATION_NAME, APPLICATION_NAME);
      validateMetricDimension(metric.dimensions(), TAG_DEPLOYMENT_ENVIRONMENT, environmentTagValue.getEnvironment());
      validateMetricDimension(metric.dimensions(), TAG_INSTANCE_TYPE_NAME, instanceTypeTagValue.getInstanceTypeValue());
      if (CollectionUtils.isNotEmpty(tags)) {
        tags.forEach(tag -> {
          validateMetricDimension(metric.dimensions(), tag.getLeft(), tag.getValue());
        });
      }
    }
  }

  private void validateTimerMetricCount(TimerMetricName metricName, int totalEventsCount, CollectionGroup collectionGroup, List<Pair<String, String>> tags) {
    MetricResult results = getSumStatsForMetrics(metricName.getAwsCountMetricName(), collectionGroup, tags);
    validateMetricsWithMinPublishedCount(results, 1, totalEventsCount);
  }

  private void validateMetricsWithMinPublishedCount(MetricResult results, int expectedMinTotalPublishedCount, int expectedSumValueOfMetricValues) {
    assertTrue(results.getTotalPublishedCount() >= expectedMinTotalPublishedCount,
        String.format("Expected at least 1 published count, but was %d.", results.getTotalPublishedCount()));
    assertEquals(expectedSumValueOfMetricValues, results.getSumOfMetricValues(), 0.0);
  }

  private void validateTimerMetricDimensions(TimerMetricName metricName, CollectionGroup collectionGroup, InstanceTypeTagValue instanceTypeTagValue,
      DeploymentEnvironment environmentTagValue, List<Pair<String, String>> tags) {
    validateMetricProperties(metricName.awsSumMetricName, collectionGroup, instanceTypeTagValue, tags, environmentTagValue);
    validateMetricProperties(metricName.awsAverageMetricName, collectionGroup, instanceTypeTagValue, tags, environmentTagValue);
    validateMetricProperties(metricName.awsMaximumMetricName, collectionGroup, instanceTypeTagValue, tags, environmentTagValue);
    validateMetricProperties(metricName.getAwsCountMetricName(), collectionGroup, instanceTypeTagValue, tags, environmentTagValue);
  }

  private void validateMetricsWithMinPublishedCount(MetricResult results, int expectedMinTotalPublishedCount, double expectedSumValueOfMetricValues) {
    assertTrue(results.getTotalPublishedCount() >= expectedMinTotalPublishedCount,
        String.format("Expected at least 1 published count, but was %d.", results.getTotalPublishedCount()));
    assertEquals(expectedSumValueOfMetricValues, results.getSumOfMetricValues(), 0.01);
  }

  protected void validateMetricDimension(List<Dimension> dimensionList, String dimensionName, String dimensionValue) {
    assertThat(dimensionList)
        .filteredOn(dimension -> dimension.name().contains(dimensionName)
            && dimension.value().contains(dimensionValue))
        .hasSize(1)
        .withFailMessage(String.format("Expected to find dimension [%s] and value [%s], but was %s.", dimensionName, dimensionValue, dimensionList));
  }

  @Getter
  @Builder
  public static class MetricResult {

    // Sum of all metric values for the name
    private final double sumOfMetricValues;
    // How many metrics were published
    private final int totalPublishedCount;
  }

  @Getter
  public static class MetricName {

    public final String metricName;
    public final String awsCountMetricName;

    public MetricName() {
      this("TestCounter" + UUID.randomUUID());
    }

    public MetricName(String metricName) {
      this.metricName = metricName;
      this.awsCountMetricName = metricName + ".count";
    }

  }

  /**
   * {@link MetricsTimer} metric produces 4 metric values in AWS:
   *
   * @see MetricsTimer
   */
  public static class TimerMetricName extends MetricName {

    public final String awsSumMetricName;
    public final String awsAverageMetricName;
    public final String awsMaximumMetricName;

    public TimerMetricName() {
      this("TestCounter" + UUID.randomUUID());
    }

    public TimerMetricName(String metricName) {
      super(metricName);
      awsSumMetricName = String.format("%s.sum", metricName);
      awsAverageMetricName = String.format("%s.avg", metricName);
      awsMaximumMetricName = String.format("%s.max", metricName);
    }
  }
}
