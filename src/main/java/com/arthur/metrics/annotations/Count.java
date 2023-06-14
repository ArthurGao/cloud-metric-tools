package com.arthur.metrics.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Count metrics are captured at all invocations (regardless whether the method succeeds or not). The {@link #captureOnFailure()} flag can be set to raise a
 * {@link Count} metric with <code>.failed</code> as a suffix (for the name).
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Count {

  /**
   * Name of the metric
   */
  String name();

  /**
   * When <code>true</code>, only the method invocations that throw an exception will be captured (with {@link #failureNameSuffix()} suffix to the
   * {@link #name()})
   */
  boolean captureOnFailureOnly() default false;
}
