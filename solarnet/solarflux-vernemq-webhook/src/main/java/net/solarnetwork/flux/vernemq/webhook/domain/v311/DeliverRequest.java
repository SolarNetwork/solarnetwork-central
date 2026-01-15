/* ========================================================================
 * Copyright 2024 SolarNetwork Foundation
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
import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.domain.Qos;
import net.solarnetwork.flux.vernemq.webhook.domain.v311.codec.DeliverRequestDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * A deliver request model.
 * 
 * @author matt
 * @version 2.1
 */
@JsonDeserialize(using = DeliverRequestDeserializer.class)
public class DeliverRequest implements Message {

	@JsonProperty("client_id")
	private final String clientId;

	private final String mountpoint;

	private final String username;

	private final String topic;

	private final byte[] payload;

	private DeliverRequest(Builder builder) {
		this.clientId = builder.clientId;
		this.mountpoint = builder.mountpoint;
		this.username = builder.username;
		this.topic = builder.topic;
		this.payload = builder.payload;
	}

	/**
	 * Creates builder to build {@link DeliverRequest}.
	 * 
	 * @return created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Creates a builder to build {@link DeliverRequest} and initialize it with
	 * the given object.
	 * 
	 * @param deliverReqeust
	 *        to initialize the builder with
	 * @return created builder
	 */
	public static Builder builder(DeliverRequest deliverReqeust) {
		return new Builder(deliverReqeust);
	}

	/**
	 * Builder to build {@link DeliverRequest}.
	 */
	public static final class Builder {

		@JsonProperty("client_id")
		private String clientId;
		private String mountpoint;
		private String username;
		private String topic;
		private byte[] payload;

		private Builder() {
		}

		private Builder(DeliverRequest deliverReqeust) {
			this.clientId = deliverReqeust.clientId;
			this.mountpoint = deliverReqeust.mountpoint;
			this.username = deliverReqeust.username;
			this.topic = deliverReqeust.topic;
			this.payload = deliverReqeust.payload;
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

		public Builder withTopic(String topic) {
			this.topic = topic;
			return this;
		}

		public Builder withPayload(byte[] payload) {
			this.payload = payload;
			return this;
		}

		public DeliverRequest build() {
			return new DeliverRequest(this);
		}
	}

	@Override
	public String toString() {
		return "DeliverRequest{" + username + ", " + topic + "}";
	}

	/**
	 * Get the client ID.
	 * 
	 * @return the clientId
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * Get the mount point.
	 * 
	 * @return the mount point
	 */
	public String getMountpoint() {
		return mountpoint;
	}

	/**
	 * Get the username.
	 * 
	 * @return the username
	 */
	public String getUsername() {
		return username;
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
	public Qos getQos() {
		return null;
	}

	@Override
	public Boolean getRetain() {
		return null;
	}

}
