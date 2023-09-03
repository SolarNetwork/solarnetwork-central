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
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonInt;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonObject;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSubscriptionSetting;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link TopicSubscriptionSetting} class.
 * 
 * @author matt
 */
public class TopicSubscriptionSettingTests extends TestSupport {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void toJsonFull() throws JsonProcessingException {
    TopicSubscriptionSetting topic = TopicSubscriptionSetting.builder().withTopic("foo")
        .withQos(Qos.AtLeastOnce).build();
    String json = objectMapper.writeValueAsString(topic);
    log.debug("Topic setting full JSON: {}", json);

    // @formatter:off
    assertThat(json, isJsonStringMatching(
        jsonObject()
          .where("topic", is(jsonText(topic.getTopic())))
          .where("qos", is(jsonInt(topic.getQos().getKey())))
        ));
    // @formatter:on
  }

  @Test
  public void fromJson() throws IOException {
    String json = "{\"topic\":\"foobar\",\"qos\":1}";

    TopicSubscriptionSetting s = objectMapper.readValue(json, TopicSubscriptionSetting.class);
    assertThat("Topic", s.getTopic(), equalTo("foobar"));
    assertThat("Qos", s.getQos(), equalTo(Qos.AtLeastOnce));
  }

}
