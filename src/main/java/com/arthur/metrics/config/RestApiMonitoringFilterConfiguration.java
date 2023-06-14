package com.arthur.metrics.config;

import com.arthur.metrics.internal.filter.MetricsFilter;
import com.arthur.metrics.service.Metrics;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ComponentScan(basePackages = {"com.arthur.metrics.internal.filter"})
@ConfigurationProperties(prefix = "parrot.metrics")
@ConditionalOnProperty(prefix = "parrot.metrics", name = "is-key-based-rest-api-monitoring-enabled", havingValue = "true")
@Log4j2
public class RestApiMonitoringFilterConfiguration {

  @Bean
  public FilterRegistrationBean<MetricsFilter> registerMetricFilter(Metrics metricsService) {
    FilterRegistrationBean<MetricsFilter> registrationBean
        = new FilterRegistrationBean<>();
    registrationBean.setFilter(new MetricsFilter(metricsService));
    registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    log.info("Successfully registered the filter to monitor APIs.");
    return registrationBean;
  }
}
