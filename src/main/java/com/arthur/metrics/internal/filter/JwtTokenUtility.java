package com.arthur.metrics.internal.filter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.arthur.metrics.internal.dimensions.RestApiDimensions;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

@Log4j2
public final class JwtTokenUtility {

  public static final String AUTHORIZATION = "Authorization";
  public static final String BEARER_HEADER = "Bearer ";
  private static final Gson gson = new Gson();

  protected static JwtClaims getClaimsFromJwtToken(HttpServletRequest request) throws InvalidJwtException {
    final String requestTokenHeader = request.getHeader(AUTHORIZATION);
    // JWT Token is in the form "Bearer token". Remove Bearer word and get only the Token
    String jwtToken = StringUtils.substring(requestTokenHeader, BEARER_HEADER.length());
    if (StringUtils.isBlank(jwtToken)) {
      throw new InvalidJwtException("JWT token is empty/missing");
    }
    try {
      int i = jwtToken.lastIndexOf('.');
      String withoutSignature = jwtToken.substring(0, i + 1);
      Jwt<Header, Claims> untrusted = Jwts.parserBuilder().build().parseClaimsJwt(withoutSignature);
      JsonElement jsonElement = gson.toJsonTree(untrusted.getBody());
      return gson.fromJson(jsonElement, JwtClaims.class);
    } catch (Throwable e) {
      throw new InvalidJwtException("Could not parse JWT token due to " + e.getMessage(), e);
    }
  }
}

@Data
@NoArgsConstructor
class JwtClaims implements Serializable {

  @SerializedName(value = "internal_user_id")
  private Long userId;

  @SerializedName(value = "internal_account_id")
  private Long accountId;

  @SerializedName(value = "internal_account_name")
  private String accountName;
}