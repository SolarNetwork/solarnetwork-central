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
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

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
    then(json)
        .asInstanceOf(JSON)
        .as("Result is JSON array")
        .isArray()
        .containsExactly(
            json("""
                {"topic":"foo", "qos":1}
                """),
            json("""
                {"topic":"bar", "qos":2}
                """)
        )
        ;
    // @formatter:on
  }

  @Test
  public void fromJson() throws IOException {
    String json = "[{\"topic\":\"bim\",\"qos\":0},{\"topic\":\"bam\",\"qos\":1}]";

    TopicSettings s = objectMapper.readValue(json, TopicSettings.class);
    // @formatter:off
    then(s)
        .as("Settings array parsed")
        .extracting(TopicSettings::getSettings, list(TopicSubscriptionSetting.class))
        .hasSize(2)
        .satisfies(l -> {
            then(l).element(0)
                .as("Topic parsed")
                .returns("bim", from(TopicSubscriptionSetting::getTopic))
                .as("QoS parsed")
                .returns(Qos.AtMostOnce, from(TopicSubscriptionSetting::getQos))
                ;
            
            then(l).element(1)
                .as("Topic parsed")
                .returns("bam", from(TopicSubscriptionSetting::getTopic))
                .as("QoS parsed")
                .returns(Qos.AtLeastOnce, from(TopicSubscriptionSetting::getQos))
                ;
        })
        ;
    // @formatter:on
  }

}
