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
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.solarnetwork.flux.vernemq.webhook.domain.TopicList;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;

/**
 * Test cases for the {@link TopicList} class.
 * 
 * @author matt
 */
public class TopicListTests extends TestSupport {

  private ObjectMapper objectMapper;

  @BeforeEach
  public void setup() {
    objectMapper = JsonUtils.defaultObjectMapper();
  }

  @Test
  public void toJsonFull() throws JsonProcessingException {
    TopicList list = new TopicList(Arrays.asList("foo", "bar"));
    String json = objectMapper.writeValueAsString(list);
    log.debug("Topic settings full JSON: {}", json);

    // @formatter:off
    assertThat(json, isJsonStringMatching(
        jsonArray(contains(
          jsonText("foo"),
          jsonText("bar")
        ))));
    // @formatter:on
  }

  @Test
  public void fromJson() throws IOException {
    String json = "[\"bim\",\"bam\"]";

    TopicList list = objectMapper.readValue(json, TopicList.class);
    assertThat("List size", list.getTopics(), hasSize(2));
    assertThat("Topic 1", list.getTopics().get(0), equalTo("bim"));
    assertThat("Topic 2", list.getTopics().get(1), equalTo("bam"));
  }

}
