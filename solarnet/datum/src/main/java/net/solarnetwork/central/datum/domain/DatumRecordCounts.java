/* ==================================================================
 * DatumRecordCounts.java - 11/07/2018 11:43:34 AM
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

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Counts of datum records for a date range.
 *
 * @author matt
 * @version 2.0
 * @since 1.31
 */
@JsonPropertyOrder({ "date", "datumTotalCount", "datumCount", "datumHourlyCount", "datumDailyCount",
		"datumMonthlyCount" })
public class DatumRecordCounts {

	private @Nullable Instant date;
	private @Nullable Long datumCount;
	private @Nullable Long datumHourlyCount;
	private @Nullable Integer datumDailyCount;
	private @Nullable Integer datumMonthlyCount;

	/**
	 * Default constructor.
	 */
	public DatumRecordCounts() {
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
	public DatumRecordCounts(@Nullable Long datumCount, @Nullable Long datumHourlyCount,
			@Nullable Integer datumDailyCount, @Nullable Integer datumMonthlyCount) {
		super();
		this.datumCount = datumCount;
		this.datumHourlyCount = datumHourlyCount;
		this.datumDailyCount = datumDailyCount;
		this.datumMonthlyCount = datumMonthlyCount;
	}

	public final @Nullable Instant getDate() {
		return date;
	}

	public final void setDate(@Nullable Instant date) {
		this.date = date;
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
