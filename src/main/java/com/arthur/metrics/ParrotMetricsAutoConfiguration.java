package com.arthur.metrics;

import com.arthur.metrics.config.ArthurMetricsConfiguration;
import com.arthur.metrics.config.ArthurMetricsProperties;
import com.arthur.metrics.config.RestApiMonitoringFilterConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties({ArthurMetricsProperties.class})
@Import({ArthurMetricsConfiguration.class, RestApiMonitoringFilterConfiguration.class})
@AutoConfigureBefore({WebMvcAutoConfiguration.class})
public class ArthurMetricsAutoConfiguration {

}
