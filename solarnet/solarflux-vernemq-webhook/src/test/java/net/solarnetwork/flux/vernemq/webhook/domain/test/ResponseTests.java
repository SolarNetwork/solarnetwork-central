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

package net.solarnetwork.flux.vernemq.webhook.domain.test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static org.assertj.core.api.BDDAssertions.then;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicList;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterModifiers;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link Response} class.
 * 
 * @author matt
 */
public class ResponseTests extends TestSupport {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void jsonOkSimple() throws JsonProcessingException {
    Response r = new Response();
    String json = objectMapper.writeValueAsString(r);
    log.debug("OK simple JSON: {}", json);

    // @formatter:off
    then(json)
        .asInstanceOf(JSON)
        .as("Result is JSON object")
        .isObject()
        .containsOnlyKeys("result")
        .as("Result OK")
        .containsEntry("result", "ok")
        ;
    // @formatter:on
  }

  @Test
  public void jsonErrorSimple() throws JsonProcessingException {
    Response r = new Response("fail");
    String json = objectMapper.writeValueAsString(r);
    log.debug("Error simple JSON: {}", json);

    // @formatter:off
    then(json)
        .asInstanceOf(JSON)
        .as("Result is JSON object")
        .isObject()
        .containsOnlyKeys("result")
        .as("Result error object")
        .node("result")
            .isObject()
            .containsOnlyKeys("error")
            .as("Error")
            .containsEntry("error", "fail")
        ;
    // @formatter:on
  }

  @Test
  public void jsonOkWithModifiers() throws JsonProcessingException {
    RegisterModifiers mods = RegisterModifiers.builder().withUpgradeQos(false).build();
    Response r = new Response(mods);
    String json = objectMapper.writeValueAsString(r);
    log.debug("Ok with mods JSON: {}", json);

    // @formatter:off
    then(json)
        .asInstanceOf(JSON)
        .as("Result is JSON object")
        .isObject()
        .containsOnlyKeys("result", "modifiers")
        .as("Result OK")
        .containsEntry("result", "ok")
        .node("modifiers")
            .isObject()
            .containsOnlyKeys("upgrade_qos")
            .as("Upgrade QoS")
            .containsEntry("upgrade_qos", false)
        ;
    // @formatter:on
  }

  @Test
  public void jsonOkWithTopicSettings() throws JsonProcessingException {
    TopicSettings topics = new TopicSettings(Arrays.asList(
        TopicSubscriptionSetting.builder().withTopic("foo").withQos(Qos.AtLeastOnce).build()));
    Response r = new Response(topics);
    String json = objectMapper.writeValueAsString(r);
    log.debug("Ok with topic settings JSON: {}", json);

    // @formatter:off
    then(json)
        .asInstanceOf(JSON)
        .as("Result is JSON object")
        .isObject()
        .containsOnlyKeys("result", "topics")
        .as("Result OK")
        .containsEntry("result", "ok")
        .node("topics")
        .isArray()
        .as("Topic specified as array")
        .hasSize(1)
        .element(0)
            .isObject()
            .containsOnlyKeys("topic", "qos")
            .as("Topic name provided")
            .containsEntry("topic", "foo")
            .as("Topic QoS provided")
            .containsEntry("qos", Qos.AtLeastOnce.getKey())
        ;
    // @formatter:on
  }

  @Test
  public void jsonOkWithTopicList() throws JsonProcessingException {
    TopicList topics = new TopicList(Arrays.asList("foo", "bar"));
    Response r = new Response(topics);
    String json = objectMapper.writeValueAsString(r);
    log.debug("Ok with topics list JSON: {}", json);

    // @formatter:off
    then(json)
        .asInstanceOf(JSON)
        .as("Result is JSON object")
        .isObject()
        .containsOnlyKeys("result", "topics")
        .as("Result OK")
        .containsEntry("result", "ok")
        .node("topics")
        .isArray()
        .as("Topics specified")
        .containsExactly("foo", "bar")
        ;
    // @formatter:on
  }

}
