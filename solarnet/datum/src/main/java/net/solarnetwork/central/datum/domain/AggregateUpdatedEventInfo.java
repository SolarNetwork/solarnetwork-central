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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * {@link DatumAppEvent} properties object for {@link DatumAppEvent} aggregate
 * updates.
 * 
 * @author matt
 * 
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

	private Aggregation aggregation;
	private Instant timeStart;

	/**
	 * Constructor.
	 */
	public AggregateUpdatedEventInfo() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param aggregation
	 *        the aggregation
	 * @param timeStart
	 *        the time start
	 */
	public AggregateUpdatedEventInfo(Aggregation aggregation, Instant timeStart) {
		super();
		this.aggregation = aggregation;
		this.timeStart = timeStart;
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
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof AggregateUpdatedEventInfo) ) {
			return false;
		}
		AggregateUpdatedEventInfo other = (AggregateUpdatedEventInfo) obj;
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
	 * Set the aggregation.
	 * 
	 * @param aggregation
	 *        the aggregation to set
	 */
	public void setAggregation(Aggregation aggregation) {
		this.aggregation = aggregation;
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
	 * Set the aggregation key value.
	 * 
	 * @param key
	 *        the aggregation key to set
	 */
	public void setAggregationKey(String key) {
		Aggregation a;
		try {
			a = Aggregation.forKey(key);
		} catch ( IllegalArgumentException e ) {
			a = Aggregation.None;
		}
		setAggregation(a);
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
	 * Set the aggregate time window starting instant.
	 * 
	 * @param timeStart
	 *        the time to set
	 */
	public void setTimeStart(Instant timeStart) {
		this.timeStart = timeStart;
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

	/**
	 * Set the aggregate time window starting instant as milliseconds since the
	 * epoch.
	 * 
	 * @param ts
	 *        the time
	 */
	public void setTimestamp(long ts) {
		setTimeStart(Instant.ofEpochMilli(ts));
	}

}
