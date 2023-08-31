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

package net.solarnetwork.flux.vernemq.webhook.service.impl.test;

import static com.spotify.hamcrest.pojo.IsPojo.pojo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.flux.vernemq.webhook.service.impl.SnTokenDetails;
import net.solarnetwork.flux.vernemq.webhook.service.impl.SnTokenDetailsRowMapper;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link SnTokenDetailsRowMapper} class.
 * 
 * @author matt
 * @version 1.1
 */
@ExtendWith(MockitoExtension.class)
public class SnTokenDetailsRowMapperTests extends TestSupport {

  @Mock
  private ResultSet resultSet;

  @Test
  public void rowWithoutPolicy() throws SQLException {
    // given
    final Long userId = (long) (Math.random() * Double.MAX_VALUE);
    given(resultSet.getLong(SnTokenDetailsRowMapper.DEFAULT_USER_ID_COL)).willReturn(userId);

    final String tokenType = "ReadNodeData";
    given(resultSet.getString(SnTokenDetailsRowMapper.DEFAULT_TOKEN_TYPE_COL))
        .willReturn(tokenType);

    given(resultSet.getString(SnTokenDetailsRowMapper.DEFAULT_POLICY_COL)).willReturn(null);

    final String tokenId = UUID.randomUUID().toString();

    // when
    SnTokenDetails result = new SnTokenDetailsRowMapper(tokenId).mapRow(resultSet, 1);

    // then
    assertThat("Token ID", result.getTokenId(), equalTo(tokenId));
    assertThat("User ID", result.getUserId(), equalTo(userId));
    assertThat("Token type", result.getTokenType(), equalTo(tokenType));
    assertThat("Policy", result.getPolicy(), nullValue());
  }

  @Test
  public void rowWithPolicy() throws SQLException {
    // given
    final Long userId = (long) (Math.random() * Double.MAX_VALUE);
    given(resultSet.getLong(SnTokenDetailsRowMapper.DEFAULT_USER_ID_COL)).willReturn(userId);

    final String tokenType = "ReadNodeData";
    given(resultSet.getString(SnTokenDetailsRowMapper.DEFAULT_TOKEN_TYPE_COL))
        .willReturn(tokenType);

    given(resultSet.getString(SnTokenDetailsRowMapper.DEFAULT_POLICY_COL))
        .willReturn(classResourceAsString("security-policy-01.json", "UTF-8"));

    final String tokenId = UUID.randomUUID().toString();

    // when
    SnTokenDetails result = new SnTokenDetailsRowMapper(tokenId).mapRow(resultSet, 1);

    // then
    assertThat("Token ID", result.getTokenId(), equalTo(tokenId));
    assertThat("User ID", result.getUserId(), equalTo(userId));
    assertThat("Token type", result.getTokenType(), equalTo(tokenType));

    // @formatter:off
    assertThat("Policy", result.getPolicy(), 
        pojo(SecurityPolicy.class)
            .withProperty("nodeIds", contains(1L, 2L, 3L))
            .withProperty("sourceIds", contains("one", "two", "three"))
            .withProperty("minAggregation", equalTo(Aggregation.Month))
            .withProperty("notAfter", equalTo(
                Instant.ofEpochMilli(1544388330000L)))
    );
    // @formatter:on
  }
}
