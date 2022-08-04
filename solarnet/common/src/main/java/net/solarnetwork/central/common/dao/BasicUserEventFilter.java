/* ==================================================================
 * BasicUserEventFilter.java - 3/08/2022 9:23:31 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.common.dao.UserEventMaintenanceDao.UserEventPurgeFilter;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of {@link UserEventFilter}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicUserEventFilter extends BasicCoreCriteria
		implements UserEventFilter, UserEventPurgeFilter {

	private String[] tags;
	private Instant startDate;
	private Instant endDate;

	/**
	 * Constructor.
	 */
	public BasicUserEventFilter() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param criteria
	 *        the criteria to copy
	 */
	public BasicUserEventFilter(PaginationCriteria criteria) {
		super(criteria);
	}

	@Override
	public void copyFrom(PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria instanceof BasicUserEventFilter ) {
			BasicUserEventFilter c = (BasicUserEventFilter) criteria;
			setTags(c.getTags());
			setStartDate(c.getStartDate());
			setEndDate(c.getEndDate());
		} else {
			if ( criteria instanceof TagCriteria ) {
				setTags(((TagCriteria) criteria).getTags());
			}
			if ( criteria instanceof DateRangeCriteria ) {
				DateRangeCriteria c = (DateRangeCriteria) criteria;
				setStartDate(c.getStartDate());
				setEndDate(c.getEndDate());
			}
		}
	}

	@Override
	public BasicUserEventFilter clone() {
		return (BasicUserEventFilter) super.clone();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(tags);
		result = prime * result + Objects.hash(endDate, startDate);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof BasicUserEventFilter) ) {
			return false;
		}
		BasicUserEventFilter other = (BasicUserEventFilter) obj;
		return Objects.equals(endDate, other.endDate) && Arrays.equals(tags, other.tags)
				&& Objects.equals(startDate, other.startDate);
	}

	@Override
	public String[] getTags() {
		return tags;
	}

	/**
	 * Set the tags.
	 * 
	 * @param tags
	 *        the tags to set
	 */
	public void setTags(String[] tags) {
		this.tags = tags;
	}

	@Override
	@JsonIgnore
	public String getTag() {
		return UserEventFilter.super.getTag();
	}

	/**
	 * Set a single tag.
	 * 
	 * @param tag
	 *        the tag to set
	 */
	@JsonSetter
	public void setTag(String kind) {
		setTags(kind != null ? new String[] { kind } : null);
	}

	@Override
	public Instant getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date.
	 * 
	 * @param startDate
	 *        the start date to set
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
	 *        the end date to set
	 */
	public void setEndDate(Instant endDate) {
		this.endDate = endDate;
	}

}
