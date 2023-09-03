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

package net.solarnetwork.flux.vernemq.webhook.domain.v311.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link RegisterRequest} class.
 * 
 * @author matt
 */
public class RegisterRequestTests extends TestSupport {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void parseFull() throws IOException {
    RegisterRequest req = objectMapper.readValue(classResourceAsBytes("auth_on_register-01.json"),
        RegisterRequest.class);
    assertThat("clean_session", req.getCleanSession(), equalTo(false));
    assertThat("client_id", req.getClientId(), equalTo("clientid"));
    assertThat("mountpoint", req.getMountpoint(), equalTo(""));
    assertThat("password", req.getPassword(), equalTo("password"));
    assertThat("peerAddress", req.getPeerAddress(), equalTo("127.0.0.1"));
    assertThat("peerPort", req.getPeerPort(), equalTo(8888));
    assertThat("username", req.getUsername(), equalTo("username"));
  }

}
