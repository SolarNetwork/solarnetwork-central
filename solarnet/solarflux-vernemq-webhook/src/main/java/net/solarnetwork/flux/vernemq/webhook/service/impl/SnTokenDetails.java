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

package net.solarnetwork.flux.vernemq.webhook.service.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import net.solarnetwork.central.security.SecurityPolicy;

/**
 * Token authentication result details.
 * 
 * @author matt
 * @version 1.0
 */
@JsonDeserialize(builder = SnTokenDetails.Builder.class)
public class SnTokenDetails {

  private final String tokenId;
  private final Long userId;
  private final String tokenType;
  private final SecurityPolicy policy;

  private SnTokenDetails(Builder builder) {
    this.tokenId = builder.tokenId;
    this.userId = builder.userId;
    this.tokenType = builder.tokenType;
    this.policy = builder.policy;
  }

  /**
   * Creates builder to build {@link SnTokenDetails}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link SnTokenDetails}.
   */
  public static final class Builder {

    private String tokenId;
    private Long userId;
    private String tokenType;
    private SecurityPolicy policy;

    private Builder() {
    }

    public Builder withTokenId(String tokenId) {
      this.tokenId = tokenId;
      return this;
    }

    public Builder withUserId(Long userId) {
      this.userId = userId;
      return this;
    }

    public Builder withTokenType(String tokenType) {
      this.tokenType = tokenType;
      return this;
    }

    public Builder withPolicy(SecurityPolicy policy) {
      this.policy = policy;
      return this;
    }

    public SnTokenDetails build() {
      return new SnTokenDetails(this);
    }
  }

  public String getTokenId() {
    return tokenId;
  }

  public Long getUserId() {
    return userId;
  }

  public String getTokenType() {
    return tokenType;
  }

  public SecurityPolicy getPolicy() {
    return policy;
  }

}
