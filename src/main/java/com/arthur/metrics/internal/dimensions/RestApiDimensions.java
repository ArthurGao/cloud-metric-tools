package com.arthur.metrics.internal.dimensions;

import static com.arthur.metrics.internal.MetricsServiceImpl.TAG_RESULT;
import static com.arthur.metrics.internal.MetricsServiceImpl.getResultTagValue;

import io.micrometer.core.instrument.Tags;
import java.util.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RestApiDimensions {

  public static final String TAG_REQUEST_URI = "Request URI";
  public static final String TAG_USER_ID = "User Id";
  public static final String TAG_ACCOUNT_ID = "Account Id";
  public static final String TAG_ACCOUNT_NAME = "Account Name";
  public static final String TAG_HTTP_STATUS = "Http Status";
  public static final String TAG_HTTP_METHOD = "Http Method";
  public static final String TAG_COUNTRY_ISO_CODE = "Country Code";
  public static final String TAG_IP_ADDRESS = "Ip Address";

  public static final String UNKNOWN = "Unknown";
  public static final String NOT_APPLICABLE = "N/A";


  private String userId;
  private String accountId;
  private String accountName;
  private int httpStatus;
  private String httpMethod;
  private String countryIsoCode;
  private String ipAddress;
  private String requestURI;
  private boolean operationSuccessful;

  public Tags getTags() {
    return Tags.of(TAG_REQUEST_URI, requestURI)
        .and(TAG_HTTP_STATUS, String.valueOf(httpStatus))
        .and(TAG_RESULT, getResultTagValue(operationSuccessful))
        .and(TAG_HTTP_METHOD, httpMethod)
        .and(TAG_USER_ID, Optional.ofNullable(userId).orElse(UNKNOWN))
        .and(TAG_ACCOUNT_ID, Optional.ofNullable(accountId).orElse(UNKNOWN))
        .and(TAG_ACCOUNT_NAME, Optional.ofNullable(accountName).orElse(NOT_APPLICABLE))
        .and(TAG_COUNTRY_ISO_CODE, Optional.ofNullable(countryIsoCode).orElse(NOT_APPLICABLE))
        .and(TAG_IP_ADDRESS, Optional.ofNullable(ipAddress).orElse("0.0.0.0"));
  }

}
