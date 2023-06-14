package com.arthur.metrics.meters;

public interface Counter {

  /**
   * Increment counter by 1
   */
  void increment();

  /**
   * Update the counter by the specified <code>amount</code>. E.g. with batch operations.
   *
   * @param amount - Amount to add to the counter
   */
  void increment(int amount);
}
