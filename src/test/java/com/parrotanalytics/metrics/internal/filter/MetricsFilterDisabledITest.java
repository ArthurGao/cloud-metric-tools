package com.arthur.metrics.internal.filter;

import static com.arthur.metrics.utils.TestUtils.APPLICATION_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arthur.metrics.AbstractITest;
import com.arthur.metrics.config.CollectionGroup;
import com.arthur.metrics.config.ArthurMetricsTestConfiguration;
import com.arthur.metrics.internal.dimensions.RestApiDimensions;
import com.arthur.metrics.utils.TestUtils;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.cloudwatch.model.Metric;

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Import({ArthurMetricsTestConfiguration.class})
@SpringBootTest(classes = {ArthurMetricsTestConfiguration.class}, properties = {"spring.application.name=" + APPLICATION_NAME})
@ActiveProfiles("disablemetrics")
public class MetricsFilterDisabledITest extends AbstractITest {

  @Resource
  private MockMvc mockMvc;

  /**
   * When is-key-based-rest-api-monitoring-enabled: false, MetricFilter is not registered. There is no metric created.
   */
  @Test
  void testAPITimerMetrics_validJwtWithAllFields_successfulMetricWithAllTags() throws Exception {
    String jwtToken = JwtTokenUtility.BEARER_HEADER + TestUtils.getToken("security/jwt_all_attributes_present");
    List<Pair<String, String>> expectedTags = Arrays.asList(
        Pair.of(RestApiDimensions.TAG_REQUEST_URI, "/testRequestNoMetrics"));

    this.mockMvc.perform(get("/testRequestNoMetrics").header(JwtTokenUtility.AUTHORIZATION, jwtToken))
        .andExpect(status().isOk());
    //Here wait for 5 seconds, if metric created, should be done
    Thread.sleep(5000);
    TimerMetricName timerMetricName = new TimerMetricName(MetricsFilter.METRIC_NAME_DURATION);
    List<Metric> results = getMetricsByName(timerMetricName.awsSumMetricName, CollectionGroup.DIRECT_API_SERVICE, expectedTags);
    assertThat(results).isEmpty();
    // validate average
    results = getMetricsByName(timerMetricName.awsAverageMetricName, CollectionGroup.DIRECT_API_SERVICE, expectedTags);
    assertThat(results).isEmpty();
    // validate maximum
    results = getMetricsByName(timerMetricName.awsMaximumMetricName, CollectionGroup.DIRECT_API_SERVICE, expectedTags);
    assertThat(results).isEmpty();
  }
}