package com.arthur.metrics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@Log4j2
@SpringBootApplication
public class ArthurTestApplication {

  public static void main(String[] args) throws UnknownHostException {
    ConfigurableApplicationContext context = SpringApplication.run(ArthurTestApplication.class);
    Environment env = context.getEnvironment();
    BuildProperties buildProperties = context.getBean(BuildProperties.class);
    String protocol = "http";
    log.info("\n----------------------------------------------------------\n\t" +
            "Application '{}' version {} is running! Access URLs:\n\t" +
            "Local: \t\t{}://localhost:{}\n\t" +
            "External: \t{}://{}:{}\n\t" +
            "Profile(s): \t{}\n----------------------------------------------------------",
        env.getProperty("spring.application.name"),
        buildProperties.getVersion(),
        protocol,
        InetAddress.getLocalHost().getHostAddress(),
        env.getActiveProfiles());

  }
}
