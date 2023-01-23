/* ==================================================================
 * AuditNodeServiceEntity.java - 22/01/2023 12:04:24 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao;

import static net.solarnetwork.domain.datum.Aggregation.Day;
import static net.solarnetwork.domain.datum.Aggregation.Hour;
import static net.solarnetwork.domain.datum.Aggregation.Month;
import static net.solarnetwork.domain.datum.Aggregation.RunningTotal;
import static net.solarnetwork.domain.datum.DatumId.nodeId;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.domain.AuditNodeServiceValue;
import net.solarnetwork.dao.BasicIdentity;
import net.solarnetwork.domain.Differentiable;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumId;

/**
 * Audit node service entity.
 * 
 * @author matt
 * @version 1.0
 */
@JsonPropertyOrder({ "ts", "nodeId", "service", "aggregation", "count" })
@JsonIgnoreProperties("id")
public class AuditNodeServiceEntity extends BasicIdentity<DatumId> implements AuditNodeServiceValue,
		Cloneable, Serializable, Differentiable<AuditNodeServiceValue> {

	private static final long serialVersionUID = 8906783581107973754L;

	private final Aggregation aggregation;
	private final long count;

	/**
	 * Create a hourly audit datum.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param service
	 *        the service name
	 * @param timestamp
	 *        the time
	 * @param count
	 *        the count
	 * @return the audit entity
	 */
	public static AuditNodeServiceEntity hourlyAuditNodeService(Long nodeId, String service,
			Instant timestamp, long count) {
		return new AuditNodeServiceEntity(nodeId(nodeId, service, timestamp), Hour, count);
	}

	/**
	 * Create a daily audit datum.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param service
	 *        the service name
	 * @param timestamp
	 *        the time
	 * @param count
	 *        the count
	 * @return the audit entity
	 */
	public static AuditNodeServiceEntity dailyAuditNodeService(Long nodeId, String service,
			Instant timestamp, long count) {
		return new AuditNodeServiceEntity(nodeId(nodeId, service, timestamp), Day, count);
	}

	/**
	 * Create a monthly audit datum.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param service
	 *        the service name
	 * @param timestamp
	 *        the time
	 * @param count
	 *        the count
	 * @return the audit entity
	 */
	public static AuditNodeServiceEntity monthlyAuditNodeService(Long nodeId, String service,
			Instant timestamp, long count) {
		return new AuditNodeServiceEntity(nodeId(nodeId, service, timestamp), Month, count);
	}

	/**
	 * Create an accumulative "running total" audit datum.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param service
	 *        the service name
	 * @param timestamp
	 *        the time
	 * @param count
	 *        the count
	 * @return the audit entity
	 */
	public static AuditNodeServiceEntity accumulativeAuditNodeService(Long nodeId, String service,
			Instant timestamp, long count) {
		return new AuditNodeServiceEntity(nodeId(nodeId, service, timestamp), RunningTotal, count);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param aggregation
	 *        the aggregation
	 * @param count
	 *        the count
	 */
	public AuditNodeServiceEntity(DatumId id, Aggregation aggregation, long count) {
		super(id);
		this.aggregation = (aggregation == null ? Aggregation.None : aggregation);
		this.count = count;
	}

	/**
	 * Test if the properties of another object are the same as in this
	 * instance.
	 * 
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(AuditNodeServiceValue other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(getId(), other.getId())
				&& Objects.equals(aggregation, other.getAggregation())
				&& Objects.equals(count, other.getCount());
		// @formatter:on
	}

	@Override
	public boolean differsFrom(AuditNodeServiceValue other) {
		return !isSameAs(other);
	}

	@JsonProperty("ts")
	@Override
	public Instant getTimestamp() {
		return AuditNodeServiceValue.super.getTimestamp();
	}

	@Override
	public Aggregation getAggregation() {
		return aggregation;
	}

	@Override
	public long getCount() {
		return count;
	}

}
