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
import net.solarnetwork.flux.vernemq.webhook.domain.TopicSettings;

/**
 * A subscribe or auth-subscribe request model.
 * 
 * @author matt
 */
@JsonDeserialize(builder = SubscribeRequest.Builder.class)
public class SubscribeRequest implements AuthRequest {

  @JsonProperty("client_id")
  private final String clientId;

  private final String mountpoint;

  private final String username;

  private final TopicSettings topics;

  private SubscribeRequest(Builder builder) {
    this.clientId = builder.clientId;
    this.mountpoint = builder.mountpoint;
    this.username = builder.username;
    this.topics = builder.topics;
  }

  /**
   * Creates builder to build {@link SubscribeRequest}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates builder to build {@link SubscribeRequest}, configured as a copy of another request.
   * 
   * @param request
   *        the request to copy
   * @return created builder
   */
  public static Builder builder(SubscribeRequest request) {
    // @formatter:off
    return new Builder()
        .withClientId(request.getClientId())
        .withMountpoint(request.getMountpoint())
        .withTopics(request.getTopics())
        .withUsername(request.getUsername());
    // @formatter:on
  }

  /**
   * Builder to build {@link SubscribeRequest}.
   */
  public static final class Builder {

    @JsonProperty("client_id")
    private String clientId;

    private String mountpoint;

    private String username;

    private TopicSettings topics;

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

    public Builder withTopics(TopicSettings topics) {
      this.topics = topics;
      return this;
    }

    public SubscribeRequest build() {
      return new SubscribeRequest(this);
    }
  }

  @Override
  public String toString() {
    return "SubscribeRequest{" + username + ", " + (topics != null ? topics : "-") + "}";
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

  public TopicSettings getTopics() {
    return topics;
  }

}
