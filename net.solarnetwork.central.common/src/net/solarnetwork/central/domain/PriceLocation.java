/* ==================================================================
 * PriceLocation.java - Feb 20, 2011 2:26:13 PM
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

package net.solarnetwork.central.domain;

import java.io.Serializable;

/**
 * Information about a specific price location.
 * 
 * @author matt
 * @version $Revision$
 */
public class PriceLocation extends BaseEntity implements Cloneable, Serializable {

	private static final long serialVersionUID = -6212145474510843847L;

	private String name;
	private String currency;
	private String unit;
	private PriceSource source;
	private String sourceData;
	
	@Override
	public String toString() {
		return "PriceLocation{id="+getId() +",name=" +this.name +",currency=" 
			+this.currency +",unit=" +this.unit +'}';
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
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
	
	/**
	 * @return the unit
	 */
	public String getUnit() {
		return unit;
	}
	
	/**
	 * @param unit the unit to set
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}
	
	/**
	 * @return the source
	 */
	public PriceSource getSource() {
		return source;
	}
	
	/**
	 * @param source the source to set
	 */
	public void setSource(PriceSource source) {
		this.source = source;
	}
	
	/**
	 * @return the sourceData
	 */
	public String getSourceData() {
		return sourceData;
	}
	
	/**
	 * @param sourceData the sourceData to set
	 */
	public void setSourceData(String sourceData) {
		this.sourceData = sourceData;
	}

}
