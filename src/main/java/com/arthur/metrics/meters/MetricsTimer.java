package com.arthur.metrics.meters;

import io.micrometer.core.instrument.Tags;
import java.time.Duration;

/**
 * {@link MetricsTimer} metric produces 4 metric values in AWS:
 * <ul>
 *   <li><code>.avg</code> - The average value for the duration (in millis) recorded</li>
 *   <li><code>.count</code> - Number of times the metric was invoked</li>
 *   <li><code>.max</code> - Maximum value of the duration value recorded</li>
 *   <li><code>.sum</code> - Total value of the duration (in millis) recorded</li>
 * </ul>
 */
public interface MetricsTimer {


  /**
   * Update the stats kept by the metric by the specified duration amount.
   *
   * @param duration {@link Duration} Duration of a single event being measured by this timer.
   */
  void duration(Duration duration);

  /**
   * Update the stats kept by the metric by the specified duration amount and tags.
   *
   * @param tags {@link Tags} tags of a single event being measured by this timer.
   */
  void duration(Duration duration, Tags tags);

  TimerInProgress start();

  interface TimerInProgress {

    /**
     * Stop taking timer measurement
     */
    void stop();

    /**
     * Stop taking timer measurement with custom tags
     *
     * @param tags {@link Tags}
     */
    void stop(Tags tags);
  }
}
