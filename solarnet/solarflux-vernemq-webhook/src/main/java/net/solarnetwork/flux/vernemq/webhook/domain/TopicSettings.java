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
 * A list of topic subscription settings.
 * 
 * @author matt
 */
public class TopicSettings implements ResponseTopics {

  private final List<TopicSubscriptionSetting> settings;

  /**
   * Constructor.
   * 
   * @param settings
   *        the settings
   */
  @JsonCreator
  public TopicSettings(List<TopicSubscriptionSetting> settings) {
    super();
    this.settings = settings;
  }

  @JsonValue
  public List<TopicSubscriptionSetting> getSettings() {
    return settings;
  }

  @Override
  public String toString() {
    return (settings != null ? StringUtils.commaDelimitedStringFromCollection(settings)
        : "<<empty>>");
  }

}
