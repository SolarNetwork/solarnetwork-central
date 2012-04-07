/* ==================================================================
 * DateTimeWindow.java - Jun 21, 2011 4:58:37 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.domain;

import java.io.Serializable;

import org.joda.time.DateTime;

/**
 * A single date/time window constraint.
 * 
 * <p>These constraints are associated with a specific Constraint as the set
 * of date/time windows for blackout and whitelist windows.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class DateTimeWindow implements Cloneable, Serializable {

	private static final long serialVersionUID = 1746247822157644917L;

	private DateTime startDate;
	private DateTime endDate;
	
	/**
	 * Default constructor.
	 */
	public DateTimeWindow() {
		super();
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param startDate the start date
	 * @param endDate the end date
	 */
	public DateTimeWindow(DateTime startDate, DateTime endDate) {
		super();
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((endDate == null) ? 0 : endDate.hashCode());
		result = prime * result
				+ ((startDate == null) ? 0 : startDate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof DateTimeWindow)) {
			return false;
		}
		DateTimeWindow other = (DateTimeWindow) obj;
		if (endDate == null) {
			if (other.endDate != null) {
				return false;
			}
		} else if (!endDate.equals(other.endDate)) {
			return false;
		}
		if (startDate == null) {
			if (other.startDate != null) {
				return false;
			}
		} else if (!startDate.equals(other.startDate)) {
			return false;
		}
		return true;
	}

	public DateTime getStartDate() {
		return startDate;
	}
	public void setStartDate(DateTime startDate) {
		this.startDate = startDate;
	}
	public DateTime getEndDate() {
		return endDate;
	}
	public void setEndDate(DateTime endDate) {
		this.endDate = endDate;
	}

}
