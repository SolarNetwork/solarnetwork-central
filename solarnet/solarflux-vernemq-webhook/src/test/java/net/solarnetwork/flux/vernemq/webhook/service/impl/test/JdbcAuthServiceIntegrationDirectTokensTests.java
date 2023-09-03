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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;

import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import net.solarnetwork.flux.vernemq.webhook.domain.ResponseStatus;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.test.DbUtils;
import net.solarnetwork.io.RFC1924OutputStream;

/**
 * JDBC integration tests.
 * 
 * @author matt
 * @version 1.0
 */
@SpringJUnitConfig
@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class JdbcAuthServiceIntegrationDirectTokensTests extends JdbcAuthServiceIntegrationTests {

  private SecureRandom rng;

  @Override
  @BeforeEach
  public void setup() {
    super.setup();
    authService.setAllowDirectTokenAuthentication(true);
    try {
      rng = SecureRandom.getInstanceStrong();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private String generateTokenSecret(SecureRandom rng, int byteCount) {
    try {
      byte[] randomBytes = new byte[byteCount];
      rng.nextBytes(randomBytes);
      ByteArrayOutputStream byos = new ByteArrayOutputStream((int) Math.ceil(byteCount * 1.25));
      FileCopyUtils.copy(randomBytes, new RFC1924OutputStream(byos));
      return byos.toString("US-ASCII");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void authenticateOk_directToken() {
    // given
    final Long userId = 123L;
    DbUtils.createUser(jdbcOps, userId);
    final String tokenId = "test.token";
    final String tokenSecret = generateTokenSecret(rng, 16);
    DbUtils.createToken(jdbcOps, tokenId, tokenSecret, userId, true,
        DbUtils.READ_NODE_DATA_TOKEN_TYPE, null);

    RegisterRequest req = RegisterRequest.builder().withClientId(tokenId).withUsername(tokenId)
        .withPassword(tokenSecret).build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Result", r.getStatus(), equalTo(ResponseStatus.OK));
    assertThat("No modifiers", r.getModifiers(), nullValue());
  }

  @Test
  public void authenticateFailed_directToken_badSecret() {
    // given
    final Long userId = 123L;
    DbUtils.createUser(jdbcOps, userId);
    final String tokenId = "test.token";
    final String tokenSecret = generateTokenSecret(rng, 16);
    DbUtils.createToken(jdbcOps, tokenId, tokenSecret, userId, true,
        DbUtils.READ_NODE_DATA_TOKEN_TYPE, null);

    RegisterRequest req = RegisterRequest.builder().withClientId(tokenId).withUsername(tokenId)
        .withPassword("bad secret").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Result", r.getStatus(), equalTo(ResponseStatus.NEXT));
    assertThat("No modifiers", r.getModifiers(), nullValue());
  }
}
