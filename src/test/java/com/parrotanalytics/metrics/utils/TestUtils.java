package com.arthur.metrics.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;
import java.util.function.Predicate;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;

public class TestUtils {

  public static final String APPLICATION_NAME = "test-app-parrot-metrics";

  private static final int WAIT_PERIOD_IN_MILLIS = 400;
  private static final Random RANDOM = new SecureRandom();

  public static void wait(Predicate<Void> condition, long maximumWaitTimeInMillis) {
    long t0 = System.currentTimeMillis();
    while (!condition.test(null)) {
      try {
        Thread.sleep(WAIT_PERIOD_IN_MILLIS);
      } catch (InterruptedException e) {
      }
      if ((System.currentTimeMillis() - t0) > maximumWaitTimeInMillis) {
        Assert.fail(String.format("Failed to meet the test condition within %d ms", maximumWaitTimeInMillis));
      }
    }
  }

  public static String generateRandomIpAddress() {
    return String.format("%d.%d.%d.%d", RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256));
  }

  public static String getToken(String fileName) throws IOException {
    ClassLoader classLoader = TestUtils.class.getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(fileName);
    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
  }
}
