/* ==================================================================
 * PriceLocationCriteria.java - Feb 20, 2011 8:41:13 PM
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

package net.solarnetwork.central.in.biz;

import java.io.Serializable;

/**
 * Criteria for PriceLocation data.
 * 
 * @author matt
 * @version $Revision$
 */
public class PriceLocationCriteria implements Serializable {

	private static final long serialVersionUID = -3392304383319988856L;

	private String sourceName;
	private String locationName;
	
	/**
	 * Default constructor.
	 */
	public PriceLocationCriteria() {
		super();
	}
	
	/**
	 * Construct with criteria parameters.
	 * 
	 * @param sourceName the source name
	 * @param locationName the location name
	 */
	public PriceLocationCriteria(String sourceName, String locationName) {
		this.sourceName = sourceName;
		this.locationName = locationName;
	}
	
	@Override
	public String toString() {
		return "PriceLocationCriteria{source=" +sourceName
				+",location=" +locationName + "}";
	}

	/**
	 * @return the sourceName
	 */
	public String getSourceName() {
		return sourceName;
	}
	
	/**
	 * @return the locationName
	 */
	public String getLocationName() {
		return locationName;
	}

	/**
	 * @param sourceName the sourceName to set
	 */
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	/**
	 * @param locationName the locationName to set
	 */
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

}
