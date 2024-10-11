/* ==================================================================
 * BasicQueryFilter.java - 9/10/2024 7:07:11 am
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

package net.solarnetwork.central.c2c.domain;

import java.time.Instant;

/**
 * Basic implementation of {@link CloudDatumStreamQueryFilter}.
 *
 * @author matt
 * @version 1.0
 */
public final class BasicQueryFilter implements CloudDatumStreamQueryFilter {

	private Instant startDate;
	private Instant endDate;

	/**
	 * Constructor.
	 */
	public BasicQueryFilter() {
		super();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicQueryFilter{startDate=");
		builder.append(startDate);
		builder.append(", endDate=");
		builder.append(endDate);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public Instant getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date.
	 *
	 * @param startDate
	 *        the date to set
	 */
	public void setStartDate(Instant startDate) {
		this.startDate = startDate;
	}

	@Override
	public Instant getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date.
	 *
	 * @param endDate
	 *        the date to set
	 */
	public void setEndDate(Instant endDate) {
		this.endDate = endDate;
	}

}
