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
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.domain.SimplePagination;

/**
 * Basic implementation of {@link CloudDatumStreamQueryFilter}.
 *
 * @author matt
 * @version 1.3
 */
public final class BasicQueryFilter extends SimplePagination
		implements CloudDatumStreamQueryFilter, PaginationCriteria {

	private Instant startDate;
	private Instant endDate;
	private Map<String, ?> parameters;

	/**
	 * Create a query filter for a given date range.
	 *
	 * @param startDate
	 *        the start date
	 * @param endDate
	 *        the end date
	 * @return the filter instance
	 */
	public static BasicQueryFilter ofRange(Instant startDate, Instant endDate) {
		var f = new BasicQueryFilter();
		f.setStartDate(startDate);
		f.setEndDate(endDate);
		return f;
	}

	/**
	 * Constructor.
	 */
	public BasicQueryFilter() {
		super();
	}

	/**
	 * Create a copy of a filter.
	 *
	 * @param filter
	 *        the filter to copy
	 * @return the copy of {@code criteria}
	 */
	public static BasicQueryFilter copyOf(CloudDatumStreamQueryFilter filter) {
		return copyOf(filter, null);
	}

	/**
	 * Create a copy of a filter.
	 *
	 * @param filter
	 *        the filter to copy
	 * @param parameters
	 *        optional parameters to override in the copy
	 * @return the copy of {@code criteria}
	 */
	public static BasicQueryFilter copyOf(CloudDatumStreamQueryFilter filter,
			Map<String, ?> parameters) {
		BasicQueryFilter copy = new BasicQueryFilter();
		if ( filter instanceof BasicQueryFilter f ) {
			copy.setStartDate(f.getStartDate());
			copy.setEndDate(f.getEndDate());
			if ( f.getParameters() != null ) {
				copy.setParameters(new LinkedHashMap<>(f.getParameters()));
			}
		} else {
			// DateRangeCriteria
			copy.setStartDate(filter.getStartDate());
			copy.setEndDate(filter.getEndDate());

			// ParameterCriteria
			copy.setParameters(new LinkedHashMap<>(filter.getParameters()));
		}
		if ( parameters != null ) {
			copy.setParameters(parameters);
		}
		return copy;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicQueryFilter{startDate=");
		builder.append(startDate);
		builder.append(", endDate=");
		builder.append(endDate);
		if ( hasParameterCriteria() ) {
			builder.append(", parameters=");
			builder.append(parameters);
		}
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

	@Override
	public Map<String, ?> getParameters() {
		return parameters;
	}

	/**
	 * Set the parameters.
	 *
	 * @param parameters
	 *        the parameters to set
	 */
	public void setParameters(Map<String, ?> parameters) {
		this.parameters = parameters;
	}

}
