package com.arthur.metrics.config;

import lombok.Getter;

public enum DeploymentEnvironment {

  /**
   * Content supply data collector applications
   */
  PRODUCTION("prod"),
  DEVELOPMENT("dev"),
  LOCAL("local");

  @Getter
  private final String environment;

  DeploymentEnvironment(String environment) {
    this.environment = environment;
  }
}
