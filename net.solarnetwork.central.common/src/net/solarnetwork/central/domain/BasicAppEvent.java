/* ==================================================================
 * BasicAppEvent.java - 29/05/2020 3:41:41 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Basic immutable {@link AppEvent}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.5
 */
@JsonDeserialize(builder = BasicAppEvent.Builder.class)
public class BasicAppEvent implements AppEvent {

	private final String topic;
	private final Instant created;
	private final Map<String, ?> eventProperties;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The event creation date will be set to the current time.
	 * </p>
	 * 
	 * @param topic
	 *        the event topic
	 * @param eventProperties
	 *        the event properties, or {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code topic} is {@literal null}
	 */
	public BasicAppEvent(String topic, Map<String, ?> eventProperties) {
		this(topic, Instant.now(), eventProperties);
	}

	/**
	 * Constructor.
	 * 
	 * @param topic
	 *        the event topic
	 * @param created
	 *        the event creation date, or {@literal null} to use the current
	 *        time
	 * @param eventProperties
	 *        the event properties, or {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code topic} is {@literal null} or empty
	 */
	public BasicAppEvent(String topic, Instant created, Map<String, ?> eventProperties) {
		super();
		if ( topic == null || topic.isEmpty() ) {
			throw new IllegalArgumentException("The topic parameter must not be null.");
		}
		this.topic = topic;
		if ( created == null ) {
			created = Instant.now();
		}
		this.created = created;
		if ( eventProperties == null ) {
			eventProperties = Collections.emptyMap();
		}
		this.eventProperties = eventProperties;
	}

	private BasicAppEvent(Builder builder) {
		this(builder.topic, builder.created, builder.eventProperties);
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, eventProperties, topic);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof BasicAppEvent) ) {
			return false;
		}
		BasicAppEvent other = (BasicAppEvent) obj;
		return Objects.equals(created, other.created)
				&& Objects.equals(eventProperties, other.eventProperties)
				&& Objects.equals(topic, other.topic);
	}

	@Override
	public String toString() {
		StringBuilder builder2 = new StringBuilder();
		builder2.append("BasicAppEvent{");
		builder2.append("topic=");
		builder2.append(topic);
		builder2.append(", ");
		builder2.append("created=");
		builder2.append(created);
		builder2.append(", ");
		if ( eventProperties != null && !eventProperties.isEmpty() ) {
			builder2.append("eventProperties=");
			builder2.append(eventProperties);
		}
		builder2.append("}");
		return builder2.toString();
	}

	/**
	 * Creates builder to build {@link BasicAppEvent}.
	 * 
	 * @return created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getTopic() {
		return topic;
	}

	@Override
	public Instant getCreated() {
		return created;
	}

	@Override
	public Map<String, ?> getEventProperties() {
		return eventProperties;
	}

	/**
	 * Get a builder, populated with this instance's values.
	 * 
	 * @return a pre-populated builder
	 */
	public Builder toBuilder() {
		return new Builder(this);
	}

	/**
	 * Builder to build {@link BasicAppEvent}.
	 */
	public static class Builder {

		private String topic;
		private Instant created;
		private Map<String, ?> eventProperties = Collections.emptyMap();

		protected Builder() {
		}

		protected Builder(AppEvent basicAppEvent) {
			this.topic = basicAppEvent.getTopic();
			this.created = basicAppEvent.getCreated();
			this.eventProperties = basicAppEvent.getEventProperties();
		}

		/**
		 * Get the configured topic.
		 * 
		 * @return the topic the topic
		 */
		public String getTopic() {
			return topic;
		}

		/**
		 * Configure the topic.
		 * 
		 * @param topic
		 *        the topic
		 * @return this builder
		 */
		public Builder withTopic(String topic) {
			this.topic = topic;
			return this;
		}

		/**
		 * Get the configured creation date.
		 * 
		 * @return the creation date
		 */
		public Instant getCreated() {
			return created;
		}

		/**
		 * Configure the creation date.
		 * 
		 * @param created
		 *        the creation date
		 * @return the
		 */
		public Builder withCreated(Instant created) {
			this.created = created;
			return this;
		}

		/**
		 * Get the configured event properties.
		 * 
		 * @return the event properties
		 */
		public Map<String, ?> getEventProperties() {
			return eventProperties;
		}

		/**
		 * Configure event properties.
		 * 
		 * @param eventProperties
		 *        the properties, or {@literal null}
		 * @return this builder
		 */
		public Builder withEventProperties(Map<String, ?> eventProperties) {
			if ( eventProperties == null ) {
				eventProperties = Collections.emptyMap();
			}
			this.eventProperties = eventProperties;
			return this;
		}

		/**
		 * Build the event instance.
		 * 
		 * @return the event
		 */
		public BasicAppEvent build() {
			return new BasicAppEvent(this);
		}
	}

}
