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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A topic subscription setting.
 * 
 * @author matt
 */
@JsonPropertyOrder({ "topic", "qos" })
@JsonDeserialize(builder = TopicSubscriptionSetting.Builder.class)
public class TopicSubscriptionSetting {

  private final String topic;
  private final Qos qos;

  private TopicSubscriptionSetting(Builder builder) {
    this.topic = builder.topic;
    this.qos = builder.qos;
  }

  /**
   * Creates builder to build {@link TopicSubscriptionSetting}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link TopicSubscriptionSetting}.
   */
  public static final class Builder {

    private String topic;
    private Qos qos;

    private Builder() {
    }

    public Builder withTopic(String topic) {
      this.topic = topic;
      return this;
    }

    public Builder withQos(Qos qos) {
      this.qos = qos;
      return this;
    }

    public TopicSubscriptionSetting build() {
      return new TopicSubscriptionSetting(this);
    }
  }

  @Override
  public String toString() {
    return topic + "@" + qos;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((qos == null) ? 0 : qos.hashCode());
    result = prime * result + ((topic == null) ? 0 : topic.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    TopicSubscriptionSetting other = (TopicSubscriptionSetting) obj;
    if (qos != other.qos) {
      return false;
    }
    if (topic == null) {
      if (other.topic != null) {
        return false;
      }
    } else if (!topic.equals(other.topic)) {
      return false;
    }
    return true;
  }

  public String getTopic() {
    return topic;
  }

  public Qos getQos() {
    return qos;
  }

}
