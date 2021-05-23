/* ==================================================================
 * AuditDatumEntityRollup.java - 16/11/2020 4:44:55 am
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

package net.solarnetwork.central.datum.v2.dao;

import java.io.Serializable;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Implementation of {@link AuditDatumRollup}.
 * 
 * @author matt
 * @version 1.1
 * @since 2.8
 */
@JsonPropertyOrder({ "ts", "nodeId", "sourceId", "aggregation", "datumTotalCount", "datumCount",
		"datumHourlyCount", "datumDailyCount", "datumMonthlyCount", "datumPropertyPostedCount",
		"datumPropertyRepostedCount", "datumQueryCount" })
@JsonIgnoreProperties({ "id", "streamId" })
public class AuditDatumEntityRollup extends AuditDatumEntity
		implements AuditDatumRollup, Cloneable, Serializable {

	private static final long serialVersionUID = -1564122596119213768L;

	private final Long nodeId;
	private final String sourceId;

	/**
	 * Create a hourly audit datum.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the time
	 * @param datumCount
	 *        the datum count
	 * @param datumPropertyCount
	 *        the datum property count
	 * @param datumQueryCount
	 *        the datum query count
	 * @param datumPropertyUpdatedCount
	 *        the datum property update count
	 * @return the audit datum
	 */
	public static AuditDatumEntityRollup hourlyAuditDatumRollup(Long nodeId, String sourceId,
			Instant timestamp, Long datumCount, Long datumPropertyCount, Long datumQueryCount,
			Long datumPropertyUpdateCount) {
		return new AuditDatumEntityRollup(nodeId, sourceId, timestamp, Aggregation.Hour, datumCount,
				null, null, null, datumPropertyCount, datumQueryCount, datumPropertyUpdateCount);
	}

	/**
	 * Create a daily audit datum.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the time
	 * @param datumCount
	 *        the datum count
	 * @param datumHourlyCount
	 *        the hourly datum count
	 * @param datumDailyCount
	 *        the daily datum count
	 * @param datumPropertyCount
	 *        the datum property count
	 * @param datumQueryCount
	 *        the datum query count
	 * @param datumPropertyUpdatedCount
	 *        the datum property update count
	 * @return the audit datum
	 */
	public static AuditDatumEntityRollup dailyAuditDatumRollup(Long nodeId, String sourceId,
			Instant timestamp, Long datumCount, Long datumHourlyCount, Integer datumDailyCount,
			Long datumPropertyCount, Long datumQueryCount, Long datumPropertyUpdateCount) {
		return new AuditDatumEntityRollup(nodeId, sourceId, timestamp, Aggregation.Day, datumCount,
				datumHourlyCount, datumDailyCount, null, datumPropertyCount, datumQueryCount,
				datumPropertyUpdateCount);
	}

	/**
	 * Create a monthly audit datum.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the time
	 * @param datumCount
	 *        the datum count
	 * @param datumHourlyCount
	 *        the hourly datum count
	 * @param datumDailyCount
	 *        the daily datum count
	 * @param datumMonthlyCount
	 *        the monthly datum count
	 * @param datumPropertyCount
	 *        the datum property count
	 * @param datumQueryCount
	 *        the datum query count
	 * @param datumPropertyUpdatedCount
	 *        the datum property update count
	 * @return the audit datum
	 */
	public static AuditDatumEntityRollup monthlyAuditDatumRollup(Long nodeId, String sourceId,
			Instant timestamp, Long datumCount, Long datumHourlyCount, Integer datumDailyCount,
			Integer datumMonthlyCount, Long datumPropertyCount, Long datumQueryCount,
			Long datumPropertyUpdateCount) {
		return new AuditDatumEntityRollup(nodeId, sourceId, timestamp, Aggregation.Month, datumCount,
				datumHourlyCount, datumDailyCount, datumMonthlyCount, datumPropertyCount,
				datumQueryCount, datumPropertyUpdateCount);
	}

	/**
	 * Create an accumulative "running total" audit datum.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the time
	 * @param datumCount
	 *        the datum count
	 * @param datumHourlyCount
	 *        the hourly datum count
	 * @param datumDailyCount
	 *        the daily datum count
	 * @param datumMonthlyCount
	 *        the monthly datum count
	 * @return the audit datum
	 */
	public static AuditDatumEntityRollup accumulativeAuditDatumRollup(Long nodeId, String sourceId,
			Instant timestamp, Long datumCount, Long datumHourlyCount, Integer datumDailyCount,
			Integer datumMonthlyCount) {
		return new AuditDatumEntityRollup(nodeId, sourceId, timestamp, Aggregation.RunningTotal,
				datumCount, datumHourlyCount, datumDailyCount, datumMonthlyCount, null, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @param aggregation
	 *        the aggregation
	 * @param datumCount
	 *        the datum count
	 * @param datumHourlyCount
	 *        the hourly datum count
	 * @param datumDailyCount
	 *        the daily datum count
	 * @param datumMonthlyCount
	 *        the monthly datum count
	 * @param datumPropertyCount
	 *        the datum property count
	 * @param datumQueryCount
	 *        the datum query count
	 * @param datumPropertyUpdatedCount
	 *        the datum property update count
	 */
	public AuditDatumEntityRollup(Long nodeId, String sourceId, Instant timestamp,
			Aggregation aggregation, Long datumCount, Long datumHourlyCount, Integer datumDailyCount,
			Integer datumMonthlyCount, Long datumPropertyCount, Long datumQueryCount,
			Long datumPropertyUpdateCount) {
		super(null, timestamp, aggregation, datumCount, datumHourlyCount, datumDailyCount,
				datumMonthlyCount, datumPropertyCount, datumQueryCount, datumPropertyUpdateCount);
		this.nodeId = nodeId;
		this.sourceId = sourceId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AuditDatumEntityRollup{");
		builder.append("nodeId=");
		builder.append(nodeId);
		builder.append(", sourceId=");
		builder.append(sourceId);
		builder.append(", ts=");
		builder.append(getTimestamp());
		if ( getAggregation() != null ) {
			builder.append(", kind=");
			builder.append(getAggregation().getKey());
		}
		if ( getDatumCount() != null ) {
			builder.append(", datumCount=");
			builder.append(getDatumCount());
		}
		if ( getDatumHourlyCount() != null ) {
			builder.append(", datumHourlyCount=");
			builder.append(getDatumHourlyCount());
		}
		if ( getDatumDailyCount() != null ) {
			builder.append(", datumDailyCount=");
			builder.append(getDatumDailyCount());
		}
		if ( getDatumMonthlyCount() != null ) {
			builder.append(", datumMonthlyCount=");
			builder.append(getDatumMonthlyCount());
		}
		if ( getDatumPropertyCount() != null ) {
			builder.append(", datumPropertyCount=");
			builder.append(getDatumPropertyCount());
		}
		if ( getDatumPropertyUpdateCount() != null ) {
			builder.append(", datumPropertyUpdateCount=");
			builder.append(getDatumPropertyUpdateCount());
		}
		if ( getDatumQueryCount() != null ) {
			builder.append(", datumQueryCount=");
			builder.append(getDatumQueryCount());
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public AuditDatumEntityRollup clone() {
		return (AuditDatumEntityRollup) super.clone();
	}

	@Override
	public Long getNodeId() {
		return nodeId;
	}

	@Override
	public String getSourceId() {
		return sourceId;
	}

}
