package com.arthur.metrics.internal.filter;

import static com.arthur.metrics.utils.TestUtils.APPLICATION_NAME;
import static com.arthur.metrics.utils.TestUtils.generateRandomIpAddress;
import static com.arthur.metrics.utils.controllers.TestController.DEFAULT_OP_DURATION_IN_MILLIS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arthur.metrics.AbstractITest;
import com.arthur.metrics.config.CollectionGroup;
import com.arthur.metrics.config.DeploymentEnvironment;
import com.arthur.metrics.config.ArthurMetricsProperties.InstanceTypeTagValue;
import com.arthur.metrics.config.ArthurMetricsTestConfiguration;
import com.arthur.metrics.internal.MetricsServiceImpl;
import com.arthur.metrics.internal.dimensions.RestApiDimensions;
import com.arthur.metrics.utils.TestUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.NestedServletException;

/**
 * NOTE: Each test case has a different country and IP address to ensure each test case would produce a unique metric.
 */
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Import({ArthurMetricsTestConfiguration.class})
@SpringBootTest(classes = {ArthurMetricsTestConfiguration.class}, properties = {"spring.application.name=" + APPLICATION_NAME})
@ActiveProfiles("enablemetrics")
public class MetricsFilterEnabledITest extends AbstractITest {

  private static final String PATH_REQUEST = "/testRequest";
  private static final String DEFAULT_ACCOUNT_NAME = "Arthur TestAccount";
  private static final TimerMetricName TIMER_METRIC_NAME = new TimerMetricName(MetricsFilter.METRIC_NAME_DURATION);
  @Resource
  private MockMvc mockMvc;

  @Test
  void metricsTimer_success200HttpGetRequest_givenValidTokenWithAllInfo_populateAllMetrics() throws Exception {
    final String countryCode = "AU";
    final String ipAddress = generateRandomIpAddress();
    final String jwtToken = createValidToken("jwt_all_attributes_present");
    List<Pair<String, String>> expectedTags = createGetTags(200, DEFAULT_ACCOUNT_NAME, ipAddress, countryCode, true);

    this.mockMvc.perform(build(get(PATH_REQUEST), countryCode, ipAddress, jwtToken)).andExpect(status().isOk());
    waitForMetricsToPublish(TIMER_METRIC_NAME.getAwsCountMetricName(), 1, CollectionGroup.DIRECT_API_SERVICE, expectedTags);

    assertEquals(9, expectedTags.size());
    validateTimerMetricDurationAndDimensions(TIMER_METRIC_NAME, 1, DEFAULT_OP_DURATION_IN_MILLIS, DEFAULT_OP_DURATION_IN_MILLIS, CollectionGroup.DIRECT_API_SERVICE,
        InstanceTypeTagValue.DATA_COLLECTOR, DeploymentEnvironment.PRODUCTION, expectedTags);
  }

  @Test
  void metricsTimer_success200HttpGetRequest_givenValidTokenWithMissingAccountName_populateAllMetrics() throws Exception {
    final String countryCode = "IN";
    final String ipAddress = generateRandomIpAddress();
    String jwtToken = createValidToken("jwt_missing_accountName");
    List<Pair<String, String>> expectedTags = createGetTags(200, "N/A", ipAddress, countryCode, true);

    this.mockMvc.perform(build(get(PATH_REQUEST), countryCode, ipAddress, jwtToken)).andExpect(status().isOk());
    waitForMetricsToPublish(TIMER_METRIC_NAME.getAwsCountMetricName(), 1, CollectionGroup.DIRECT_API_SERVICE, expectedTags);

    assertEquals(9, expectedTags.size());
    validateTimerMetricDurationAndDimensions(TIMER_METRIC_NAME, 1, DEFAULT_OP_DURATION_IN_MILLIS, DEFAULT_OP_DURATION_IN_MILLIS, CollectionGroup.DIRECT_API_SERVICE,
        InstanceTypeTagValue.DATA_COLLECTOR, DeploymentEnvironment.PRODUCTION, expectedTags);
  }

  @Test
  void metricsTimer_success200HttpGetRequestThreeTimes_givenValidTokenWithMissingAccountName_populateAllMetrics() throws Exception {
    final String countryCode = "AU";
    final String ipAddress = generateRandomIpAddress();
    final String jwtToken = createValidToken("jwt_all_attributes_present");
    List<Pair<String, String>> expectedTags = createGetTags(200, DEFAULT_ACCOUNT_NAME, ipAddress, countryCode, true);
    int count = 3;
    for (int i = 0; i < count; i++) {
      this.mockMvc.perform(build(get(PATH_REQUEST), countryCode, ipAddress, jwtToken)).andExpect(status().isOk());
    }
    waitForMetricsToPublish(TIMER_METRIC_NAME.getAwsCountMetricName(), 3, CollectionGroup.DIRECT_API_SERVICE, expectedTags);
    assertEquals(9, expectedTags.size());
    validateTimerMetricDurationAndDimensions(TIMER_METRIC_NAME, 3, DEFAULT_OP_DURATION_IN_MILLIS, DEFAULT_OP_DURATION_IN_MILLIS, CollectionGroup.DIRECT_API_SERVICE,
        InstanceTypeTagValue.DATA_COLLECTOR, DeploymentEnvironment.PRODUCTION, expectedTags);
  }

  @Test
  void metricsTimer_success202HttpPostRequest_givenValidTokenWithAllInfoAndNoCountryOrIPHeaders_populateAllMetrics() throws Exception {
    final String jwtToken = createValidToken("jwt_all_attributes_present");
    List<Pair<String, String>> expectedTags = createTags(202, HttpMethod.POST, "/testPostRequest", DEFAULT_ACCOUNT_NAME, "0.0.0.0", "N/A", true);

    this.mockMvc.perform(post("/testPostRequest").header(JwtTokenUtility.AUTHORIZATION, jwtToken)).andExpect(status().isAccepted());
    waitForMetricsToPublish(TIMER_METRIC_NAME.getAwsCountMetricName(), 1, CollectionGroup.DIRECT_API_SERVICE, expectedTags);

    assertEquals(9, expectedTags.size());
    validateTimerMetricDurationAndDimensions(TIMER_METRIC_NAME, 1, DEFAULT_OP_DURATION_IN_MILLIS, DEFAULT_OP_DURATION_IN_MILLIS, CollectionGroup.DIRECT_API_SERVICE,
        InstanceTypeTagValue.DATA_COLLECTOR, DeploymentEnvironment.PRODUCTION, expectedTags);
  }

  @Test
  void metricsTimer_success404HttpPutRequest_givenValidTokenNoUserIdAndAccountId_populateAllMetrics() throws Exception {
    final String countryCode = "US";
    final String ipAddress = generateRandomIpAddress();
    final String jwtToken = createValidToken("jwt_missing_userIdAndAccountId");
    List<Pair<String, String>> expectedTags = createTags(404, HttpMethod.PUT, "/testPutRequestWith404", DEFAULT_ACCOUNT_NAME, ipAddress, countryCode, "Unknown",
        "Unknown", true);

    this.mockMvc.perform(build(put("/testPutRequestWith404"), countryCode, ipAddress, jwtToken)).andExpect(status().isNotFound());
    waitForMetricsToPublish(TIMER_METRIC_NAME.getAwsCountMetricName(), 1, CollectionGroup.DIRECT_API_SERVICE, expectedTags);

    assertEquals(9, expectedTags.size());
    validateTimerMetricDurationAndDimensions(TIMER_METRIC_NAME, 1, DEFAULT_OP_DURATION_IN_MILLIS, DEFAULT_OP_DURATION_IN_MILLIS, CollectionGroup.DIRECT_API_SERVICE,
        InstanceTypeTagValue.DATA_COLLECTOR, DeploymentEnvironment.PRODUCTION, expectedTags);
  }

  @Test
  void metricsTimer_success204HttpDeleteRequest_givenValidTokenWithAllInfo_populateAllMetrics() throws Exception {
    final String countryCode = "AU";
    final String ipAddress = generateRandomIpAddress();
    final String jwtToken = createValidToken("jwt_all_attributes_present");
    List<Pair<String, String>> expectedTags = createTags(204, HttpMethod.DELETE, "/testDelete", DEFAULT_ACCOUNT_NAME,
        ipAddress, countryCode, true);

    this.mockMvc.perform(build(delete("/testDelete"), countryCode, ipAddress, jwtToken)).andExpect(status().isNoContent());
    waitForMetricsToPublish(TIMER_METRIC_NAME.getAwsCountMetricName(), 1, CollectionGroup.DIRECT_API_SERVICE, expectedTags);

    assertEquals(9, expectedTags.size());
    validateTimerMetricDurationAndDimensions(TIMER_METRIC_NAME, 1, DEFAULT_OP_DURATION_IN_MILLIS, DEFAULT_OP_DURATION_IN_MILLIS, CollectionGroup.DIRECT_API_SERVICE,
        InstanceTypeTagValue.DATA_COLLECTOR, DeploymentEnvironment.PRODUCTION, expectedTags);
  }

  @Test
  void metricsTimer_success202HttpHeadRequest_givenValidTokenWithAllInfo_populateAllMetrics() throws Exception {
    final String countryCode = "AU";
    final String ipAddress = generateRandomIpAddress();
    final String jwtToken = createValidToken("jwt_all_attributes_present");
    List<Pair<String, String>> expectedTags = createTags(202, HttpMethod.HEAD, "/testHeader", DEFAULT_ACCOUNT_NAME,
        ipAddress, countryCode, true);

    this.mockMvc.perform(build(head("/testHeader"), countryCode, ipAddress, jwtToken)).andExpect(status().isAccepted());
    waitForMetricsToPublish(TIMER_METRIC_NAME.getAwsCountMetricName(), 1, CollectionGroup.DIRECT_API_SERVICE, expectedTags);

    assertEquals(9, expectedTags.size());
    validateTimerMetricDurationAndDimensions(TIMER_METRIC_NAME, 1, DEFAULT_OP_DURATION_IN_MILLIS, DEFAULT_OP_DURATION_IN_MILLIS, CollectionGroup.DIRECT_API_SERVICE,
        InstanceTypeTagValue.DATA_COLLECTOR, DeploymentEnvironment.PRODUCTION, expectedTags);
  }

  @Test
  void metricsTimer_success500HttpPatchRequest_givenValidTokenWithAllInfo_populateAllMetrics() throws Exception {
    final String countryCode = "GB";
    final String ipAddress = generateRandomIpAddress();
    final String jwtToken = createValidToken("jwt_all_attributes_present");
    List<Pair<String, String>> expectedTags = createTags(500, HttpMethod.PATCH, "/testPatchRequestWith500", DEFAULT_ACCOUNT_NAME, ipAddress, countryCode, true);

    this.mockMvc.perform(build(patch("/testPatchRequestWith500"), countryCode, ipAddress, jwtToken)).andExpect(status().isInternalServerError());
    waitForMetricsToPublish(TIMER_METRIC_NAME.getAwsCountMetricName(), 1, CollectionGroup.DIRECT_API_SERVICE, expectedTags);

    assertEquals(9, expectedTags.size());
    validateTimerMetricDurationAndDimensions(TIMER_METRIC_NAME, 1, DEFAULT_OP_DURATION_IN_MILLIS, DEFAULT_OP_DURATION_IN_MILLIS, CollectionGroup.DIRECT_API_SERVICE,
        InstanceTypeTagValue.DATA_COLLECTOR, DeploymentEnvironment.PRODUCTION, expectedTags);
  }

  @Test
  void metricsTimer_failureHttpGetRequest_givenValidTokenWithAllInfo_populateAllMetricsWithFailureResult() throws Exception {
    final String countryCode = "VN";
    final String ipAddress = generateRandomIpAddress();
    final String jwtToken = createValidToken("jwt_all_attributes_present");
    // NOTE: Since there is no default exception handler, spring would treat the exception from PATCH method (or any method) as a success
    List<Pair<String, String>> expectedTags = createTags(200, HttpMethod.PATCH, "/testRuntimeException", DEFAULT_ACCOUNT_NAME, ipAddress, countryCode, false);

    assertThatThrownBy(() -> {
      this.mockMvc.perform(build(patch("/testRuntimeException"), countryCode, ipAddress, jwtToken));
    }).isInstanceOf(NestedServletException.class);
    waitForMetricsToPublish(TIMER_METRIC_NAME.getAwsCountMetricName(), 1, CollectionGroup.DIRECT_API_SERVICE, expectedTags);

    assertEquals(9, expectedTags.size());
    validateTimerMetricDurationAndDimensions(TIMER_METRIC_NAME, 1, DEFAULT_OP_DURATION_IN_MILLIS, DEFAULT_OP_DURATION_IN_MILLIS, CollectionGroup.DIRECT_API_SERVICE,
        InstanceTypeTagValue.DATA_COLLECTOR, DeploymentEnvironment.PRODUCTION, expectedTags);
  }

  @Test
  void metricsTimer_success200HttpGetRequest_givenInvalidJwtToken_populateAllMetricsExceptUserAccountDetails() throws Exception {
    final String countryCode = "MX";
    final String ipAddress = generateRandomIpAddress();
    final String jwtToken = createValidToken("invalid_token");
    List<Pair<String, String>> expectedTags = createTags(200, HttpMethod.GET, PATH_REQUEST, "N/A", ipAddress, countryCode, "Unknown", "Unknown", true);

    this.mockMvc.perform(build(get(PATH_REQUEST), countryCode, ipAddress, jwtToken)).andExpect(status().isOk());
    waitForMetricsToPublish(TIMER_METRIC_NAME.getAwsCountMetricName(), 1, CollectionGroup.DIRECT_API_SERVICE, expectedTags);

    assertEquals(9, expectedTags.size());
    validateTimerMetricDurationAndDimensions(TIMER_METRIC_NAME, 1, DEFAULT_OP_DURATION_IN_MILLIS, DEFAULT_OP_DURATION_IN_MILLIS, CollectionGroup.DIRECT_API_SERVICE,
        InstanceTypeTagValue.DATA_COLLECTOR, DeploymentEnvironment.PRODUCTION, expectedTags);
  }

  private MockHttpServletRequestBuilder build(MockHttpServletRequestBuilder builder, String countryCode, String ipAddress, String jwtToken) {
    return builder.header(MetricsFilter.ISO_COUNTRY_CODE, countryCode)
        .header(MetricsFilter.IP_ADDRESS, ipAddress)
        .header(JwtTokenUtility.AUTHORIZATION, jwtToken);
  }

  private String createValidToken(String jwtTokenFileName) throws IOException {
    return JwtTokenUtility.BEARER_HEADER + TestUtils.getToken("security/" + jwtTokenFileName);
  }

  private List<Pair<String, String>> createGetTags(int httpStatus, String accountName, String ipAddress, String countryCode, boolean isSuccessfulOperation) {
    return createTags(httpStatus, HttpMethod.GET, PATH_REQUEST, accountName, ipAddress, countryCode, isSuccessfulOperation);
  }

  private List<Pair<String, String>> createTags(int httpStatus, HttpMethod httpMethod, String requestUri, String accountName, String ipAddress,
      String countryCode, boolean isSuccessfulOperation) {
    return createTags(httpStatus, httpMethod, requestUri, accountName, ipAddress, countryCode, "1", "99", isSuccessfulOperation);
  }

  private List<Pair<String, String>> createTags(int httpStatus, HttpMethod httpMethod, String requestUri, String accountName, String ipAddress,
      String countryCode, String userId, String accountId, boolean isSuccessfulOperation) {
    return Arrays.asList(
        // Pair.of(MetricsServiceImpl.TAG_DEPLOYMENT_ENVIRONMENT, DeploymentEnvironment.PRODUCTION.getEnvironment()),
        Pair.of(RestApiDimensions.TAG_IP_ADDRESS, ipAddress),
        Pair.of(RestApiDimensions.TAG_REQUEST_URI, requestUri),
        Pair.of(RestApiDimensions.TAG_ACCOUNT_ID, accountId),
        Pair.of(RestApiDimensions.TAG_ACCOUNT_NAME, accountName),
        Pair.of(RestApiDimensions.TAG_USER_ID, userId),
        Pair.of(RestApiDimensions.TAG_COUNTRY_ISO_CODE, countryCode),
        Pair.of(RestApiDimensions.TAG_HTTP_STATUS, String.valueOf(httpStatus)),
        Pair.of(RestApiDimensions.TAG_HTTP_METHOD, httpMethod.name()),
        Pair.of(MetricsServiceImpl.TAG_RESULT, isSuccessfulOperation ? MetricsServiceImpl.SUCCESS : MetricsServiceImpl.FAILURE));
  }
}
