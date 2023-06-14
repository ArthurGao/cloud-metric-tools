package com.arthur.metrics.internal;

import com.arthur.metrics.meters.MetricsTimer;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import lombok.Builder;

@Builder
public class MetricsTimerImpl implements MetricsTimer {

  private final String metricName;
  private final MeterRegistry meterRegistry;
  private final Tags basicTags;
  private TimerInProgress timerInProgress;

  @Override
  public TimerInProgress start() {
    if (timerInProgress != null) {
      throw new IllegalStateException("Timer sequence has already been started! Please stop it before start a new one.");
    }
    timerInProgress = new TimerInProgressImpl(this);
    return timerInProgress;
  }

  @Override
  public void duration(Duration duration) {
    Timer timer = meterRegistry.timer(metricName, basicTags);
    timer.record(duration);
    timerInProgress = null;
  }

  @Override
  public void duration(Duration duration, Tags tags) {
    Timer timer = meterRegistry.timer(metricName, tags.and(basicTags));
    timer.record(duration);
    timerInProgress = null;
  }

  public static class TimerInProgressImpl implements TimerInProgress {

    private final MetricsTimerImpl metricsTimer;
    private Long startTimeInNanos;

    private TimerInProgressImpl(MetricsTimerImpl metricsTimer) {
      this.startTimeInNanos = Clock.SYSTEM.monotonicTime();
      this.metricsTimer = metricsTimer;
    }

    @Override
    public void stop() {
      if (startTimeInNanos == null) {
        throw new IllegalStateException("Timer sequence has already been stopped! Please start a new timer sequence.");
      }
      long durationInNanos = Clock.SYSTEM.monotonicTime() - startTimeInNanos;
      startTimeInNanos = null;
      metricsTimer.duration(Duration.ofNanos(durationInNanos));
    }

    @Override
    public void stop(Tags tags) {
      if (startTimeInNanos == null) {
        throw new IllegalStateException("Timer sequence has already been stopped! Please start a new timer sequence.");
      }
      long durationInNanos = Clock.SYSTEM.monotonicTime() - startTimeInNanos;
      startTimeInNanos = null;
      metricsTimer.duration(Duration.ofNanos(durationInNanos), tags);
    }
  }
}
