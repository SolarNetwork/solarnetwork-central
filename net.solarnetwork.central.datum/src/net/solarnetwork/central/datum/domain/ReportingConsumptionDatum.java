/* ===================================================================
 * ReportingConsumptionDatum.java
 * 
 * Created Aug 11, 2009 3:54:43 PM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.central.datum.domain;

import net.solarnetwork.central.domain.EntityMatch;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

/**
 * Extension of {@link ConsumptionDatum} with some additional properties geared
 * towards reporting.
 * 
 * @author matt
 * @version 1.1
 */
public class ReportingConsumptionDatum extends ConsumptionDatum implements WattHourDatum,
		ReportingDatum, EntityMatch {

	private static final long serialVersionUID = -6376812878462350574L;

	private Double wattHours;
	private Double cost;
	private String currency;
	private LocalDateTime localDateTime;

	@Override
	public String toString() {
		return "ReportingConsumptionDatum{sourceId=" + getSourceId() + ",watts=" + getWatts()
				+ ",wattHours=" + getWattHours() + ",cost=" + getCost() + ",currency=" + getCurrency()
				+ '}';
	}

	@Override
	public LocalDate getLocalDate() {
		if ( localDateTime == null ) {
			return null;
		}
		return localDateTime.toLocalDate();
	}

	@Override
	public LocalTime getLocalTime() {
		if ( localDateTime == null ) {
			return null;
		}
		return localDateTime.toLocalTime();
	}

	public void setLocalDateTime(LocalDateTime localDateTime) {
		this.localDateTime = localDateTime;
	}

	@Override
	public Double getWattHours() {
		return wattHours;
	}

	public void setWattHours(Double wattHours) {
		this.wattHours = wattHours;
	}

	@Override
	public Double getCost() {
		return cost;
	}

	public void setCost(Double cost) {
		this.cost = cost;
	}

	@Override
	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

}
