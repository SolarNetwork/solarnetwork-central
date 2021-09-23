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

	private Instant date;
	private Long datumCount;
	private Long datumHourlyCount;
	private Integer datumDailyCount;
	private Integer datumMonthlyCount;

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
	public DatumRecordCounts(Long datumCount, Long datumHourlyCount, Integer datumDailyCount,
			Integer datumMonthlyCount) {
		super();
		this.datumCount = datumCount;
		this.datumHourlyCount = datumHourlyCount;
		this.datumDailyCount = datumDailyCount;
		this.datumMonthlyCount = datumMonthlyCount;
	}

	public Instant getDate() {
		return date;
	}

	public void setDate(Instant date) {
		this.date = date;
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
