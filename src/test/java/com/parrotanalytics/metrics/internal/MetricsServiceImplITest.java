package com.arthur.metrics.internal;

import static com.arthur.metrics.utils.TestUtils.APPLICATION_NAME;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arthur.metrics.AbstractITest;
import com.arthur.metrics.config.CollectionGroup;
import com.arthur.metrics.config.DeploymentEnvironment;
import com.arthur.metrics.config.ArthurMetricsProperties.InstanceTypeTagValue;
import com.arthur.metrics.config.ArthurMetricsTestConfiguration;
import com.arthur.metrics.meters.Counter;
import com.arthur.metrics.meters.MetricsTimer;
import com.arthur.metrics.meters.MetricsTimer.TimerInProgress;
import com.arthur.metrics.service.Metrics;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import(ArthurMetricsTestConfiguration.class)
@SpringBootTest(classes = {ArthurMetricsTestConfiguration.class}, properties = {"spring.application.name=" + APPLICATION_NAME})
class MetricsServiceImplITest extends AbstractITest {

  @Autowired
  private Metrics metrics;

  @Test
  void testCountMetric_publishCounters() {
    MetricName metricName = new MetricName();
    Counter counter = metrics.createOrGetCounter(metricName.metricName);
    counter.increment();
    counter.increment();

    waitForMetricsToPublish(metricName.awsCountMetricName, 2, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    MetricResult results = getSumStatsForMetrics(metricName.awsCountMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    validateMetrics(results, 1, 2);
    validateMetricProperties(metricName.awsCountMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, InstanceTypeTagValue.DATA_COLLECTOR);
  }

  @Test
  void testCountMetric_accessingSameCounterByName() {
    MetricName metricName = new MetricName();
    Counter counter = metrics.createOrGetCounter(metricName.metricName);
    counter.increment();
    // access counter by same name and increment again
    counter = metrics.createOrGetCounter(metricName.metricName);
    counter.increment();
    counter.increment();

    waitForMetricsToPublish(metricName.awsCountMetricName, 3, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    MetricResult results = getSumStatsForMetrics(metricName.awsCountMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    validateMetrics(results, 1, 3);
    validateMetricProperties(metricName.awsCountMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, InstanceTypeTagValue.DATA_COLLECTOR);
  }

  @Test
  void testCountMetric_incrementByAmount_singleInvocation() {
    MetricName metricName = new MetricName();
    Counter counter = metrics.createOrGetCounter(metricName.metricName);
    counter.increment(200);

    waitForMetricsToPublish(metricName.awsCountMetricName, 1, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    MetricResult results = getSumStatsForMetrics(metricName.awsCountMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    validateMetrics(results, 1, 200);
    validateMetricProperties(metricName.awsCountMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, InstanceTypeTagValue.DATA_COLLECTOR);
  }

  @Test
  void testCountMetric_incrementByAmount_multipleInvocations() {
    MetricName metricName = new MetricName();
    Counter counter = metrics.createOrGetCounter(metricName.metricName);
    counter.increment(200);
    counter.increment(300);

    waitForMetricsToPublish(metricName.awsCountMetricName, 2, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    MetricResult results = getSumStatsForMetrics(metricName.awsCountMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    validateMetrics(results, 1, 500);
    validateMetricProperties(metricName.awsCountMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, InstanceTypeTagValue.DATA_COLLECTOR);
  }

  @Test
  void testTimerMetric_publishTimeDurations() {
    TimerMetricName metricName = new TimerMetricName();
    MetricsTimer metricsTimer = metrics.createOrGetTimer(metricName.getMetricName());
    metricsTimer.duration(Duration.ofMillis(1000));
    metricsTimer.duration(Duration.ofMillis(2000));
    metricsTimer.duration(Duration.ofSeconds(40));

    waitForMetricsToPublish(metricName.getAwsCountMetricName(), 3, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    validateTimerMetric(metricName, 43000, 3, 40000, CollectionGroup.CATALOG_DATA_COLLECTOR,
        InstanceTypeTagValue.DATA_COLLECTOR, DeploymentEnvironment.DEVELOPMENT);
  }

  @Test
  void testTimerMetric_accessingSameTimerByName() {
    TimerMetricName metricName = new TimerMetricName();
    MetricsTimer metricsTimer = metrics.createOrGetTimer(metricName.getMetricName());
    metricsTimer.duration(Duration.ofMillis(1000));
    metricsTimer.duration(Duration.ofMillis(2000));

    // access counter by same name and increment again
    metricsTimer = metrics.createOrGetTimer(metricName.getMetricName());
    metricsTimer.duration(Duration.ofSeconds(30));
    metricsTimer.duration(Duration.ofMillis(4000));

    waitForMetricsToPublish(metricName.getAwsCountMetricName(), 4, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    validateTimerMetric(metricName, 37000, 4, 30000, CollectionGroup.CATALOG_DATA_COLLECTOR,
        InstanceTypeTagValue.DATA_COLLECTOR, DeploymentEnvironment.DEVELOPMENT);
  }

  @Test
  void testTimerMetric_startAndStopTimer_validateDuration() throws Exception {
    final long minOperationTimeInMillis = 5000;
    TimerMetricName metricName = new TimerMetricName();
    MetricsTimer timer = metrics.createOrGetTimer(metricName.getMetricName());
    TimerInProgress timerInProgress = timer.start();
    Thread.sleep(minOperationTimeInMillis);
    timerInProgress.stop();

    waitForMetricsToPublish(metricName.getAwsCountMetricName(), 1, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    validateTimerMetricDurationAndDimensions(metricName, 1, minOperationTimeInMillis, minOperationTimeInMillis, CollectionGroup.CATALOG_DATA_COLLECTOR,
        InstanceTypeTagValue.DATA_COLLECTOR, null);
  }

  @Test
  void testTimerMetric_startTimerTwice_getException() throws Exception {
    TimerMetricName metricName = new TimerMetricName();
    MetricsTimer metricsTimer = metrics.createOrGetTimer(metricName.getMetricName());
    TimerInProgress timerInProgress = metricsTimer.start();
    try {
      assertThatThrownBy(metricsTimer::start).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Timer sequence has already been started! Please stop it before start a new one.");
    } finally {
      timerInProgress.stop();
    }
  }

  @Test
  void testTimerMetric_stopTimerTwice_getException() throws Exception {
    TimerMetricName metricName = new TimerMetricName();
    MetricsTimer metricsTimer = metrics.createOrGetTimer(metricName.getMetricName());
    TimerInProgress timerInProgress = metricsTimer.start();
    timerInProgress.stop();
    assertThatThrownBy(timerInProgress::stop).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Timer sequence has already been stopped! Please start a new timer sequence.");
  }
}
