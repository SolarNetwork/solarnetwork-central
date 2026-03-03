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

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * Counts of datum records for a date range.
 *
 * <p>
 * Note that {@link GeneralNodeDatumPK} is used as the primary key, but the
 * {@code created} property is serialized into JSON as {@literal ts}.
 * </p>
 *
 * @author matt
 * @version 3.0
 */
@JsonPropertyOrder({ "ts", "nodeId", "sourceId", "processed", "datumTotalCount", "datumCount",
		"datumHourlyCount", "datumDailyCount", "datumMonthlyCount", "datumPropertyPostedCount",
		"datumPostedCount", "datumQueryCount" })
public final class AuditDatumRecordCounts
		implements Entity<GeneralNodeDatumPK>, Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = -1393664537620448888L;

	private final GeneralNodeDatumPK id = new GeneralNodeDatumPK();
	private @Nullable Instant processed;
	private @Nullable Long datumCount;
	private @Nullable Long datumHourlyCount;
	private @Nullable Integer datumDailyCount;
	private @Nullable Integer datumMonthlyCount;
	private @Nullable Long datumPropertyPostedCount;
	private @Nullable Long datumPostedCount;
	private @Nullable Long datumQueryCount;

	/**
	 * Default constructor.
	 */
	public AuditDatumRecordCounts() {
		super();
	}

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
		if ( nodeId != null ) {
			setNodeId(nodeId);
		}
		if ( sourceId != null ) {
			setSourceId(sourceId);
		}
		this.datumCount = datumCount;
		this.datumHourlyCount = datumHourlyCount;
		this.datumDailyCount = datumDailyCount;
		this.datumMonthlyCount = datumMonthlyCount;
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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof AuditDatumRecordCounts other) ) {
			return false;
		}
		return Objects.equals(id, other.id);
	}

	/**
	 * Convenience getter for {@link GeneralNodeDatumPK#getNodeId()}.
	 *
	 * @return the nodeId
	 */
	public final @Nullable Long getNodeId() {
		return id.getNodeId();
	}

	/**
	 * Convenience setter for {@link GeneralNodeDatumPK#setNodeId(Long)}.
	 *
	 * @param nodeId
	 *        the nodeId to set
	 */
	public final void setNodeId(@Nullable Long nodeId) {
		id.setNodeId(nodeId);
	}

	/**
	 * Convenience getter for {@link GeneralNodeDatumPK#getSourceId()}.
	 *
	 * @return the sourceId
	 */
	public final @Nullable String getSourceId() {
		return id.getSourceId();
	}

	/**
	 * Convenience setter for {@link GeneralNodeDatumPK#setSourceId(String)}.
	 *
	 * @param sourceId
	 *        the sourceId to set
	 */
	public final void setSourceId(@Nullable String sourceId) {
		id.setSourceId(sourceId);
	}

	/**
	 * Convenience setter for {@link GeneralNodeDatumPK#setCreated(Instant)}.
	 *
	 * @param created
	 *        the created to set
	 */
	@JsonProperty("ts")
	public final void setCreated(@Nullable Instant created) {
		id.setCreated(created);
	}

	@Override
	@JsonProperty("ts")
	public final @Nullable Instant getCreated() {
		return id.getCreated();
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public final GeneralNodeDatumPK getId() {
		return id;
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

	public final void setDatumCount(@Nullable Long datumCount) {
		this.datumCount = datumCount;
	}

	public final @Nullable Long getDatumHourlyCount() {
		return datumHourlyCount;
	}

	public final void setDatumHourlyCount(@Nullable Long datumHourlyCount) {
		this.datumHourlyCount = datumHourlyCount;
	}

	public final @Nullable Integer getDatumDailyCount() {
		return datumDailyCount;
	}

	public final void setDatumDailyCount(@Nullable Integer datumDailyCount) {
		this.datumDailyCount = datumDailyCount;
	}

	public final @Nullable Integer getDatumMonthlyCount() {
		return datumMonthlyCount;
	}

	public final void setDatumMonthlyCount(@Nullable Integer datumMonthlyCount) {
		this.datumMonthlyCount = datumMonthlyCount;
	}

}
