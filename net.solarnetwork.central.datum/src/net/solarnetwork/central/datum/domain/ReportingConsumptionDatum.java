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

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

/**
 * Extension of {@link ConsumptionDatum} with some additional properties
 * geared towards reporting.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public class ReportingConsumptionDatum extends ConsumptionDatum implements WattHourDatum, ReportingDatum {

	private static final long serialVersionUID = 4663373431471373770L;

	private Double wattHours;
	private Double cost;
	private String currency;
	private LocalDateTime localDateTime;
	
	@Override
	public String toString() {
		return "ReportingConsumptionDatum{sourceId=" +getSourceId()
		+",watts=" +getWatts()
		+",wattHours=" +getWattHours()
		+",cost=" +getCost()
		+",currency=" +getCurrency()
		+'}';
	}

	/* (non-Javadoc)
	 * @see net.sf.solarnetwork.central.domain.ReportingDatum#getLocalDate()
	 */
	public LocalDate getLocalDate() {
		if ( localDateTime == null ) {
			return null;
		}
		return localDateTime.toLocalDate();
	}

	/* (non-Javadoc)
	 * @see net.sf.solarnetwork.central.domain.ReportingDatum#getLocalTime()
	 */
	public LocalTime getLocalTime() {
		if ( localDateTime == null ) {
			return null;
		}
		return localDateTime.toLocalTime();
	}
	
	/**
	 * @param localDateTime the localDateTime to set
	 */
	public void setLocalDateTime(LocalDateTime localDateTime) {
		this.localDateTime = localDateTime;
	}

	/**
	 * @return the wattHours
	 */
	public Double getWattHours() {
		return wattHours;
	}
	
	/**
	 * @param wattHours the wattHours to set
	 */
	public void setWattHours(Double wattHours) {
		this.wattHours = wattHours;
	}
	
	/**
	 * @return the cost
	 */
	public Double getCost() {
		return cost;
	}
	
	/**
	 * @param cost the cost to set
	 */
	public void setCost(Double cost) {
		this.cost = cost;
	}
	
	/**
	 * @return the currency
	 */
	public String getCurrency() {
		return currency;
	}
	
	/**
	 * @param currency the currency to set
	 */
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	
}
