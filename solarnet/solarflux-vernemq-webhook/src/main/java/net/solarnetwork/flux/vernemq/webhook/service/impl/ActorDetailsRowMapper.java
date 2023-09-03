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

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.domain.ActorDetails;
import net.solarnetwork.flux.vernemq.webhook.domain.ActorType;

/**
 * {@link RowMapper} for {@link ActorDetails}.
 * 
 * @author matt
 * @version 1.1
 */
public class ActorDetailsRowMapper implements RowMapper<Actor> {

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

  /**
   * The default value for the {@code nodeIdsCol} property.
   */
  public static int DEFAULT_NODE_IDS_COL = 4;

  private static final Logger log = LoggerFactory.getLogger(ActorDetailsRowMapper.class);

  private final String tokenId;
  private final int userIdCol;
  private final int tokenTypeCol;
  private final int policyCol;
  private final int nodeIdsCol;

  /**
   * Constructor with default settings.
   * 
   * @param tokenId
   *        the token ID
   */
  public ActorDetailsRowMapper(String tokenId) {
    this(tokenId, DEFAULT_USER_ID_COL, DEFAULT_TOKEN_TYPE_COL, DEFAULT_POLICY_COL,
        DEFAULT_NODE_IDS_COL);
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
   * @param nodeIdsCol
   *        the JDBC column for the node IDs array
   */
  public ActorDetailsRowMapper(String tokenId, int userIdCol, int tokenTypeCol, int policyCol,
      int nodeIdsCol) {
    super();
    this.tokenId = tokenId;
    this.userIdCol = userIdCol;
    this.tokenTypeCol = tokenTypeCol;
    this.policyCol = policyCol;
    this.nodeIdsCol = nodeIdsCol;
  }

  @Override
  public Actor mapRow(ResultSet rs, int rowNum) throws SQLException {
    Long userId = rs.getLong(userIdCol);
    String tokenType = rs.getString(tokenTypeCol);
    String policyJson = rs.getString(policyCol);
    SecurityPolicy policy = null;
    if (policyJson != null) {
      policy = JsonUtils.getObjectFromJSON(policyJson, BasicSecurityPolicy.class);
    }

    ActorType actorType = ActorType.forValue(tokenType);
    boolean publishAllowed = (actorType == ActorType.Node);

    Set<Long> nodeIds = null;
    Array dbNodeIds = rs.getArray(nodeIdsCol);
    if (dbNodeIds != null) {
      Object data = dbNodeIds.getArray();
      if (data != null && data.getClass().isArray()) {
        Object[] arrayData = (Object[]) data;
        nodeIds = new LinkedHashSet<>(arrayData.length);
        for (int i = 0; i < arrayData.length; i++) {
          Object val = arrayData[i];
          if (val instanceof Long) {
            nodeIds.add((Long) val);
          } else if (val instanceof Number) {
            nodeIds.add(((Number) val).longValue());
          } else {
            log.warn("Unexpected non-Number node ID array value returned from DB: [{}]", val);
          }
        }
      }
    }

    return new ActorDetails(this.tokenId, actorType, publishAllowed, userId, policy, nodeIds);
  }

}
