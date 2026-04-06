/* ==================================================================
 * AuditDatumRecordCounts.java - 11/07/2018 11:43:34 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Unique;

/**
 * Counts of datum records for a date range.
 *
 * <p>
 * Note that {@link GeneralNodeDatumPK} is used as the primary key, but the
 * {@code created} property is serialized into JSON as {@literal ts}.
 * </p>
 *
 * @author matt
 * @version 4.0
 */
@JsonPropertyOrder({ "ts", "nodeId", "sourceId", "processed", "datumTotalCount", "datumCount",
		"datumHourlyCount", "datumDailyCount", "datumMonthlyCount", "datumPropertyPostedCount",
		"datumPostedCount", "datumQueryCount" })
@JsonIgnoreProperties({ "id" })
public final class AuditDatumRecordCounts implements Unique<ObjectRecordId>, Cloneable, Serializable,
		CopyingIdentity<AuditDatumRecordCounts, ObjectRecordId> {

	@Serial
	private static final long serialVersionUID = -1393664537620448888L;

	private final ObjectRecordId id;
	private @Nullable Instant processed;
	private final @Nullable Long datumCount;
	private final @Nullable Long datumHourlyCount;
	private final @Nullable Integer datumDailyCount;
	private final @Nullable Integer datumMonthlyCount;
	private @Nullable Long datumPropertyPostedCount;
	private @Nullable Long datumPostedCount;
	private @Nullable Long datumQueryCount;

	/**
	 * Construct with values.
	 *
	 * @param datumCount
	 *        the datum count
	 * @param datumHourlyCount
	 *        the hourly count
	 * @param datumDailyCount
	 *        the daily count
	 * @param datumMonthlyCount
	 *        the monthly count
	 */
	public AuditDatumRecordCounts(@Nullable Long datumCount, @Nullable Long datumHourlyCount,
			@Nullable Integer datumDailyCount, @Nullable Integer datumMonthlyCount) {
		this(null, null, datumCount, datumHourlyCount, datumDailyCount, datumMonthlyCount);
	}

	/**
	 * Construct with values.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param datumCount
	 *        the datum count
	 * @param datumHourlyCount
	 *        the hourly count
	 * @param datumDailyCount
	 *        the daily count
	 * @param datumMonthlyCount
	 *        the monthly count
	 */
	public AuditDatumRecordCounts(@Nullable Long nodeId, @Nullable String sourceId,
			@Nullable Long datumCount, @Nullable Long datumHourlyCount,
			@Nullable Integer datumDailyCount, @Nullable Integer datumMonthlyCount) {
		super();
		this.id = new ObjectRecordId(nodeId, sourceId, null);
		this.datumCount = datumCount;
		this.datumHourlyCount = datumHourlyCount;
		this.datumDailyCount = datumDailyCount;
		this.datumMonthlyCount = datumMonthlyCount;
	}

	/**
	 * Construct with values.
	 *
	 * @param id
	 *        the ID
	 * @param datumCount
	 *        the datum count
	 * @param datumHourlyCount
	 *        the hourly count
	 * @param datumDailyCount
	 *        the daily count
	 * @param datumMonthlyCount
	 *        the monthly count
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@code null}
	 * @since 4.0
	 */
	public AuditDatumRecordCounts(ObjectRecordId id, @Nullable Long datumCount,
			@Nullable Long datumHourlyCount, @Nullable Integer datumDailyCount,
			@Nullable Integer datumMonthlyCount) {
		super();
		this.id = requireNonNullArgument(id, "id");
		this.datumCount = datumCount;
		this.datumHourlyCount = datumHourlyCount;
		this.datumDailyCount = datumDailyCount;
		this.datumMonthlyCount = datumMonthlyCount;
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 4.0
	 */
	public AuditDatumRecordCounts(ObjectRecordId id) {
		this(id, null, null, null, null);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("DatumRecordCounts{nodeId=").append(getNodeId());
		buf.append(",sourceId=").append(getSourceId());
		buf.append(",ts=").append(getCreated());
		buf.append(",datumTotalCount=").append(getDatumTotalCount());
		buf.append("}");
		return buf.toString();
	}

	@Override
	public AuditDatumRecordCounts clone() {
		try {
			return (AuditDatumRecordCounts) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// should never get here
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof AuditDatumRecordCounts other) ) {
			return false;
		}
		return Objects.equals(id, other.id);
	}

	@Override
	public AuditDatumRecordCounts copyWithId(ObjectRecordId id) {
		AuditDatumRecordCounts copy = new AuditDatumRecordCounts(id, datumCount, datumHourlyCount,
				datumDailyCount, datumMonthlyCount);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(AuditDatumRecordCounts other) {
		other.datumPostedCount = datumPostedCount;
		other.datumPropertyPostedCount = datumPropertyPostedCount;
		other.datumQueryCount = datumQueryCount;
	}

	@Override
	public @Nullable ObjectRecordId getId() {
		return id;
	}

	/**
	 * Get the node ID.
	 *
	 * @return the nodeId
	 */
	public final @Nullable Long getNodeId() {
		return id.getObjectId();
	}

	/**
	 * Get the source ID.
	 *
	 * @return the sourceId
	 */
	public final @Nullable String getSourceId() {
		return id.getSourceId();
	}

	/**
	 * Get the creation date.
	 *
	 * @return the creation date
	 */
	@JsonProperty("ts")
	public final @Nullable Instant getCreated() {
		return id.getTimestamp();
	}

	public final @Nullable Instant getProcessed() {
		return processed;
	}

	public final void setProcessed(@Nullable Instant processed) {
		this.processed = processed;
	}

	/**
	 * Get the sum total of all datum counts.
	 *
	 * @return the sum total of the datum count properties
	 */
	public final long getDatumTotalCount() {
		long t = 0;
		if ( datumCount != null ) {
			t += datumCount;
		}
		if ( datumHourlyCount != null ) {
			t += datumHourlyCount;
		}
		if ( datumDailyCount != null ) {
			t += datumDailyCount;
		}
		if ( datumMonthlyCount != null ) {
			t += datumMonthlyCount;
		}
		return t;
	}

	public final @Nullable Long getDatumPropertyPostedCount() {
		return datumPropertyPostedCount;
	}

	public final void setDatumPropertyPostedCount(@Nullable Long datumPropertyPostedCount) {
		this.datumPropertyPostedCount = datumPropertyPostedCount;
	}

	public final @Nullable Long getDatumPostedCount() {
		return datumPostedCount;
	}

	public final void setDatumPostedCount(@Nullable Long datumPostedCount) {
		this.datumPostedCount = datumPostedCount;
	}

	public final @Nullable Long getDatumQueryCount() {
		return datumQueryCount;
	}

	public final void setDatumQueryCount(@Nullable Long datumQueryCount) {
		this.datumQueryCount = datumQueryCount;
	}

	public final @Nullable Long getDatumCount() {
		return datumCount;
	}

	public final @Nullable Long getDatumHourlyCount() {
		return datumHourlyCount;
	}

	public final @Nullable Integer getDatumDailyCount() {
		return datumDailyCount;
	}

	public final @Nullable Integer getDatumMonthlyCount() {
		return datumMonthlyCount;
	}

}
