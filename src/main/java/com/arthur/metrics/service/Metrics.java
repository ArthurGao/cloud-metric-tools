package com.arthur.metrics.service;

import com.arthur.metrics.meters.Counter;
import com.arthur.metrics.meters.MetricsTimer;

public interface Metrics {

  /**
   * Idempotent method that either create or gets a (reference to an existing) {@link Counter} for the given name. Adds the following tags/dimensions to the *
   * metric:
   * <ul>
   *    <li><code>Application Name</code></li>
   *    <li><code>Instance Type</code></li>
   * </ul>
   *
   * @param metricName
   * @return the {@link Counter} for the given name.
   */
  Counter createOrGetCounter(String metricName);

  /**
   * Idempotent method that either create or gets a (reference to an existing) {@link Counter} for the given name. This operation also adds <code>Result</code>
   * tag to the metric (E.g. dimension) based on whether the operation was a success or not.
   * <p>
   * Adds the following tags/dimensions to the metric:
   * <ul>
   *      <li><code>Application Name</code></li>
   *       <li><code>Instance Type</code></li>
   *       <li><code>Result</code></li>
   *     </ul>
   *
   * @param metricName
   * @param isOperationSuccessful <code>true</code> if the operation was a success, otherwise <code>false</code>
   * @return the {@link Counter} for the given name.
   */
  Counter createOrGetCounter(String metricName, boolean isOperationSuccessful);

  /**
   * Idempotent method that either create or gets a (reference to an existing) {@link MetricsTimer} for the given name.
   *
   * @param metricName
   * @return the {@link MetricsTimer} for the given name.
   */
  MetricsTimer createOrGetTimer(String metricName);
}
