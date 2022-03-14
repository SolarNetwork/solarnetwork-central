/* ==================================================================
 * BasicDatumAppEvent.java - 29/05/2020 3:58:24 pm
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

package net.solarnetwork.central.datum.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.solarnetwork.event.BasicAppEvent;

/**
 * Basic immutable implementation of {@link DatumAppEvent}.
 * 
 * @author matt
 * @version 2.0
 * @since 2.6
 */
@JsonDeserialize(builder = BasicDatumAppEvent.Builder.class)
public class BasicDatumAppEvent extends BasicAppEvent implements DatumAppEvent {

	private final Long nodeId;
	private final String sourceId;

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
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @throws IllegalArgumentException
	 *         if {@code topic} or {@code nodeId} or {@code sourceId} are
	 *         {@literal null} or empty
	 */
	public BasicDatumAppEvent(String topic, Map<String, ?> eventProperties, Long nodeId,
			String sourceId) {
		this(topic, Instant.now(), eventProperties, nodeId, sourceId);
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
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @throws IllegalArgumentException
	 *         if {@code topic} or {@code nodeId} or {@code sourceId} are
	 *         {@literal null} or empty
	 */
	public BasicDatumAppEvent(String topic, Instant created, Map<String, ?> eventProperties, Long nodeId,
			String sourceId) {
		super(topic, created, eventProperties);
		this.nodeId = requireNonNullArgument(nodeId, "nodeId");
		if ( sourceId == null || sourceId.isEmpty() ) {
			throw new IllegalArgumentException("The sourceId parameter must not be null.");
		}
		this.sourceId = sourceId;
	}

	private BasicDatumAppEvent(Builder builder) {
		this(builder.getTopic(), builder.getCreated(), builder.getEventProperties(), builder.nodeId,
				builder.sourceId);
	}

	@Override
	public Long getNodeId() {
		return nodeId;
	}

	@Override
	public String getSourceId() {
		return sourceId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(nodeId, sourceId);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof BasicDatumAppEvent) ) {
			return false;
		}
		BasicDatumAppEvent other = (BasicDatumAppEvent) obj;
		return Objects.equals(nodeId, other.nodeId) && Objects.equals(sourceId, other.sourceId);
	}

	@Override
	public String toString() {
		StringBuilder builder2 = new StringBuilder();
		builder2.append("BasicDatumAppEvent{");
		builder2.append("topic=");
		builder2.append(getTopic());
		builder2.append(", ");
		builder2.append("created=");
		builder2.append(getCreated());
		builder2.append(", ");
		builder2.append("nodeId=");
		builder2.append(nodeId);
		builder2.append(", ");
		builder2.append("sourceId=");
		builder2.append(sourceId);
		builder2.append(", ");
		if ( getEventProperties() != null && !getEventProperties().isEmpty() ) {
			builder2.append("eventProperties=");
			builder2.append(getEventProperties());
		}
		builder2.append("}");
		return builder2.toString();
	}

	/**
	 * Creates builder to build {@link BasicDatumAppEvent}.
	 * 
	 * @return created builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Get a builder, populated with this instance's values.
	 * 
	 * @return a pre-populated builder
	 */
	@Override
	public Builder toBuilder() {
		return new Builder(this);
	}

	/**
	 * Builder to build {@link BasicDatumAppEvent}.
	 */
	public static class Builder extends BasicAppEvent.Builder {

		private Long nodeId;
		private String sourceId;

		private Builder() {
			super();
		}

		protected Builder(DatumAppEvent other) {
			super(other);
			this.nodeId = other.getNodeId();
			this.sourceId = other.getSourceId();
		}

		public Builder withNodeId(Long nodeId) {
			this.nodeId = nodeId;
			return this;
		}

		public Builder withSourceId(String sourceId) {
			this.sourceId = sourceId;
			return this;
		}

		@Override
		public BasicDatumAppEvent build() {
			return new BasicDatumAppEvent(this);
		}
	}

}
