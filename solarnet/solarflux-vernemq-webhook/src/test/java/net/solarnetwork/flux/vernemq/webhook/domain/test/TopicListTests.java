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
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.solarnetwork.flux.vernemq.webhook.domain.TopicList;
import net.solarnetwork.flux.vernemq.webhook.test.JsonUtils;
import net.solarnetwork.flux.vernemq.webhook.test.TestSupport;
import tools.jackson.databind.ObjectMapper;

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
  public void toJsonFull() {
    TopicList list = new TopicList(Arrays.asList("foo", "bar"));
    String json = objectMapper.writeValueAsString(list);
    log.debug("Topic settings full JSON: {}", json);

    // @formatter:off
    then(json)
        .asInstanceOf(JSON)
        .as("Result is JSON array")
        .isArray()
        .as("Topic specified as array")
        .containsExactly("foo", "bar")
        ;
    // @formatter:on
  }

  @Test
  public void fromJson() throws IOException {
    String json = "[\"bim\",\"bam\"]";

    TopicList list = objectMapper.readValue(json, TopicList.class);

    // @formatter:off
    then(list)
        .isNotNull()
        .extracting(TopicList::getTopics, list(String.class))
        .as("Parsed topics array")
        .containsExactly("bim", "bam")
        ;
    // @formatter:on
  }

}
