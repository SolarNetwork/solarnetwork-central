/* ==================================================================
 * AuditDatumEntity.java - 8/11/2020 5:24:18 pm
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
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.DatumRecordCounts;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.BasicIdentity;

/**
 * Audit datum entity.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
@JsonPropertyOrder({ "ts", "streamId", "aggregation", "datumTotalCount", "datumCount",
		"datumHourlyCount", "datumDailyCount", "datumMonthlyCount", "datumPropertyPostedCount",
		"datumQueryCount" })
@JsonIgnoreProperties("id")
public class AuditDatumEntity extends BasicIdentity<DatumPK>
		implements AuditDatum, Cloneable, Serializable {

	private static final long serialVersionUID = 6055080475540292240L;

	private final Aggregation aggregation;
	private final Long datumCount;
	private final Long datumHourlyCount;
	private final Integer datumDailyCount;
	private final Integer datumMonthlyCount;
	private final Long datumPropertyCount;
	private final Long datumQueryCount;

	/**
	 * Create a datum record counts instance.
	 * 
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
	 * @return the datum record counts
	 */
	public static DatumRecordCounts datumRecordCounts(Instant timestamp, Long datumCount,
			Long datumHourlyCount, Integer datumDailyCount, Integer datumMonthlyCount) {
		return new AuditDatumEntity(null, timestamp, null, datumCount, datumHourlyCount, datumDailyCount,
				datumMonthlyCount, null, null);
	}

	/**
	 * Create a hourly audit datum.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the time
	 * @param datumCount
	 *        the datum count
	 * @param datumPropertyCount
	 *        the datum property count
	 * @param datumQueryCount
	 *        the datum query count
	 * @return the audit datum
	 */
	public static AuditDatumEntity hourlyAuditDatum(UUID streamId, Instant timestamp, Long datumCount,
			Long datumPropertyCount, Long datumQueryCount) {
		return new AuditDatumEntity(streamId, timestamp, Aggregation.Hour, datumCount, null, null, null,
				datumPropertyCount, datumQueryCount);
	}

	/**
	 * Create a daily audit datum.
	 * 
	 * @param streamId
	 *        the stream ID
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
	 * @return the audit datum
	 */
	public static AuditDatumEntity dailyAuditDatum(UUID streamId, Instant timestamp, Long datumCount,
			Long datumHourlyCount, Integer datumDailyCount, Long datumPropertyCount,
			Long datumQueryCount) {
		return new AuditDatumEntity(streamId, timestamp, Aggregation.Day, datumCount, datumHourlyCount,
				datumDailyCount, null, datumPropertyCount, datumQueryCount);
	}

	/**
	 * Create a monthly audit datum.
	 * 
	 * @param streamId
	 *        the stream ID
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
	 * @return the audit datum
	 */
	public static AuditDatumEntity monthlyAuditDatum(UUID streamId, Instant timestamp, Long datumCount,
			Long datumHourlyCount, Integer datumDailyCount, Integer datumMonthlyCount,
			Long datumPropertyCount, Long datumQueryCount) {
		return new AuditDatumEntity(streamId, timestamp, Aggregation.Month, datumCount, datumHourlyCount,
				datumDailyCount, datumMonthlyCount, datumPropertyCount, datumQueryCount);
	}

	/**
	 * Create an accumulative "running total" audit datum.
	 * 
	 * @param streamId
	 *        the stream ID
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
	public static AuditDatumEntity accumulativeAuditDatum(UUID streamId, Instant timestamp,
			Long datumCount, Long datumHourlyCount, Integer datumDailyCount, Integer datumMonthlyCount) {
		return new AuditDatumEntity(streamId, timestamp, Aggregation.RunningTotal, datumCount,
				datumHourlyCount, datumDailyCount, datumMonthlyCount, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param streamId
	 *        the stream ID
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
	 */
	public AuditDatumEntity(UUID streamId, Instant timestamp, Aggregation aggregation, Long datumCount,
			Long datumHourlyCount, Integer datumDailyCount, Integer datumMonthlyCount,
			Long datumPropertyCount, Long datumQueryCount) {
		super(new DatumPK(streamId, timestamp));
		this.aggregation = aggregation;
		this.datumCount = datumCount;
		this.datumHourlyCount = datumHourlyCount;
		this.datumDailyCount = datumDailyCount;
		this.datumMonthlyCount = datumMonthlyCount;
		this.datumPropertyCount = datumPropertyCount;
		this.datumQueryCount = datumQueryCount;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if ( getStreamId() != null ) {
			builder.append("AuditDatumEntity{");
			builder.append("streamId=");
			builder.append(getStreamId());
		} else {
			builder.append("DatumRecordCounts{");
		}
		if ( getTimestamp() != null ) {
			if ( getStreamId() != null ) {
				builder.append(", ");
			}
			builder.append("ts=");
			builder.append(getTimestamp());
		}
		if ( aggregation != null ) {
			builder.append(", kind=");
			builder.append(aggregation.getKey());
		}
		if ( datumCount != null ) {
			builder.append(", datumCount=");
			builder.append(datumCount);
		}
		if ( datumHourlyCount != null ) {
			builder.append(", datumHourlyCount=");
			builder.append(datumHourlyCount);
		}
		if ( datumDailyCount != null ) {
			builder.append(", datumDailyCount=");
			builder.append(datumDailyCount);
		}
		if ( datumMonthlyCount != null ) {
			builder.append(", datumMonthlyCount=");
			builder.append(datumMonthlyCount);
		}
		if ( datumPropertyCount != null ) {
			builder.append(", datumPropertyCount=");
			builder.append(datumPropertyCount);
		}
		if ( datumQueryCount != null ) {
			builder.append(", datumQueryCount=");
			builder.append(datumQueryCount);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public AuditDatumEntity clone() {
		return (AuditDatumEntity) super.clone();
	}

	@Override
	public UUID getStreamId() {
		DatumPK id = getId();
		return (id != null ? id.getStreamId() : null);
	}

	@JsonProperty("ts")
	@Override
	public Instant getTimestamp() {
		DatumPK id = getId();
		return (id != null ? id.getTimestamp() : null);
	}

	@Override
	public Aggregation getAggregation() {
		return aggregation;
	}

	@Override
	public Long getDatumCount() {
		return datumCount;
	}

	@Override
	public Long getDatumHourlyCount() {
		return datumHourlyCount;
	}

	@Override
	public Integer getDatumDailyCount() {
		return datumDailyCount;
	}

	@Override
	public Integer getDatumMonthlyCount() {
		return datumMonthlyCount;
	}

	@JsonProperty("datumPropertyPostedCount")
	@Override
	public Long getDatumPropertyCount() {
		return datumPropertyCount;
	}

	@Override
	public Long getDatumQueryCount() {
		return datumQueryCount;
	}

}
