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

import java.io.Serializable;
import java.time.Instant;
import org.joda.time.DateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.domain.Entity;
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
 * @version 2.0
 */
@JsonPropertyOrder({ "ts", "nodeId", "sourceId", "processed", "datumTotalCount", "datumCount",
		"datumHourlyCount", "datumDailyCount", "datumMonthlyCount", "datumPropertyPostedCount",
		"datumPostedCount", "datumQueryCount" })
public class AuditDatumRecordCounts implements Entity<GeneralNodeDatumPK>, Cloneable, Serializable {

	private static final long serialVersionUID = -1393664537620448888L;

	private GeneralNodeDatumPK id = new GeneralNodeDatumPK();
	private Instant processed;
	private Long datumCount;
	private Long datumHourlyCount;
	private Integer datumDailyCount;
	private Integer datumMonthlyCount;
	private Long datumPropertyPostedCount;
	private Long datumPostedCount;
	private Long datumQueryCount;

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
	public AuditDatumRecordCounts(Long datumCount, Long datumHourlyCount, Integer datumDailyCount,
			Integer datumMonthlyCount) {
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
	public AuditDatumRecordCounts(Long nodeId, String sourceId, Long datumCount, Long datumHourlyCount,
			Integer datumDailyCount, Integer datumMonthlyCount) {
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
	public Object clone() {
		try {
			return super.clone();
		} catch ( CloneNotSupportedException e ) {
			// should never get here
			throw new RuntimeException(e);
		}
	}

	@Override
	public int compareTo(GeneralNodeDatumPK o) {
		if ( id == null && o == null ) {
			return 0;
		}
		if ( id == null ) {
			return -1;
		}
		if ( o == null ) {
			return 1;
		}
		return id.compareTo(o);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		AuditDatumRecordCounts other = (AuditDatumRecordCounts) obj;
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		} else if ( !id.equals(other.id) ) {
			return false;
		}
		return true;
	}

	/**
	 * Convenience getter for {@link GeneralNodeDatumPK#getNodeId()}.
	 * 
	 * @return the nodeId
	 */
	public Long getNodeId() {
		return (id == null ? null : id.getNodeId());
	}

	/**
	 * Convenience setter for {@link GeneralNodeDatumPK#setNodeId(Long)}.
	 * 
	 * @param nodeId
	 *        the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		if ( id == null ) {
			id = new GeneralNodeDatumPK();
		}
		id.setNodeId(nodeId);
	}

	/**
	 * Convenience getter for {@link GeneralNodeDatumPK#getSourceId()}.
	 * 
	 * @return the sourceId
	 */
	public String getSourceId() {
		return (id == null ? null : id.getSourceId());
	}

	/**
	 * Convenience setter for {@link GeneralNodeDatumPK#setSourceId(String)}.
	 * 
	 * @param sourceId
	 *        the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		if ( id == null ) {
			id = new GeneralNodeDatumPK();
		}
		id.setSourceId(sourceId);
	}

	/**
	 * Convenience setter for {@link GeneralNodeDatumPK#setCreated(DateTime)}.
	 * 
	 * @param created
	 *        the created to set
	 */
	@JsonProperty("ts")
	public void setCreated(Instant created) {
		if ( id == null ) {
			id = new GeneralNodeDatumPK();
		}
		id.setCreated(created);
	}

	@Override
	@JsonProperty("ts")
	public Instant getCreated() {
		return (id == null ? null : id.getCreated());
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public GeneralNodeDatumPK getId() {
		return id;
	}

	public Instant getProcessed() {
		return processed;
	}

	public void setProcessed(Instant processed) {
		this.processed = processed;
	}

	/**
	 * Get the sum total of all datum counts.
	 * 
	 * @return the sum total of the datum count properties
	 */
	public long getDatumTotalCount() {
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

	public Long getDatumPropertyPostedCount() {
		return datumPropertyPostedCount;
	}

	public void setDatumPropertyPostedCount(Long datumPropertyPostedCount) {
		this.datumPropertyPostedCount = datumPropertyPostedCount;
	}

	public Long getDatumPostedCount() {
		return datumPostedCount;
	}

	public void setDatumPostedCount(Long datumPostedCount) {
		this.datumPostedCount = datumPostedCount;
	}

	public Long getDatumQueryCount() {
		return datumQueryCount;
	}

	public void setDatumQueryCount(Long datumQueryCount) {
		this.datumQueryCount = datumQueryCount;
	}

	public Long getDatumCount() {
		return datumCount;
	}

	public void setDatumCount(Long datumCount) {
		this.datumCount = datumCount;
	}

	public Long getDatumHourlyCount() {
		return datumHourlyCount;
	}

	public void setDatumHourlyCount(Long datumHourlyCount) {
		this.datumHourlyCount = datumHourlyCount;
	}

	public Integer getDatumDailyCount() {
		return datumDailyCount;
	}

	public void setDatumDailyCount(Integer datumDailyCount) {
		this.datumDailyCount = datumDailyCount;
	}

	public Integer getDatumMonthlyCount() {
		return datumMonthlyCount;
	}

	public void setDatumMonthlyCount(Integer datumMonthlyCount) {
		this.datumMonthlyCount = datumMonthlyCount;
	}

}
