package com.arthur.metrics.internal.aop;

import static com.arthur.metrics.internal.MetricsServiceImpl.TAG_APPLICATION_NAME;
import static com.arthur.metrics.internal.MetricsServiceImpl.TAG_DEPLOYMENT_ENVIRONMENT;
import static com.arthur.metrics.internal.MetricsServiceImpl.TAG_INSTANCE_TYPE_NAME;
import static com.arthur.metrics.internal.MetricsServiceImpl.TAG_RESULT;
import static com.arthur.metrics.utils.SampleCounterService.COUNT_NAME_DO_CONDITIONAL_EXCEPTION_WITH_CAPTURE_ALL_OP;
import static com.arthur.metrics.utils.SampleCounterService.COUNT_NAME_DO_CONDITIONAL_EXCEPTION_WITH_FAILURE_OP;
import static com.arthur.metrics.utils.SampleCounterService.COUNT_NAME_DO_EXCEPTION_OP;
import static com.arthur.metrics.utils.SampleCounterService.COUNT_NAME_DO_EXCEPTION_WITH_FAILURE_OP;
import static com.arthur.metrics.utils.SampleCounterService.COUNT_NAME_DO_REPEATED_OP;
import static com.arthur.metrics.utils.SampleCounterService.COUNT_NAME_DO_SINGLE_OP;
import static com.arthur.metrics.utils.TestUtils.APPLICATION_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.arthur.metrics.AbstractITest;
import com.arthur.metrics.config.CollectionGroup;
import com.arthur.metrics.config.DeploymentEnvironment;
import com.arthur.metrics.config.ArthurMetricsProperties.InstanceTypeTagValue;
import com.arthur.metrics.config.ArthurMetricsTestConfiguration;
import com.arthur.metrics.internal.MetricsServiceImpl;
import com.arthur.metrics.utils.SampleCounterService;
import java.util.List;
import javax.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.Metric;

/**
 * @see SampleCounterService for annotations on using {@link com.arthur.metrics.annotations.Count} annotation
 */
@ExtendWith(SpringExtension.class)
@Import({ArthurMetricsTestConfiguration.class})
@SpringBootTest(classes = {ArthurMetricsTestConfiguration.class}, properties = {"spring.application.name=" + APPLICATION_NAME})
@ActiveProfiles("withdevenvironmentexplicitlyset")
class CountMetricAspectITest extends AbstractITest {

  @Resource
  private SampleCounterService subject;

  @Test
  void testCountAspect_successfulOperation() {
    String awsMetricName = COUNT_NAME_DO_SINGLE_OP + ".count";
    subject.doSingleOperation();

    waitForMetricsToPublish(awsMetricName, 1, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    MetricResult results = getSumStatsForMetrics(awsMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    validateMetrics(results, 1, 1);
    validateMetricProperties(awsMetricName, true);
  }

  @Test
  void testCountAspect_successfulOperation_multipleInvocations() {
    String awsMetricName = COUNT_NAME_DO_REPEATED_OP + ".count";
    subject.doRepeatedOperation();
    subject.doRepeatedOperation();
    subject.doRepeatedOperation();

    waitForMetricsToPublish(awsMetricName, 3, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    MetricResult results = getSumStatsForMetrics(awsMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    validateMetrics(results, 1, 3);
    validateMetricProperties(awsMetricName, true);
  }

  @Test
  void testCountAspect_whenExceptionIsThrown_onlyCaptureOriginalMetricByDefault() throws Exception {
    String awsMetricName = COUNT_NAME_DO_EXCEPTION_OP + ".count";
    try {
      subject.doThrowExceptionOperation();
      fail("Pre-condition failed. Expected to throw a RuntimeException");
    } catch (RuntimeException e) {
    }

    waitForMetricsToPublish(awsMetricName, 1, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    verifyNoMetricsPublished(COUNT_NAME_DO_EXCEPTION_OP + ".failed.count", CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    MetricResult results = getSumStatsForMetrics(awsMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    validateMetrics(results, 1, 1);
    validateMetricProperties(awsMetricName, false);
  }

  @Test
  void testCountAspect_whenOnlyCaptureFailuresEnabled_captureFailure() {
    String awsMetricName = COUNT_NAME_DO_EXCEPTION_WITH_FAILURE_OP + ".count";
    try {
      subject.doThrowExceptionOperationWithCaptureOnFailureEnabled();
      fail("Pre-condition failed. Expected to throw a RuntimeException");
    } catch (RuntimeException e) {
    }

    waitForMetricsToPublish(awsMetricName, 1, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    MetricResult results = getSumStatsForMetrics(awsMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    validateMetrics(results, 1, 1);
    validateMetricProperties(awsMetricName, false);
  }

  @Test
  void testCountAspect_whenOnlyCaptureFailuresEnabled_ignoreSuccessAndCaptureOnlyFailureCount() {
    String awsMetricName = COUNT_NAME_DO_CONDITIONAL_EXCEPTION_WITH_FAILURE_OP + ".count";

    subject.doConditionalThrow(false);
    try {
      subject.doConditionalThrow(true);
      fail("Pre-condition failed. Expected to throw a RuntimeException");
    } catch (RuntimeException e) {
    }

    waitForMetricsToPublish(awsMetricName, 1, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    MetricResult results = getSumStatsForMetrics(awsMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    validateMetrics(results, 1, 1);
    validateMetricProperties(awsMetricName, false);
  }

  @Test
  void testCountAspect_whenOnlyCaptureFailuresNotSet_captureBothSuccessAndFailureCounts() {
    String awsMetricName = COUNT_NAME_DO_CONDITIONAL_EXCEPTION_WITH_CAPTURE_ALL_OP + ".count";

    subject.doConditionalThrowCaptureAllEvents(false);
    try {
      subject.doConditionalThrowCaptureAllEvents(true);
      fail("Pre-condition failed. Expected to throw a RuntimeException");
    } catch (RuntimeException e) {
    }

    waitForMetricsToPublish(awsMetricName, 2, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    MetricResult results = getSumStatsForMetrics(awsMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    validateMetrics(results, 1, 2);

    List<Metric> metrics = getMetricsByName(awsMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    assertEquals(2, metrics.size());
    validateMetric(metrics.get(0), true);
    validateMetric(metrics.get(1), false);
  }

  private void validateMetricProperties(String awsMetricName, boolean isSuccess) {
    List<Metric> metrics = getMetricsByName(awsMetricName, CollectionGroup.CATALOG_DATA_COLLECTOR, null);
    assertTrue(metrics.size() >= 1);
    for (Metric metric : metrics) {
      validateMetric(metric, isSuccess);
    }
  }

  private void validateMetric(Metric metric, boolean isSuccessfulOperation) {
    String expectedResultValue = isSuccessfulOperation ? MetricsServiceImpl.SUCCESS : MetricsServiceImpl.FAILURE;
    assertEquals(CollectionGroup.CATALOG_DATA_COLLECTOR.getGroupName(), metric.namespace());
    assertEquals(4, metric.dimensions().size());
    validateMetricDimension(metric.dimensions().get(0), TAG_APPLICATION_NAME, APPLICATION_NAME);
    validateMetricDimension(metric.dimensions().get(1), TAG_DEPLOYMENT_ENVIRONMENT, DeploymentEnvironment.DEVELOPMENT.getEnvironment());
    validateMetricDimension(metric.dimensions().get(2), TAG_INSTANCE_TYPE_NAME, InstanceTypeTagValue.DATA_COLLECTOR.getInstanceTypeValue());
    validateMetricDimension(metric.dimensions().get(3), TAG_RESULT, expectedResultValue);
  }

  private void validateMetricDimension(Dimension dimension, String dimensionName, String dimensionValue) {
    assertEquals(dimensionName, dimension.name());
    assertEquals(dimensionValue, dimension.value());
  }
}
