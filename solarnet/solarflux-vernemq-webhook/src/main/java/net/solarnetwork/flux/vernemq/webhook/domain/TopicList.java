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

package net.solarnetwork.flux.vernemq.webhook.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import net.solarnetwork.util.StringUtils;

/**
 * A list of topics, implementing {@link ResponseTopics}.
 * 
 * @author matt
 */
public class TopicList implements ResponseTopics {

  private final List<String> topics;

  /**
   * Constructor.
   * 
   * @param topics
   *        the topics
   */
  @JsonCreator
  public TopicList(List<String> topics) {
    super();
    this.topics = topics;
  }

  @Override
  public String toString() {
    return (topics != null ? StringUtils.commaDelimitedStringFromCollection(topics) : "<<empty>>");
  }

  @JsonValue
  public List<String> getTopics() {
    return topics;
  }

}
