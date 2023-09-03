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

import net.solarnetwork.flux.vernemq.webhook.domain.ResponseModifiers;

/**
 * Registration response modifiers.
 * 
 * @author matt
 */
public class RegisterModifiers implements ResponseModifiers {

  @JsonProperty("subscriber_id")
  private final String subscriberId;

  @JsonProperty("reg_view")
  private final String regView;

  @JsonProperty("clean_session")
  private final Boolean cleanSession;

  @JsonProperty("max_message_size")
  private final Integer maxMessageSize;

  @JsonProperty("max_message_rate")
  private final Integer maxMessageRate;

  @JsonProperty("max_inflight_messages")
  private final Integer maxInflightMessages;

  @JsonProperty("retry_interval")
  private final Long retryInterval;

  @JsonProperty("upgrade_qos")
  private final Boolean upgradeQos;

  private RegisterModifiers(Builder builder) {
    this.subscriberId = builder.subscriberId;
    this.regView = builder.regView;
    this.cleanSession = builder.cleanSession;
    this.maxMessageSize = builder.maxMessageSize;
    this.maxMessageRate = builder.maxMessageRate;
    this.maxInflightMessages = builder.maxInflightMessages;
    this.retryInterval = builder.retryInterval;
    this.upgradeQos = builder.upgradeQos;
  }

  /**
   * Creates builder to build {@link RegisterModifiers}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link RegisterModifiers}.
   */
  public static class Builder {

    private String subscriberId;
    private String regView;
    private Boolean cleanSession;
    private Integer maxMessageSize;
    private Integer maxMessageRate;
    private Integer maxInflightMessages;
    private Long retryInterval;
    private Boolean upgradeQos;

    private Builder() {
      super();
    }

    public Builder withSubscriberId(String subscriberId) {
      this.subscriberId = subscriberId;
      return this;
    }

    public Builder withRegView(String regView) {
      this.regView = regView;
      return this;
    }

    public Builder withCleanSession(Boolean cleanSession) {
      this.cleanSession = cleanSession;
      return this;
    }

    public Builder withMaxMessageSize(Integer maxMessageSize) {
      this.maxMessageSize = maxMessageSize;
      return this;
    }

    public Builder withMaxMessageRate(Integer maxMessageRate) {
      this.maxMessageRate = maxMessageRate;
      return this;
    }

    public Builder withMaxInflightMessages(Integer maxInflightMessages) {
      this.maxInflightMessages = maxInflightMessages;
      return this;
    }

    public Builder withRetryInterval(Long retryInterval) {
      this.retryInterval = retryInterval;
      return this;
    }

    public Builder withUpgradeQos(Boolean upgradeQos) {
      this.upgradeQos = upgradeQos;
      return this;
    }

    public RegisterModifiers build() {
      return new RegisterModifiers(this);
    }
  }

  @Override
  public String toString() {
    return "RegisterModifiers{"
        + (subscriberId != null ? "subscriberId=" + subscriberId + ", " : "")
        + (regView != null ? "regView=" + regView + ", " : "")
        + (cleanSession != null ? "cleanSession=" + cleanSession + ", " : "")
        + (maxMessageSize != null ? "maxMessageSize=" + maxMessageSize + ", " : "")
        + (maxMessageRate != null ? "maxMessageRate=" + maxMessageRate + ", " : "")
        + (maxInflightMessages != null ? "maxInflightMessages=" + maxInflightMessages + ", " : "")
        + (retryInterval != null ? "retryInterval=" + retryInterval + ", " : "")
        + (upgradeQos != null ? "upgradeQos=" + upgradeQos : "") + "}";
  }

  public String getSubscriberId() {
    return subscriberId;
  }

  public String getRegView() {
    return regView;
  }

  public Boolean getCleanSession() {
    return cleanSession;
  }

  public Integer getMaxMessageSize() {
    return maxMessageSize;
  }

  public Integer getMaxMessageRate() {
    return maxMessageRate;
  }

  public Integer getMaxInflightMessages() {
    return maxInflightMessages;
  }

  public Long getRetryInterval() {
    return retryInterval;
  }

  public Boolean getUpgradeQos() {
    return upgradeQos;
  }

}
