/* ========================================================================
 * Copyright 2024 SolarNetwork Foundation
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
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;

import net.solarnetwork.flux.vernemq.webhook.domain.HookType;
import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.service.AuditService;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;
import net.solarnetwork.flux.vernemq.webhook.web.NonAuthHooksController;

/**
 * Test cases for the {@link NonAuthHooksController} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
@SpringJUnitConfig
@WebMvcTest(NonAuthHooksController.class)
public class NonAuthHooksControllerTests extends TestSupport {

  private static final String OK_RESPONSE_JSON = "{\"result\":\"ok\"}";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private AuditService authService;

  @Captor
  private ArgumentCaptor<Message> messageCaptor;

  @Test
  public void onDeliver() throws Exception {
    // GIVEN

    // WHEN

    // @formatter:off
    mvc.perform(
        post("/hook")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HOOK_HEADER, HookType.OnDeliver.getKey())
            .content(classResourceAsBytes("on_deliver-01.json"))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(OK_RESPONSE_JSON));
    // @formatter:on

    // THEN
    then(authService).should().auditDeliverMessage(messageCaptor.capture());

    // @formatter:off
    and.then(messageCaptor.getValue())
      .as("Topic from JSON")
      .returns("a/b", from(Message::getTopic))
      .as("Payload")
      .returns(Base64.getDecoder().decode("aGVsbG8="), from(Message::getPayload))
      ;
    // @formatter:on
  }

}
