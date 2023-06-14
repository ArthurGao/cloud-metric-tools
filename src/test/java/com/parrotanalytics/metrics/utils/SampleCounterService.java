package com.arthur.metrics.utils;

import com.arthur.metrics.annotations.Count;
import org.springframework.stereotype.Component;

@Component
public class SampleCounterService {

  public static final String COUNT_NAME_DO_SINGLE_OP = "SampleCounterPojo.doSingleOperationCountMetric";
  public static final String COUNT_NAME_DO_REPEATED_OP = "SampleCounterPojo.doRepeatedOperationCountMetric";
  public static final String COUNT_NAME_DO_EXCEPTION_OP = "SampleCounterPojo.doThrowExceptionOperationCountMetric";
  public static final String COUNT_NAME_DO_EXCEPTION_WITH_FAILURE_OP = "SampleCounterPojo.doThrowExceptionOperationWithCaptureOnFailureEnabledCountMetric";
  public static final String COUNT_NAME_DO_CONDITIONAL_EXCEPTION_WITH_FAILURE_OP = "SampleCounterPojo.doConditionalThrowCountMetric";
  public static final String COUNT_NAME_DO_CONDITIONAL_EXCEPTION_WITH_CAPTURE_ALL_OP = "SampleCounterPojo.doConditionalThrowCaptureAllEventsMetric";

  @Count(name = COUNT_NAME_DO_SINGLE_OP)
  public void doSingleOperation() {
    // some operation
  }

  @Count(name = COUNT_NAME_DO_REPEATED_OP)
  public void doRepeatedOperation() {
    // some operation
  }

  @Count(name = COUNT_NAME_DO_EXCEPTION_OP)
  public void doThrowExceptionOperation() throws RuntimeException {
    throw new RuntimeException("Throws exception");
  }

  @Count(name = COUNT_NAME_DO_EXCEPTION_WITH_FAILURE_OP, captureOnFailureOnly = true)
  public void doThrowExceptionOperationWithCaptureOnFailureEnabled() throws RuntimeException {
    throw new RuntimeException("Throws exception");
  }

  @Count(name = COUNT_NAME_DO_CONDITIONAL_EXCEPTION_WITH_FAILURE_OP, captureOnFailureOnly = true)
  public void doConditionalThrow(boolean isSimulateException) {
    if (isSimulateException) {
      throw new RuntimeException("Throws exception");
    }
  }

  @Count(name = COUNT_NAME_DO_CONDITIONAL_EXCEPTION_WITH_CAPTURE_ALL_OP)
  public void doConditionalThrowCaptureAllEvents(boolean isSimulateException) {
    if (isSimulateException) {
      throw new RuntimeException("Throws exception");
    }
  }
}
