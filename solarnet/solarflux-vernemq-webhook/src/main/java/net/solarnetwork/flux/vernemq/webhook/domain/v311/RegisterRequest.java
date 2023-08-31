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

/**
 * A register or auth-register request model.
 * 
 * @author matt
 */
@JsonDeserialize(builder = RegisterRequest.Builder.class)
public class RegisterRequest implements AuthRequest {

  @JsonProperty("client_id")
  private final String clientId;

  private final String mountpoint;

  private final String username;

  @JsonProperty("peer_addr")
  private final String peerAddress;

  @JsonProperty("peer_port")
  private final Integer peerPort;

  private final String password;

  @JsonProperty("clean_session")
  private final Boolean cleanSession;

  private RegisterRequest(Builder builder) {
    this.peerAddress = builder.peerAddress;
    this.peerPort = builder.peerPort;
    this.username = builder.username;
    this.password = builder.password;
    this.mountpoint = builder.mountpoint;
    this.clientId = builder.clientId;
    this.cleanSession = builder.cleanSession;
  }

  /**
   * Creates builder to build {@link RegisterRequest}, configured as a copy of another request.
   * 
   * @param request
   *        the request to copy
   * @return created builder
   */
  public static Builder builder(RegisterRequest request) {
    // @formatter:off
    return new Builder()
        .withCleanSession(request.getCleanSession())
        .withClientId(request.getClientId())
        .withMountpoint(request.getMountpoint())
        .withPassword(request.getPassword())
        .withPeerAddress(request.getPeerAddress())
        .withPeerPort(request.getPeerPort())
        .withUsername(request.getUsername());
    // @formatter:on
  }

  /**
   * Creates builder to build {@link RegisterRequest}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link RegisterRequest}.
   */
  public static final class Builder {

    @JsonProperty("peer_addr")
    private String peerAddress;

    @JsonProperty("peer_port")
    private Integer peerPort;

    private String username;
    private String password;
    private String mountpoint;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("clean_session")
    private Boolean cleanSession;

    private Builder() {
    }

    public Builder withPeerAddress(String peerAddress) {
      this.peerAddress = peerAddress;
      return this;
    }

    public Builder withPeerPort(Integer peerPort) {
      this.peerPort = peerPort;
      return this;
    }

    public Builder withUsername(String username) {
      this.username = username;
      return this;
    }

    public Builder withPassword(String password) {
      this.password = password;
      return this;
    }

    public Builder withMountpoint(String mountpoint) {
      this.mountpoint = mountpoint;
      return this;
    }

    public Builder withClientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder withCleanSession(Boolean cleanSession) {
      this.cleanSession = cleanSession;
      return this;
    }

    public RegisterRequest build() {
      return new RegisterRequest(this);
    }
  }

  public String getPeerAddress() {
    return peerAddress;
  }

  public Integer getPeerPort() {
    return peerPort;
  }

  @Override
  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public String getMountpoint() {
    return mountpoint;
  }

  @Override
  public String getClientId() {
    return clientId;
  }

  public Boolean getCleanSession() {
    return cleanSession;
  }

}
