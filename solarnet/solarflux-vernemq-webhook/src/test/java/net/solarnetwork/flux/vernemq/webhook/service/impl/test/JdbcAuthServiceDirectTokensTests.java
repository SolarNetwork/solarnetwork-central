/* ========================================================================
 * Copyright 2019 SolarNetwork Foundation
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;

import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.domain.ActorDetails;
import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import net.solarnetwork.flux.vernemq.webhook.domain.ResponseStatus;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.service.AuthorizationEvaluator;
import net.solarnetwork.flux.vernemq.webhook.service.impl.ActorDetailsRowMapper;
import net.solarnetwork.flux.vernemq.webhook.service.impl.JdbcAuthService;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link JdbcAuthService} when direct token authentication is enabled.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class JdbcAuthServiceDirectTokensTests extends TestSupport {

  @Mock
  private JdbcOperations jdbcOps;

  @Mock
  private AuthorizationEvaluator authorizationEvaluator;

  private JdbcAuthService authService;

  @BeforeEach
  public void setup() {
    authService = new JdbcAuthService(jdbcOps, authorizationEvaluator);
    authService.setAllowDirectTokenAuthentication(true);
  }

  @Test
  public void authenticateUsernameMissing() {
    // given
    RegisterRequest req = RegisterRequest.builder().build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticateUsernameEmpty() {
    // given
    RegisterRequest req = RegisterRequest.builder().withClientId("").withUsername("").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticatePasswordMissing() {
    // given
    RegisterRequest req = RegisterRequest.builder().withClientId("token").withUsername("token")
        .build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticatePasswordEmpty() {
    // given
    RegisterRequest req = RegisterRequest.builder().withClientId("token").withUsername("token")
        .withPassword("").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticatePasswordMalformedTokens() {
    // given
    RegisterRequest req = RegisterRequest.builder().withClientId("token").withUsername("token")
        .withPassword("not a password").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticatePasswordRequestDateMissing() {
    // given
    RegisterRequest req = RegisterRequest.builder().withClientId("token").withUsername("token")
        .withPassword("Signature=010203").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticatePasswordRequestDateEmpty() {
    // given
    RegisterRequest req = RegisterRequest.builder().withClientId("token").withUsername("token")
        .withPassword("Date=,Signature=010203").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Error", r.getStatus(), equalTo(ResponseStatus.ERROR));
    log.debug("Got error: {}", r.getErrorStatus());
  }

  @Test
  public void authenticatePasswordRequestDateMalformed() {
    // given
    RegisterRequest req = RegisterRequest.builder().withClientId("token").withUsername("token")
        .withPassword("Date=foo,Signature=010203").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Error", r.getStatus(), equalTo(ResponseStatus.ERROR));
    log.debug("Got error: {}", r.getErrorStatus());
  }

  @Test
  public void authenticatePasswordSignatureMissing() {
    // given
    RegisterRequest req = RegisterRequest.builder().withClientId("token").withUsername("token")
        .withPassword("Date=123").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));
  }

  @Test
  public void authenticateNodeIpMaskAllowed() {
    // given
    authService.setIpMask("128.0.0.4/31");
    RegisterRequest req = RegisterRequest.builder().withUsername("solarnode").withClientId("2")
        .withPeerAddress("128.0.0.5").build();

    List<Actor> actors = Arrays.asList(new ActorDetails(123L, 2L));
    given(jdbcOps.query(Mockito.any(PreparedStatementCreator.class),
        Mockito.any(ActorDetailsRowMapper.class))).willReturn(actors);

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.OK));
  }

  @Test
  public void authenticateNodeIpMaskDenied() {
    // given
    authService.setIpMask("128.0.0.4/31");
    RegisterRequest req = RegisterRequest.builder().withUsername("solarnode")
        .withPeerAddress("10.0.0.1").build();

    // when
    Response r = authService.authenticateRequest(req);

    // then
    assertThat("Next", r.getStatus(), equalTo(ResponseStatus.NEXT));

    then(jdbcOps).should(Mockito.never()).query(any(PreparedStatementCreator.class),
        any(ActorDetailsRowMapper.class));
  }

}
