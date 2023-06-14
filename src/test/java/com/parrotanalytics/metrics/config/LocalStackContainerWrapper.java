package com.arthur.metrics.config;

import lombok.extern.log4j.Log4j2;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;

@Log4j2
public class LocalStackContainerWrapper {

  private static final String LOCALSTACK_DOCKER_IMAGE_NAME = "localstack/localstack";
  private static final String LOCALSTACK_DOCKER_VERSION = "1.0.3";
  private static LocalStackContainer localstackContainer;

  public static LocalStackContainer getInstance() {
    if (localstackContainer == null) {
      localstackContainer = new LocalStackContainer(DockerImageName.parse(LOCALSTACK_DOCKER_IMAGE_NAME)
          .withTag(LOCALSTACK_DOCKER_VERSION))
          .withServices(Service.CLOUDWATCH);

      Runtime.getRuntime()
          .addShutdownHook(new Thread(() -> {
            log.info("Stopping LocalStackContainer ...");
            localstackContainer.stop();
            log.info("Stopped LocalStackContainer");
          }));
      localstackContainer.start();
      log.info("LocalStackContainer started.");
    }
    return localstackContainer;
  }
}
