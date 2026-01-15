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

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.SubscribeRequest;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;
import tools.jackson.databind.ObjectMapper;

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
    // WHEN
    SubscribeRequest req = objectMapper.readValue(classResourceAsBytes("auth_on_subscribe-01.json"),
        SubscribeRequest.class);

    // THEN
    // @formatter:off
    then(req)
      .returns("clientid", from(SubscribeRequest::getClientId))
      .returns("", from(SubscribeRequest::getMountpoint))
      .returns("username", from(SubscribeRequest::getUsername))
      .extracting(r -> r.getTopics().getSettings(), list(TopicSubscriptionSetting.class))
      .hasSize(2)
      .satisfies(l -> {
        then(l).element(0)
          .returns("a/b", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.AtLeastOnce, from(TopicSubscriptionSetting::getQos))
          ;
        then(l).element(1)
          .returns("c/d", from(TopicSubscriptionSetting::getTopic))
          .returns(Qos.ExactlyOnce, from(TopicSubscriptionSetting::getQos))
          ;
        })
      ;
    // @formatter:on
  }

}
