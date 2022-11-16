/* ==================================================================
 * BasicOcppCriteria.java - 17/11/2022 9:09:41 am
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

package net.solarnetwork.central.ocpp.dao;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class BasicOcppCriteria extends BasicCoreCriteria
		implements ChargePointStatusFilter, ChargePointActionStatusFilter {

	private Long[] chargePointIds;
	private String[] actions;
	private Instant startDate;
	private Instant endDate;

	/**
	 * Copy the properties of another criteria into this instance.
	 * 
	 * <p>
	 * This method will test for conformance to all the various criteria
	 * interfaces implemented by this class, and copy those properties as well.
	 * </p>
	 * 
	 * @param criteria
	 *        the criteria to copy
	 */
	@Override
	public void copyFrom(PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria instanceof BasicOcppCriteria c ) {
			setChargePointIds(c.chargePointIds);
			setActions(c.actions);
			setStartDate(c.startDate);
			setEndDate(c.endDate);
		} else {
			if ( criteria instanceof ChargePointCriteria c ) {
				setChargePointIds(c.getChargePointIds());
			}
			if ( criteria instanceof ActionCriteria c ) {
				setActions(c.getActions());
			}
			if ( criteria instanceof DateRangeCriteria c ) {
				setStartDate(c.getStartDate());
				setEndDate(c.getEndDate());
			}
		}
	}

	/**
	 * Create a copy of a criteria.
	 * 
	 * @param criteria
	 *        the criteria to copy
	 * @return the copy
	 */
	public static BasicOcppCriteria copy(PaginationCriteria criteria) {
		BasicOcppCriteria c = new BasicOcppCriteria();
		c.copyFrom(criteria);
		return c;
	}

	@Override
	public BasicOcppCriteria clone() {
		return (BasicOcppCriteria) super.clone();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(actions);
		result = prime * result + Arrays.hashCode(chargePointIds);
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
		if ( !(obj instanceof BasicOcppCriteria) ) {
			return false;
		}
		BasicOcppCriteria other = (BasicOcppCriteria) obj;
		return Arrays.equals(actions, other.actions)
				&& Arrays.equals(chargePointIds, other.chargePointIds)
				&& Objects.equals(endDate, other.endDate) && Objects.equals(startDate, other.startDate);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicOcppCriteria{");
		if ( getUserIds() != null ) {
			builder.append("userIds=");
			builder.append(Arrays.toString(getUserIds()));
			builder.append(", ");
		}
		if ( chargePointIds != null ) {
			builder.append("chargePointIds=");
			builder.append(Arrays.toString(chargePointIds));
			builder.append(", ");
		}
		if ( actions != null ) {
			builder.append("actions=");
			builder.append(Arrays.toString(actions));
			builder.append(", ");
		}
		if ( startDate != null ) {
			builder.append("startDate=");
			builder.append(startDate);
			builder.append(", ");
		}
		if ( endDate != null ) {
			builder.append("endDate=");
			builder.append(endDate);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Set a single charge point ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single charge point
	 * ID at a time. The charge point ID is still stored on the
	 * {@code chargePointIds} array, just as the first value. Calling this
	 * method replaces any existing {@code chargePointIds} value with a new
	 * array containing just the ID passed into this method.
	 * </p>
	 * 
	 * @param chargePointId
	 *        the ID of the charge point
	 */
	@JsonSetter
	public void setChargePointId(Long chargePointId) {
		this.chargePointIds = (chargePointId == null ? null : new Long[] { chargePointId });
	}

	@Override
	@JsonIgnore
	public Long getChargePointId() {
		return (this.chargePointIds == null || this.chargePointIds.length < 1 ? null
				: this.chargePointIds[0]);
	}

	@Override
	public Long[] getChargePointIds() {
		return chargePointIds;
	}

	/**
	 * Set a list of charge point IDs to filter on.
	 * 
	 * @param chargePointIds
	 *        The charge point IDs to filter on.
	 */
	public void setChargePointIds(Long[] chargePointIds) {
		this.chargePointIds = chargePointIds;
	}

	/**
	 * Set an action.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single action at a
	 * time. The action is still stored on the {@code actions} array, just as
	 * the first value. Calling this method replaces any existing
	 * {@code actions} value with a new array containing just the ID passed into
	 * this method.
	 * </p>
	 * 
	 * @param action
	 *        the action to set
	 */
	@JsonSetter
	public void setAction(String action) {
		this.actions = (action == null ? null : new String[] { action });
	}

	@Override
	@JsonIgnore
	public String getAction() {
		return (this.actions == null || this.actions.length < 1 ? null : this.actions[0]);
	}

	@Override
	public String[] getActions() {
		return actions;
	}

	/**
	 * Set a list of actions to filter on.
	 * 
	 * @param actions
	 *        The actions to filter on.
	 */
	public void setActions(String[] actions) {
		this.actions = actions;
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
