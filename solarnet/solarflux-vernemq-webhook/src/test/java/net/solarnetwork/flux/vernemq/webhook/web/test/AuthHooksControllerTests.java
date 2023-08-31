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

package net.solarnetwork.flux.vernemq.webhook.web.test;

import static net.solarnetwork.flux.vernemq.webhook.domain.HookType.HOOK_HEADER;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;

import net.solarnetwork.flux.vernemq.webhook.domain.HookType;
import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.PublishRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.SubscribeRequest;
import net.solarnetwork.flux.vernemq.webhook.service.AuthService;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;
import net.solarnetwork.flux.vernemq.webhook.web.AuthHooksController;

@SpringJUnitConfig
@WebMvcTest(AuthHooksController.class)
public class AuthHooksControllerTests extends TestSupport {

  private static final String OK_RESPONSE_JSON = "{\"result\":\"ok\"}";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private AuthService authService;

  @Test
  public void authOnRegister() throws Exception {
    // given
    Response resp = new Response();
    given(authService.authenticateRequest(Mockito.any(RegisterRequest.class))).willReturn(resp);

    // when

    // @formatter:off
    mvc.perform(
        post("/hook")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HOOK_HEADER, HookType.AuthenticateOnRegister.getKey())
            .content(classResourceAsBytes("auth_on_register-01.json"))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(OK_RESPONSE_JSON));
    // @formatter:on
  }

  @Test
  public void authOnPublish() throws Exception {
    // given
    Response resp = new Response();
    given(authService.authorizeRequest(Mockito.any(PublishRequest.class))).willReturn(resp);

    // when

    // @formatter:off
    mvc.perform(
        post("/hook")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HOOK_HEADER, HookType.AuthorizeOnPublish.getKey())
            .content(classResourceAsBytes("auth_on_publish-01.json"))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(OK_RESPONSE_JSON));
    // @formatter:on
  }

  @Test
  public void authOnSubscribe() throws Exception {
    // given
    Response resp = new Response();
    given(authService.authorizeRequest(Mockito.any(SubscribeRequest.class))).willReturn(resp);

    // when

    // @formatter:off
    mvc.perform(
        post("/hook")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HOOK_HEADER, HookType.AuthorizeOnSubscribe.getKey())
            .content(classResourceAsBytes("auth_on_subscribe-01.json"))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(OK_RESPONSE_JSON));
    // @formatter:on
  }

}
