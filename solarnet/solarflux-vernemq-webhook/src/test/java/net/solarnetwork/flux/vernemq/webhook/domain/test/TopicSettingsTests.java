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

import static com.spotify.hamcrest.jackson.IsJsonStringMatching.isJsonStringMatching;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonArray;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonInt;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonObject;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link TopicSettings} class.
 * 
 * @author matt
 */
public class TopicSettingsTests extends TestSupport {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void toJsonFull() throws JsonProcessingException {
    TopicSettings settings = new TopicSettings(Arrays.asList(
        TopicSubscriptionSetting.builder().withTopic("foo").withQos(Qos.AtLeastOnce).build(),
        TopicSubscriptionSetting.builder().withTopic("bar").withQos(Qos.ExactlyOnce).build()));
    String json = objectMapper.writeValueAsString(settings);
    log.debug("Topic settings full JSON: {}", json);

    // @formatter:off
    assertThat(json, isJsonStringMatching(
        jsonArray(contains(
          jsonObject()
            .where("topic", is(jsonText("foo")))
            .where("qos", is(jsonInt(1))),
          jsonObject()
            .where("topic", is(jsonText("bar")))
            .where("qos", is(jsonInt(2)))
        ))));
    // @formatter:on
  }

  @Test
  public void fromJson() throws IOException {
    String json = "[{\"topic\":\"bim\",\"qos\":0},{\"topic\":\"bam\",\"qos\":1}]";

    TopicSettings s = objectMapper.readValue(json, TopicSettings.class);
    assertThat("Setting size", s.getSettings(), hasSize(2));
    assertThat("Topic 1", s.getSettings().get(0).getTopic(), equalTo("bim"));
    assertThat("Qos 1", s.getSettings().get(0).getQos(), equalTo(Qos.AtMostOnce));
    assertThat("Topic 2", s.getSettings().get(1).getTopic(), equalTo("bam"));
    assertThat("Qos 2", s.getSettings().get(1).getQos(), equalTo(Qos.AtLeastOnce));
  }

}
