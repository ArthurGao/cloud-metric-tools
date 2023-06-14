package com.arthur.metrics.internal;

import com.arthur.metrics.meters.Counter;
import lombok.Builder;

@Builder
public class CounterImpl implements Counter {

  private final io.micrometer.core.instrument.Counter counter;

  @Override
  public void increment() {
    counter.increment();
  }

  @Override
  public void increment(int amount) {
    counter.increment(amount);
  }
}
