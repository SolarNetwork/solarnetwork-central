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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import net.solarnetwork.flux.vernemq.webhook.domain.AuthRequest;
import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.domain.Qos;

/**
 * A publish or auth-publish request model.
 * 
 * @author matt
 */
@JsonDeserialize(builder = PublishRequest.Builder.class)
public class PublishRequest implements Message, AuthRequest {

  @JsonProperty("client_id")
  private final String clientId;

  private final String mountpoint;

  private final String username;

  private final Qos qos;

  private final String topic;

  private final byte[] payload;

  private final Boolean retain;

  private PublishRequest(Builder builder) {
    this.clientId = builder.clientId;
    this.mountpoint = builder.mountpoint;
    this.username = builder.username;
    this.qos = builder.qos;
    this.topic = builder.topic;
    this.payload = builder.payload;
    this.retain = builder.retain;
  }

  /**
   * Creates builder to build {@link PublishRequest}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates builder to build {@link PublishRequest}, configured as a copy of another request.
   * 
   * @param request
   *        the request to copy
   * @return created builder
   */
  public static Builder builder(PublishRequest request) {
    // @formatter:off
    return new Builder()
        .withClientId(request.getClientId())
        .withMountpoint(request.getMountpoint())
        .withPayload(request.getPayload())
        .withQos(request.getQos())
        .withRetain(request.getRetain())
        .withTopic(request.getTopic())
        .withUsername(request.getUsername());
    // @formatter:on
  }

  /**
   * Builder to build {@link PublishRequest}.
   */
  public static final class Builder {

    @JsonProperty("client_id")
    private String clientId;

    private String mountpoint;

    private String username;

    private Qos qos;

    private String topic;

    private byte[] payload;

    private Boolean retain;

    private Builder() {
    }

    public Builder withClientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder withMountpoint(String mountpoint) {
      this.mountpoint = mountpoint;
      return this;
    }

    public Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    public Builder withQos(Qos qos) {
      this.qos = qos;
      return this;
    }

    public Builder withTopic(String topic) {
      this.topic = topic;
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

    public PublishRequest build() {
      return new PublishRequest(this);
    }
  }

  @Override
  public String toString() {
    return "PublishRequest{" + username + ", " + topic + "@" + qos + "}";
  }

  @Override
  public String getClientId() {
    return clientId;
  }

  @Override
  public String getMountpoint() {
    return mountpoint;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public Qos getQos() {
    return qos;
  }

  @Override
  public String getTopic() {
    return topic;
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
