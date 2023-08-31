/* ========================================================================
 * Copyright 2018 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.flux.vernemq.webhook.service.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.codec.JsonUtils;

/**
 * {@link RowMapper} for {@link SnTokenDetails}.
 * 
 * @author matt
 * @version 1.1
 */
public class SnTokenDetailsRowMapper implements RowMapper<SnTokenDetails> {

  /**
   * The default value for the {@code userIdCol} property.
   */
  public static int DEFAULT_USER_ID_COL = 1;

  /**
   * The default value for the {@code tokenTypeCol} property.
   */
  public static int DEFAULT_TOKEN_TYPE_COL = 2;

  /**
   * The default value for the {@code policyCol} property.
   */
  public static int DEFAULT_POLICY_COL = 3;

  private final String tokenId;
  private final int userIdCol;
  private final int tokenTypeCol;
  private final int policyCol;

  /**
   * Constructor with default settings.
   * 
   * @param tokenId
   *        the token ID
   */
  public SnTokenDetailsRowMapper(String tokenId) {
    this(tokenId, DEFAULT_USER_ID_COL, DEFAULT_TOKEN_TYPE_COL, DEFAULT_POLICY_COL);
  }

  /**
   * Constructor.
   * 
   * @param tokenId
   *        the token ID
   * @param userIdCol
   *        the JDBC column for the user ID
   * @param tokenTypeCol
   *        the JDBC column for the token type
   * @param policyCol
   *        the JDBC column for the policy
   */
  public SnTokenDetailsRowMapper(String tokenId, int userIdCol, int tokenTypeCol, int policyCol) {
    super();
    this.tokenId = tokenId;
    this.userIdCol = userIdCol;
    this.tokenTypeCol = tokenTypeCol;
    this.policyCol = policyCol;
  }

  @Override
  public SnTokenDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
    Long userId = rs.getLong(userIdCol);
    String tokenType = rs.getString(tokenTypeCol);
    String policyJson = rs.getString(policyCol);
    SecurityPolicy policy = null;
    if (policyJson != null) {
      policy = JsonUtils.getObjectFromJSON(policyJson, BasicSecurityPolicy.class);
    }
    // @formatter:off
    return SnTokenDetails.builder()
        .withTokenId(this.tokenId)
        .withUserId(userId)
        .withTokenType(tokenType)
        .withPolicy(policy)
        .build();
    // @formatter:on
  }

}
