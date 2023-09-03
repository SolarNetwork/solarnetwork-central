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

package net.solarnetwork.flux.vernemq.webhook.test;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcOperations;

import net.solarnetwork.central.security.SecurityPolicy;

/**
 * Utilities to help with SolarNetwork database access.
 * 
 * @author matt
 * @version 1.1
 */
public final class DbUtils {

  /** The "read node data" token type. */
  public static final String READ_NODE_DATA_TOKEN_TYPE = "ReadNodeData";

  /** The "user" token type. */
  public static final String USER_TOKEN_TYPE = "User";

  /**
   * Insert a new SolarNetwork user.
   * 
   * <p>
   * The {@code email} value will be set to {@literal test@localhost}.
   * </p>
   * 
   * @param jdbcOps
   *        the JDBC ops
   * @param userId
   *        the user ID
   */
  public static void createUser(JdbcOperations jdbcOps, Long userId) {
    createUser(jdbcOps, userId, "test@localhost");
  }

  /**
   * Insert a new SolarNetwork user.
   * 
   * @param jdbcOps
   *        the JDBC ops
   * @param userId
   *        the user ID
   * @param email
   *        the user email
   */
  public static void createUser(JdbcOperations jdbcOps, Long userId, String email) {
    jdbcOps.update("INSERT INTO solaruser.user_user(id,email,disp_name,password) VALUES (?,?,?,?)",
        userId, email, "Test User", "test.password");
  }

  /**
   * Insert a new SolarNetwork security token.
   * 
   * @param jdbcOps
   *        the JDBC ops
   * @param tokenId
   *        the token ID
   * @param tokenSecret
   *        the token secret
   * @param userId
   *        the user ID
   * @param active
   *        {@literal} true if active
   * @param type
   *        the type, e.g. {@link #READ_NODE_DATA_TOKEN_TYPE}
   * @param policy
   *        the policy
   */
  public static void createToken(JdbcOperations jdbcOps, String tokenId, String tokenSecret,
      Long userId, boolean active, String type, SecurityPolicy policy) {
    // CHECKSTYLE OFF: LineLength
    jdbcOps.update(
        "INSERT INTO solaruser.user_auth_token(auth_token,auth_secret,user_id,status,token_type,jpolicy)"
            + " VALUES (?,?,?,?::solaruser.user_auth_token_status,?::solaruser.user_auth_token_type,?::jsonb)",
        tokenId, tokenSecret, userId, active ? "Active" : "Disabled", type,
        net.solarnetwork.codec.JsonUtils.getJSONString(policy, null));
    // CHECKSTYLE ON: LineLength
  }

  /**
   * Insert a new SolarNode association with a user.
   * 
   * @param jdbcOps
   *        the JDBC ops
   * @param userId
   *        the user ID
   * @param nodeId
   *        the node ID
   */
  public static void createUserNode(JdbcOperations jdbcOps, Long userId, Long nodeId) {
    try {
      jdbcOps.queryForObject("SELECT node_id FROM solarnet.sn_node WHERE node_id = ?", Long.class,
          nodeId);
    } catch (IncorrectResultSizeDataAccessException e) {
      // assume node does not exist yet
      Long locId = null;
      try {
        locId = jdbcOps.queryForObject("SELECT id FROM solarnet.sn_loc ORDER BY id LIMIT 1",
            Long.class);
      } catch (IncorrectResultSizeDataAccessException e2) {
        // assume location does not exist yet
        locId = 0L;
        jdbcOps.update("INSERT INTO solarnet.sn_loc (id,country,time_zone) VALUES (?,?,?)", locId,
            "-", "UTC");
      }
      jdbcOps.update("INSERT INTO solarnet.sn_node (node_id,loc_id) VALUES (?,?)", nodeId, locId);
    }

    jdbcOps.update("INSERT INTO solaruser.user_node(user_id,node_id) VALUES (?,?)", userId, nodeId);
  }
}
