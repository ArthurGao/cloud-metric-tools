package com.arthur.metrics.internal.aop;

import static com.arthur.metrics.internal.MetricsServiceImpl.FAILURE;
import static com.arthur.metrics.internal.MetricsServiceImpl.SUCCESS;
import static com.arthur.metrics.internal.MetricsServiceImpl.TAG_RESULT;
import static com.arthur.metrics.utils.SimpleTimedService.TIMER_NAME_CONDITIONAL_EXCEPTION_OP;
import static com.arthur.metrics.utils.SimpleTimedService.TIMER_NAME_EXCEPTION_OP;
import static com.arthur.metrics.utils.SimpleTimedService.TIMER_NAME_REPEAT_OP;
import static com.arthur.metrics.utils.SimpleTimedService.TIMER_NAME_SIMPLE_OP;
import static com.arthur.metrics.utils.TestUtils.APPLICATION_NAME;
import static org.junit.jupiter.api.Assertions.fail;

import com.arthur.metrics.AbstractITest;
import com.arthur.metrics.config.CollectionGroup;
import com.arthur.metrics.config.ArthurMetricsProperties.InstanceTypeTagValue;
import com.arthur.metrics.config.ArthurMetricsTestConfiguration;
import com.arthur.metrics.utils.SimpleTimedService;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @see com.arthur.metrics.utils.SimpleTimedService for annotations on using {@link com.arthur.metrics.annotations.Timer} annotation
 */
@ExtendWith(SpringExtension.class)
@Import({ArthurMetricsTestConfiguration.class})
@SpringBootTest(classes = {ArthurMetricsTestConfiguration.class}, properties = {"spring.application.name=" + APPLICATION_NAME})
class TimerMetricAspectITest extends AbstractITest {

  private static final int DEFAULT_OP_DURATION_IN_MILLIS = 500;

  @Resource
  private SimpleTimedService subject;

  @Test
  void testTimerAspect_successfulOperation() throws Exception {
    TimerMetricName metricName = new TimerMetricName(TIMER_NAME_SIMPLE_OP);

    subject.doSingleOperation(DEFAULT_OP_DURATION_IN_MILLIS);
    waitForMetricsToPublish(metricName.getAwsCountMetricName(), 1, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    validateTimerMetricDurationAndDimensions(metricName, 1, DEFAULT_OP_DURATION_IN_MILLIS, DEFAULT_OP_DURATION_IN_MILLIS,
        CollectionGroup.CATALOG_DATA_COLLECTOR,
        InstanceTypeTagValue.DATA_COLLECTOR, createExpectedTags(true));
  }

  @Test
  void testTimerAspect_successfulOperation_multipleInvocations() throws Exception {
    TimerMetricName metricName = new TimerMetricName(TIMER_NAME_REPEAT_OP);

    subject.doMultipleOperations(1500);
    subject.doMultipleOperations(1000);
    subject.doMultipleOperations(500);
    waitForMetricsToPublish(metricName.getAwsCountMetricName(), 3, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    validateTimerMetricDurationAndDimensions(metricName, 3, 3000, 1500, CollectionGroup.CATALOG_DATA_COLLECTOR, InstanceTypeTagValue.DATA_COLLECTOR,
        createExpectedTags(true));
  }

  @Test
  void testTimerAspect_whenExceptionIsThrown() throws Exception {
    TimerMetricName metricName = new TimerMetricName(TIMER_NAME_EXCEPTION_OP);

    try {
      subject.raiseExceptionAfter(DEFAULT_OP_DURATION_IN_MILLIS);
      fail("Pre-condition failed. Expected to throw a RuntimeException");
    } catch (RuntimeException e) {
    }
    waitForMetricsToPublish(metricName.getAwsCountMetricName(), 1, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    validateTimerMetricDurationAndDimensions(metricName, 1, DEFAULT_OP_DURATION_IN_MILLIS, DEFAULT_OP_DURATION_IN_MILLIS,
        CollectionGroup.CATALOG_DATA_COLLECTOR,
        InstanceTypeTagValue.DATA_COLLECTOR, createExpectedTags(false));
  }

  @Test
  void testCountAspect_whenExceptionIsThrowsConditionally_captureTimerMetrics() throws Exception {
    TimerMetricName metricName = new TimerMetricName(TIMER_NAME_CONDITIONAL_EXCEPTION_OP);

    subject.doConditionalThrow(500, false);
    try {
      subject.doConditionalThrow(800, true);
      fail("Pre-condition failed. Expected to throw a RuntimeException");
    } catch (RuntimeException e) {
    }

    waitForMetricsToPublish(metricName.getAwsCountMetricName(), 2, CollectionGroup.CATALOG_DATA_COLLECTOR, null);

    // validate overall duration stats for both events
    validateTimerMetricDuration(metricName, 2, 1300, 800, CollectionGroup.CATALOG_DATA_COLLECTOR, InstanceTypeTagValue.DATA_COLLECTOR, null);
    // validate individual events with their dimensions
    validateTimerMetricDurationAndDimensions(metricName, 1, 500, 500, CollectionGroup.CATALOG_DATA_COLLECTOR, InstanceTypeTagValue.DATA_COLLECTOR,
        createExpectedTags(true));
    validateTimerMetricDurationAndDimensions(metricName, 1, 800, 800, CollectionGroup.CATALOG_DATA_COLLECTOR, InstanceTypeTagValue.DATA_COLLECTOR,
        createExpectedTags(false));

  }

  private List<Pair<String, String>> createExpectedTags(boolean isSuccessful) {
    String tagValue = isSuccessful ? SUCCESS : FAILURE;
    return Collections.singletonList(new ImmutablePair<>(TAG_RESULT, tagValue));
  }
}