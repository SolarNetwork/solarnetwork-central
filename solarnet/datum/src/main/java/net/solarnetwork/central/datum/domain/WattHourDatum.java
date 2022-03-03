/* ===================================================================
 * WattHourDatum.java
 * 
 * Created Aug 14, 2009 11:22:30 AM
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

/**
 * NodeDatum API for watt-hour related data.
 *
 * @author matt
 * @version 1.0 $Date$
 */
public interface WattHourDatum {

	/**
	 * Get the watt-hour value.
	 * 
	 * @return the wattHours
	 */
	public Double getWattHours();
	
	/**
	 * Get a cost for the watt hours.
	 * 
	 * @return the cost
	 */
	public Double getCost();
	
	/**
	 * Get the currency of the cost for the watt hours.
	 * 
	 * @return the currency
	 */
	public String getCurrency();

}
