package com.arthur.metrics.utils;

import com.arthur.metrics.annotations.Timer;
import org.springframework.stereotype.Component;

@Component
public class SimpleTimedService {

  public static final String TIMER_NAME_SIMPLE_OP = "SimpleTimedService.doSingleOperationTimerMetric";
  public static final String TIMER_NAME_REPEAT_OP = "SimpleTimedService.doMultipleOperationsTimerMetric";
  public static final String TIMER_NAME_EXCEPTION_OP = "SimpleTimedService.raiseExceptionAfterTimerMetric";
  public static final String TIMER_NAME_CONDITIONAL_EXCEPTION_OP = "SimpleTimedService.doConditionalThrowTimerMetric";

  @Timer(name = TIMER_NAME_SIMPLE_OP)
  public void doSingleOperation(long waitTimeInMillis) throws InterruptedException {
    Thread.sleep(waitTimeInMillis);
  }

  @Timer(name = TIMER_NAME_REPEAT_OP)
  public void doMultipleOperations(long waitTimeInMillis) throws InterruptedException {
    Thread.sleep(waitTimeInMillis);
  }

  @Timer(name = TIMER_NAME_EXCEPTION_OP)
  public void raiseExceptionAfter(long waitTimeInMillis) throws InterruptedException {
    Thread.sleep(waitTimeInMillis);
    throw new RuntimeException("Simulated Exception");
  }

  @Timer(name = TIMER_NAME_CONDITIONAL_EXCEPTION_OP)
  public void doConditionalThrow(long waitTimeInMillis, boolean isSimulateException) throws InterruptedException {
    Thread.sleep(waitTimeInMillis);
    if (isSimulateException) {
      throw new RuntimeException("Simulated exception");
    }
  }
}
