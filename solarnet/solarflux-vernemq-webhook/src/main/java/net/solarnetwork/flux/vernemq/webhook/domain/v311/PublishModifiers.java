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

package net.solarnetwork.flux.vernemq.webhook.domain.v311;

import java.util.Arrays;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.ResponseModifiers;

/**
 * Publish response modifiers.
 * 
 * @author matt
 * @version 1.0
 */
@JsonDeserialize(builder = PublishModifiers.Builder.class)
public class PublishModifiers implements ResponseModifiers, Message {

  private final String topic;

  private final Qos qos;

  private final byte[] payload;

  private final Boolean retain;

  private PublishModifiers(Builder builder) {
    this.topic = builder.topic;
    this.qos = builder.qos;
    this.payload = builder.payload;
    this.retain = builder.retain;
  }

  /**
   * Creates builder to build {@link PublishModifiers}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link PublishModifiers}.
   */
  public static final class Builder {

    private String topic;
    private Qos qos;
    private byte[] payload;
    private Boolean retain;

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

    public Builder withPayload(byte[] payload) {
      this.payload = payload;
      return this;
    }

    public Builder withRetain(Boolean retain) {
      this.retain = retain;
      return this;
    }

    public PublishModifiers build() {
      return new PublishModifiers(this);
    }
  }

  @Override
  public String toString() {
    return "PublishModifiers{" + (topic != null ? "topic=" + topic + ", " : "")
        + (qos != null ? "qos=" + qos + ", " : "")
        + (payload != null ? "payload=" + Arrays.toString(payload) + ", " : "")
        + (retain != null ? "retain=" + retain : "") + "}";
  }

  @Override
  public String getTopic() {
    return topic;
  }

  @Override
  public Qos getQos() {
    return qos;
  }

  @Override
  public byte[] getPayload() {
    return payload;
  }

  @Override
  public Boolean getRetain() {
    return retain;
  }

}
