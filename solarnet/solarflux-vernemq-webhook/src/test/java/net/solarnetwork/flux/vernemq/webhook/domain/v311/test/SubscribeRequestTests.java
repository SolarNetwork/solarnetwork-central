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

import static com.spotify.hamcrest.pojo.IsPojo.pojo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.SubscribeRequest;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link SubscribeRequest} class.
 * 
 * @author matt
 */
public class SubscribeRequestTests extends TestSupport {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void parseFull() throws IOException {
    SubscribeRequest req = objectMapper.readValue(classResourceAsBytes("auth_on_subscribe-01.json"),
        SubscribeRequest.class);
    assertThat("client_id", req.getClientId(), equalTo("clientid"));
    assertThat("mountpoint", req.getMountpoint(), equalTo(""));
    assertThat("username", req.getUsername(), equalTo("username"));

    // @formatter:off
    assertThat("topics", req.getTopics(), 
        pojo(TopicSettings.class)
            .withProperty("settings", contains(
                pojo(TopicSubscriptionSetting.class)
                  .withProperty("topic", equalTo("a/b"))
                  .withProperty("qos", equalTo(Qos.AtLeastOnce)),
                pojo(TopicSubscriptionSetting.class)
                  .withProperty("topic", equalTo("c/d"))
                  .withProperty("qos", equalTo(Qos.ExactlyOnce))
            ))
    );
    // @formatter:on
  }

}
