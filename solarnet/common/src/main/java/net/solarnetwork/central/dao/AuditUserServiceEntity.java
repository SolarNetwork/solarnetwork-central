/* ==================================================================
 * AuditUserServiceEntity.java - 29/05/2024 3:50:56 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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
import net.solarnetwork.central.domain.AuditUserServiceValue;
import net.solarnetwork.dao.BasicIdentity;
import net.solarnetwork.domain.Differentiable;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumId;

/**
 * Audit user service entity.
 * 
 * <p>
 * Although {@link DatumId} is used as a primary key for this entity, and
 * {@link DatumId#getKind()} will be set to
 * {@link net.solarnetwork.domain.datum.ObjectDatumKind#Node}, this entity is
 * <b>not</b> node related, and the {@link DatumId#getObjectId()} actually
 * refers to a <b>user</b> entity.
 * 
 * @author matt
 * @version 1.1
 */
@JsonPropertyOrder({ "ts", "userId", "service", "aggregation", "count" })
@JsonIgnoreProperties("id")
public class AuditUserServiceEntity extends BasicIdentity<DatumId> implements AuditUserServiceValue,
		Cloneable, Serializable, Differentiable<AuditUserServiceValue> {

	private static final long serialVersionUID = 8620718545151038544L;

	private final Aggregation aggregation;
	private final long count;

	/**
	 * Create a hourly audit datum.
	 * 
	 * @param userId
	 *        the user ID
	 * @param service
	 *        the service name
	 * @param timestamp
	 *        the time
	 * @param count
	 *        the count
	 * @return the audit entity
	 */
	public static AuditUserServiceEntity hourlyAuditUserService(Long userId, String service,
			Instant timestamp, long count) {
		return new AuditUserServiceEntity(nodeId(userId, service, timestamp), Hour, count);
	}

	/**
	 * Create a daily audit datum.
	 * 
	 * @param userId
	 *        the user ID
	 * @param service
	 *        the service name
	 * @param timestamp
	 *        the time
	 * @param count
	 *        the count
	 * @return the audit entity
	 */
	public static AuditUserServiceEntity dailyAuditUserService(Long userId, String service,
			Instant timestamp, long count) {
		return new AuditUserServiceEntity(nodeId(userId, service, timestamp), Day, count);
	}

	/**
	 * Create a monthly audit datum.
	 * 
	 * @param userId
	 *        the user ID
	 * @param service
	 *        the service name
	 * @param timestamp
	 *        the time
	 * @param count
	 *        the count
	 * @return the audit entity
	 */
	public static AuditUserServiceEntity monthlyAuditUserService(Long userId, String service,
			Instant timestamp, long count) {
		return new AuditUserServiceEntity(nodeId(userId, service, timestamp), Month, count);
	}

	/**
	 * Create an accumulative "running total" audit datum.
	 * 
	 * @param userId
	 *        the user ID
	 * @param service
	 *        the service name
	 * @param timestamp
	 *        the time
	 * @param count
	 *        the count
	 * @return the audit entity
	 */
	public static AuditUserServiceEntity accumulativeAuditUserService(Long userId, String service,
			Instant timestamp, long count) {
		return new AuditUserServiceEntity(nodeId(userId, service, timestamp), RunningTotal, count);
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
	public AuditUserServiceEntity(DatumId id, Aggregation aggregation, long count) {
		super(id);
		this.aggregation = (aggregation == null ? Aggregation.None : aggregation);
		this.count = count;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AuditNodeServiceEntity{aggregation=");
		builder.append(aggregation);
		builder.append(", userId=");
		builder.append(getUserId());
		builder.append(", service=");
		builder.append(getService());
		builder.append(", timestamp=");
		builder.append(getTimestamp());
		builder.append(", count=");
		builder.append(count);
		builder.append("}");
		return builder.toString();
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
	public boolean isSameAs(AuditUserServiceValue other) {
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
	public boolean differsFrom(AuditUserServiceValue other) {
		return !isSameAs(other);
	}

	@JsonProperty("ts")
	@Override
	public Instant getTimestamp() {
		return AuditUserServiceValue.super.getTimestamp();
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
