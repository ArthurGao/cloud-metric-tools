package com.arthur.metrics.internal.aop;

import static com.arthur.metrics.internal.MetricsServiceImpl.TAG_RESULT;
import static com.arthur.metrics.internal.MetricsServiceImpl.getResultTagValue;

import com.arthur.metrics.annotations.Timer;
import com.arthur.metrics.meters.MetricsTimer.TimerInProgress;
import com.arthur.metrics.service.Metrics;
import io.micrometer.core.instrument.Tags;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
@Log4j2
public class TimerMetricAspect {

  private final Metrics metricsService;

  public TimerMetricAspect(Metrics metricsService) {
    this.metricsService = metricsService;
    log.info("Created TimerMetricAspect");
  }

  @Around("@annotation(timer)")
  public Object interceptAndRecordInvocationTimerMetric(ProceedingJoinPoint joinPoint, Timer timer) throws Throwable {
    final TimerInProgress timerMetric = metricsService.createOrGetTimer(timer.name()).start();
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    boolean stopWhenCompleted = CompletionStage.class.isAssignableFrom(method.getReturnType());
    if (stopWhenCompleted) {
      try {
        return ((CompletionStage) joinPoint.proceed()).whenComplete((resultx, throwable) -> {
          stopTimer(timerMetric, throwable == null);
        });
      } catch (Throwable t) {
        stopTimer(timerMetric, false);
        throw t;
      }
    } else {
      try {
        Object result = joinPoint.proceed();
        stopTimer(timerMetric, true);
        return result;
      } catch (Throwable t) {
        stopTimer(timerMetric, false);
        throw t;
      }
    }
  }

  private void stopTimer(TimerInProgress timer, boolean isSuccess) {
    Tags tags = Tags.of(TAG_RESULT, getResultTagValue(isSuccess));
    timer.stop(tags);
  }
}
