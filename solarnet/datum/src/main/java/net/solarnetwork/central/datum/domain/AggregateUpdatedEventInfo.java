/* ==================================================================
 * AggregateUpdatedEventInfo.java - 4/06/2020 8:42:04 pm
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * {@link DatumAppEvent} properties object for {@link DatumAppEvent} aggregate
 * updates.
 *
 * @author matt
 * @version 1.1
 */
@JsonPropertyOrder({ "aggregationKey", "timestamp" })
public class AggregateUpdatedEventInfo {

	/**
	 * A {@link DatumAppEvent} topic for when a datum aggregate has been
	 * updated.
	 *
	 * <p>
	 * The event properties shall be the same as those produced by serializing
	 * this class as a JSON object.
	 * </p>
	 */
	public static final String AGGREGATE_UPDATED_TOPIC = "datum/agg/update";

	private final Aggregation aggregation;
	private final Instant timeStart;

	/**
	 * Constructor.
	 *
	 * @param aggregationKey
	 *        the aggregation key
	 * @param timeStart
	 *        the time start
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null} or {@code aggregationKey} is not
	 *         a valid {@link Aggregation} key
	 */
	@JsonCreator
	public AggregateUpdatedEventInfo(@JsonProperty("aggregationKey") String aggregationKey,
			@JsonProperty("timestamp") Instant timeStart) {
		this(Aggregation.forKey(aggregationKey), timeStart);
	}

	/**
	 * Constructor.
	 *
	 * @param aggregation
	 *        the aggregation
	 * @param timeStart
	 *        the time start
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public AggregateUpdatedEventInfo(Aggregation aggregation, Instant timeStart) {
		super();
		this.aggregation = requireNonNullArgument(aggregation, "aggregation");
		this.timeStart = requireNonNullArgument(timeStart, "timeStart");
	}

	/**
	 * Get this object as an event property map.
	 *
	 * @return the event property map, never {@literal null}
	 */
	public Map<String, Object> toEventProperties() {
		Map<String, Object> m = new HashMap<>(2);
		m.put("timestamp", getTimestamp());
		m.put("aggregationKey", getAggregationKey());
		return m;
	}

	@Override
	public int hashCode() {
		return Objects.hash(aggregation, timeStart);
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof AggregateUpdatedEventInfo other) ) {
			return false;
		}
		return aggregation == other.aggregation && Objects.equals(timeStart, other.timeStart);
	}

	/**
	 * Get the aggregation.
	 *
	 * @return the aggregation
	 */
	@JsonIgnore
	public Aggregation getAggregation() {
		return aggregation;
	}

	/**
	 * Get the aggregation key value.
	 *
	 * @return the aggregation key, never {@literal null}
	 */
	public String getAggregationKey() {
		Aggregation a = getAggregation();
		return (a != null ? a.getKey() : Aggregation.None.getKey());
	}

	/**
	 * Get the aggregate time window starting instant.
	 *
	 * @return the time
	 */
	@JsonIgnore
	public Instant getTimeStart() {
		return timeStart;
	}

	/**
	 * Get the aggregate time window starting instant as milliseconds since the
	 * epoch.
	 *
	 * @return the time
	 */
	public long getTimestamp() {
		final Instant ts = getTimeStart();
		return (ts != null ? ts.toEpochMilli() : 0);
	}

}
