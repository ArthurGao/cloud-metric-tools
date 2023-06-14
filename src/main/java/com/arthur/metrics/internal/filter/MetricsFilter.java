package com.arthur.metrics.internal.filter;

import com.arthur.metrics.internal.dimensions.RestApiDimensions;
import com.arthur.metrics.meters.MetricsTimer;
import com.arthur.metrics.meters.MetricsTimer.TimerInProgress;
import com.arthur.metrics.service.Metrics;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MetricsFilter extends HttpFilter {

  protected static final String METRIC_NAME_DURATION = "restapi.duration";
  protected static final String IP_ADDRESS = "X-FORWARDED-FOR";
  protected static final String ISO_COUNTRY_CODE = "x-country-code";

  private final Metrics metricsService;

  public MetricsFilter(Metrics metricsService) {
    this.metricsService = metricsService;
  }

  @Override
  public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    RestApiDimensions restApiDimensions = new RestApiDimensions();
    enrichDimensionsByRequest(request, restApiDimensions);
    final MetricsTimer metricsTimer = metricsService.createOrGetTimer(METRIC_NAME_DURATION);
    TimerInProgress timerInProgress = metricsTimer.start();
    try {
      chain.doFilter(request, response);
      enrichDimensionsByResponse(restApiDimensions, response, true);
    } catch (Exception e) {
      enrichDimensionsByResponse(restApiDimensions, response, false);
      throw e;
    } finally {
      timerInProgress.stop(restApiDimensions.getTags());
    }
  }

  private void enrichDimensionsByRequest(HttpServletRequest request, RestApiDimensions restApiDimensions) {
    restApiDimensions.setRequestURI(request.getRequestURI());
    restApiDimensions.setHttpMethod(request.getMethod());
    enrichDimensionsWithUserAndAccount(request, restApiDimensions);
    getFromRequestHeader(request, IP_ADDRESS).ifPresent(restApiDimensions::setIpAddress);
    getFromRequestHeader(request, ISO_COUNTRY_CODE).ifPresent(restApiDimensions::setCountryIsoCode);
  }

  private void enrichDimensionsWithUserAndAccount(HttpServletRequest request, RestApiDimensions restApiDimensions) {
    try {
      final JwtClaims jwtClaims = JwtTokenUtility.getClaimsFromJwtToken(request);
      restApiDimensions.setAccountId(Optional.ofNullable(jwtClaims.getAccountId()).map(id -> id.toString()).orElse(null));
      restApiDimensions.setAccountName(jwtClaims.getAccountName());
      restApiDimensions.setUserId(Optional.ofNullable(jwtClaims.getUserId()).map(id -> id.toString()).orElse(null));
    } catch (InvalidJwtException e) {
      log.warn("Could not parse the JWT token for request {} due to {}.", request.getRequestURI(), e.getMessage(), e);
    }
  }

  private void enrichDimensionsByResponse(RestApiDimensions restApiDimensions, HttpServletResponse response, boolean isSuccess) {
    restApiDimensions.setHttpStatus(response.getStatus());
    restApiDimensions.setOperationSuccessful(isSuccess);
  }

  private static Optional<String> getFromRequestHeader(HttpServletRequest request, String name) {
    return Optional.ofNullable(request.getHeader(name));
  }
}
