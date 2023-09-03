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

import static com.spotify.hamcrest.jackson.IsJsonMissing.jsonMissing;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonBoolean;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonInt;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonLong;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonObject;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterModifiers;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;

/**
 * Test cases for the {@link RegisterModifiers} class.
 * 
 * @author matt
 */
public class RegisterModifiersTests {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void jsonFull() {
    RegisterModifiers mods = RegisterModifiers.builder().withCleanSession(true)
        .withMaxInflightMessages(1).withMaxMessageRate(2).withMaxMessageSize(3).withRegView("foo")
        .withRetryInterval(4L).withSubscriberId("bar").withUpgradeQos(true).build();

    JsonNode json = objectMapper.valueToTree(mods);
    // @formatter:off
    assertThat(json, is(
        jsonObject()
          .where("clean_session", is(jsonBoolean(mods.getCleanSession())))
          .where("max_message_rate", is(jsonInt(mods.getMaxMessageRate())))
          .where("max_message_size", is(jsonInt(mods.getMaxMessageSize())))
          .where("max_inflight_messages", is(jsonInt(mods.getMaxInflightMessages())))
          .where("reg_view", is(jsonText(mods.getRegView())))
          .where("retry_interval", is(jsonLong(mods.getRetryInterval())))
          .where("subscriber_id", is(jsonText(mods.getSubscriberId())))
          .where("upgrade_qos", is(jsonBoolean(mods.getCleanSession())))
        ));
    // @formatter:on
  }

  @Test
  public void jsonSome() {
    RegisterModifiers mods = RegisterModifiers.builder().withMaxMessageSize(1)
        .withMaxInflightMessages(2).withRetryInterval(3L).build();

    JsonNode json = objectMapper.valueToTree(mods);
    // @formatter:off
    assertThat(json, is(
        jsonObject()
          .where("clean_session", is(jsonMissing()))
          .where("max_message_rate", is(jsonMissing()))
          .where("max_message_size", is(jsonInt(mods.getMaxMessageSize())))
          .where("max_inflight_messages", is(jsonInt(mods.getMaxInflightMessages())))
          .where("reg_view", is(jsonMissing()))
          .where("retry_interval", is(jsonLong(mods.getRetryInterval())))
          .where("subscriber_id", is(jsonMissing()))
          .where("upgrade_qos", is(jsonMissing()))
        ));
    // @formatter:on
  }

}
