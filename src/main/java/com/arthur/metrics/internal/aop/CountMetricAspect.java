package com.arthur.metrics.internal.aop;

import com.arthur.metrics.annotations.Count;
import com.arthur.metrics.service.Metrics;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
@Log4j2
public class CountMetricAspect {

  private final Metrics metricsService;

  public CountMetricAspect(Metrics metricsService) {
    this.metricsService = metricsService;
    log.info("Created CountMetricAspect");
  }

  @Around("@annotation(count)")
  public Object interceptAndRecordInvocationCountMetric(ProceedingJoinPoint joinPoint, Count count) throws Throwable {
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    boolean stopWhenCompleted = CompletionStage.class.isAssignableFrom(method.getReturnType()); // E.g. Futures
    if (stopWhenCompleted) {
      try {
        return ((CompletionStage) joinPoint.proceed()).whenComplete((resultx, throwable) -> {
          if (throwable != null) {
            recordCount(count, throwable == null);
          }
        });
      } catch (Throwable t) {
        recordCount(count, false);
        throw t;
      }
    } else {
      try {
        Object result = joinPoint.proceed();
        recordCount(count, true);
        return result;
      } catch (Throwable t) {
        recordCount(count, false);
        throw t;
      }
    }
  }

  private void recordCount(Count count, boolean isSuccessful) {
    if (isCaptureRecord(count, isSuccessful)) {
      metricsService.createOrGetCounter(count.name(), isSuccessful).increment();
    }
  }

  private boolean isCaptureRecord(Count count, boolean isSuccessful) {
    return isCaptureOnlyOnFailureAndRequestFailed(count, isSuccessful)
        || canCaptureAnyRequest(count);
  }
  private boolean canCaptureAnyRequest(Count count) {
    return !count.captureOnFailureOnly();
  }
  private boolean isCaptureOnlyOnFailureAndRequestFailed(Count count, boolean isSuccessful) {
    return count.captureOnFailureOnly() && !isSuccessful;
  }
}
