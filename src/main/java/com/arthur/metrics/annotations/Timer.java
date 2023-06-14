package com.arthur.metrics.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Timer metrics are captured at all invocations (regardless whether the method succeeds or not).
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timer {

  /**
   * Name of the metric
   */
  String name();
}
