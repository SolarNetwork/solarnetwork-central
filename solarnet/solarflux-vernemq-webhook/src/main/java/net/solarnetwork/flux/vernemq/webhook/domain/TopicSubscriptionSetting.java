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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A topic subscription setting.
 *
 * @author matt
 */
@JsonPropertyOrder({ "topic", "qos", "no_local", "rap", "retain_handling" })
@JsonDeserialize(builder = TopicSubscriptionSetting.Builder.class)
public class TopicSubscriptionSetting {

  private final String topic;
  private final Qos qos;

  @JsonProperty("no_local")
  private final Boolean noLocal;
  private final Boolean rap;

  @JsonProperty("retain_handling")
  private final String retainHandling;

  private TopicSubscriptionSetting(Builder builder) {
    this.topic = builder.topic;
    this.qos = builder.qos;
    this.noLocal = builder.noLocal;
    this.rap = builder.rap;
    this.retainHandling = builder.retainHandling;
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

    @JsonProperty("no_local")
    private Boolean noLocal;
    private Boolean rap;

    @JsonProperty("retain_handling")
    private String retainHandling;

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

    public Builder withNoLocal(Boolean noLocal) {
      this.noLocal = noLocal;
      return this;
    }

    public Builder withRap(Boolean rap) {
      this.rap = rap;
      return this;
    }

    public Builder withRetainHandling(String retainHandling) {
      this.retainHandling = retainHandling;
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

  public String getTopic() {
    return topic;
  }

  @Override
  public int hashCode() {
    return Objects.hash(topic, qos, noLocal, rap, retainHandling);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TopicSubscriptionSetting other)) {
      return false;
    }
    return Objects.equals(topic, other.topic) && qos == other.qos
        && Objects.equals(noLocal, other.noLocal) && Objects.equals(rap, other.rap)
        && Objects.equals(retainHandling, other.retainHandling);
  }

  public Qos getQos() {
    return qos;
  }

  public Boolean getNoLocal() {
    return noLocal;
  }

  public Boolean getRap() {
    return rap;
  }

  public String getRetainHandling() {
    return retainHandling;
  }

}
