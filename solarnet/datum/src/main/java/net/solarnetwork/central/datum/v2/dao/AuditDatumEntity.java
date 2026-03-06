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

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.DatumRecordCounts;
import net.solarnetwork.domain.BasicIdentity;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Audit datum entity.
 *
 * @author matt
 * @version 1.3
 * @since 2.8
 */
@JsonPropertyOrder({ "ts", "streamId", "aggregation", "datumTotalCount", "datumCount",
		"datumHourlyCount", "datumDailyCount", "datumMonthlyCount", "datumPropertyPostedCount",
		"datumPropertyRepostedCount", "datumQueryCount", "fluxDataInCount" })
@JsonIgnoreProperties({ "id", "timestamp" })
public class AuditDatumEntity extends BasicIdentity<DatumPK>
		implements AuditDatum, Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = 8810831398503917185L;

	private final @Nullable Aggregation aggregation;
	private final @Nullable Long datumCount;
	private final @Nullable Long datumHourlyCount;
	private final @Nullable Integer datumDailyCount;
	private final @Nullable Integer datumMonthlyCount;
	private final @Nullable Long datumPropertyCount;
	private final @Nullable Long datumPropertyUpdateCount;
	private final @Nullable Long datumQueryCount;
	private final @Nullable Long fluxDataInCount;

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
	 * @throws IllegalArgumentException
	 *         if {@code timestamp} is {@code null}
	 */
	public static DatumRecordCounts datumRecordCounts(Instant timestamp, @Nullable Long datumCount,
			@Nullable Long datumHourlyCount, @Nullable Integer datumDailyCount,
			@Nullable Integer datumMonthlyCount) {
		return new AuditDatumEntity(StreamDatum.UNASSIGNED_STREAM_ID, timestamp, null, datumCount,
				datumHourlyCount, datumDailyCount, datumMonthlyCount, null, null, null, null);
	}

	/**
	 * Create an hourly audit datum.
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
	 * @param datumPropertyUpdateCount
	 *        the datum property update count
	 * @param fluxDataInCount
	 *        the SolarFlux data input count
	 * @return the audit datum
	 * @throws IllegalArgumentException
	 *         if {@code streamId} or {@code timestamp} is {@code null}
	 * @since 1.3
	 */
	public static AuditDatumEntity ioAuditDatum(UUID streamId, Instant timestamp,
			@Nullable Long datumCount, @Nullable Long datumPropertyCount, @Nullable Long datumQueryCount,
			@Nullable Long datumPropertyUpdateCount, @Nullable Long fluxDataInCount) {
		return new AuditDatumEntity(streamId, timestamp, Aggregation.Hour, datumCount, null, null, null,
				datumPropertyCount, datumQueryCount, datumPropertyUpdateCount, fluxDataInCount);
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
	 * @param datumPropertyUpdateCount
	 *        the datum property update count
	 * @param fluxDataInCount
	 *        the SolarFlux data input count
	 * @return the audit datum
	 * @throws IllegalArgumentException
	 *         if {@code streamId} or {@code timestamp} is {@code null}
	 * @since 1.3
	 */
	public static AuditDatumEntity dailyAuditDatum(UUID streamId, Instant timestamp,
			@Nullable Long datumCount, @Nullable Long datumHourlyCount,
			@Nullable Integer datumDailyCount, @Nullable Long datumPropertyCount,
			@Nullable Long datumQueryCount, @Nullable Long datumPropertyUpdateCount,
			@Nullable Long fluxDataInCount) {
		return new AuditDatumEntity(streamId, timestamp, Aggregation.Day, datumCount, datumHourlyCount,
				datumDailyCount, null, datumPropertyCount, datumQueryCount, datumPropertyUpdateCount,
				fluxDataInCount);
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
	 * @param datumPropertyUpdateCount
	 *        the datum property update count
	 * @param fluxDataInCount
	 *        the SolarFlux data input count
	 * @return the audit datum
	 * @throws IllegalArgumentException
	 *         if {@code streamId} or {@code timestamp} is {@code null}
	 * @since 1.3
	 */
	public static AuditDatumEntity monthlyAuditDatum(UUID streamId, Instant timestamp,
			@Nullable Long datumCount, @Nullable Long datumHourlyCount,
			@Nullable Integer datumDailyCount, @Nullable Integer datumMonthlyCount,
			@Nullable Long datumPropertyCount, @Nullable Long datumQueryCount,
			@Nullable Long datumPropertyUpdateCount, @Nullable Long fluxDataInCount) {
		return new AuditDatumEntity(streamId, timestamp, Aggregation.Month, datumCount, datumHourlyCount,
				datumDailyCount, datumMonthlyCount, datumPropertyCount, datumQueryCount,
				datumPropertyUpdateCount, fluxDataInCount);
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
	 * @throws IllegalArgumentException
	 *         if {@code streamId} or {@code timestamp} is {@code null}
	 */
	public static AuditDatumEntity accumulativeAuditDatum(UUID streamId, Instant timestamp,
			@Nullable Long datumCount, @Nullable Long datumHourlyCount,
			@Nullable Integer datumDailyCount, @Nullable Integer datumMonthlyCount) {
		return new AuditDatumEntity(streamId, timestamp, Aggregation.RunningTotal, datumCount,
				datumHourlyCount, datumDailyCount, datumMonthlyCount, null, null, null, null);
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
	 * @param datumPropertyUpdateCount
	 *        the datum property update count
	 * @throws IllegalArgumentException
	 *         if {@code streamId} or {@code timestamp} is {@code null}
	 */
	public AuditDatumEntity(UUID streamId, Instant timestamp, Aggregation aggregation,
			@Nullable Long datumCount, @Nullable Long datumHourlyCount,
			@Nullable Integer datumDailyCount, @Nullable Integer datumMonthlyCount,
			@Nullable Long datumPropertyCount, @Nullable Long datumQueryCount,
			@Nullable Long datumPropertyUpdateCount, @Nullable Long fluxDataInCount) {
		super(new DatumPK(streamId, timestamp));
		this.aggregation = aggregation;
		this.datumCount = datumCount;
		this.datumHourlyCount = datumHourlyCount;
		this.datumDailyCount = datumDailyCount;
		this.datumMonthlyCount = datumMonthlyCount;
		this.datumPropertyCount = datumPropertyCount;
		this.datumQueryCount = datumQueryCount;
		this.datumPropertyUpdateCount = datumPropertyUpdateCount;
		this.fluxDataInCount = fluxDataInCount;
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
		if ( datumPropertyUpdateCount != null ) {
			builder.append(", datumPropertyUpdateCount=");
			builder.append(datumPropertyUpdateCount);
		}
		if ( datumQueryCount != null ) {
			builder.append(", datumQueryCount=");
			builder.append(datumQueryCount);
		}
		if ( fluxDataInCount != null ) {
			builder.append(", fluxDataInCount=");
			builder.append(fluxDataInCount);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public AuditDatumEntity clone() {
		return (AuditDatumEntity) super.clone();
	}

	/**
	 * Get the optional timestamp.
	 *
	 * @return the timestamp, or {@code null}
	 */
	public final Instant getTs() {
		Instant ts = getTimestamp();
		return (ts == null || Instant.EPOCH.compareTo(ts) == 0 ? null : ts);
	}

	@Override
	public final @Nullable Aggregation getAggregation() {
		return aggregation;
	}

	@Override
	public final @Nullable Long getDatumCount() {
		return datumCount;
	}

	@Override
	public final @Nullable Long getDatumHourlyCount() {
		return datumHourlyCount;
	}

	@Override
	public final @Nullable Integer getDatumDailyCount() {
		return datumDailyCount;
	}

	@Override
	public final @Nullable Integer getDatumMonthlyCount() {
		return datumMonthlyCount;
	}

	@JsonProperty("datumPropertyPostedCount")
	@Override
	public final @Nullable Long getDatumPropertyCount() {
		return datumPropertyCount;
	}

	@Override
	public final @Nullable Long getDatumQueryCount() {
		return datumQueryCount;
	}

	@JsonProperty("datumPropertyRepostedCount")
	@Override
	public final @Nullable Long getDatumPropertyUpdateCount() {
		return datumPropertyUpdateCount;
	}

	@Override
	public final @Nullable Long getFluxDataInCount() {
		return fluxDataInCount;
	}

}
