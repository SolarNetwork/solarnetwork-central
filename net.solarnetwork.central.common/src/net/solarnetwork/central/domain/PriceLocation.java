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
 */

package net.solarnetwork.central.domain;

import java.io.Serializable;
import net.solarnetwork.util.SerializeIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Information about a specific price location.
 * 
 * @author matt
 * @version 1.2
 */
public class PriceLocation extends BaseEntity implements Cloneable, Serializable, SourceLocationMatch {

	private static final long serialVersionUID = -5102271666130994934L;

	private String name;
	private String currency;
	private String unit;
	private SolarLocation location;
	private PriceSource source;
	private String sourceData;

	@Override
	public String toString() {
		return "PriceLocation{id=" + getId() + ",name=" + this.name + ",currency=" + this.currency
				+ ",unit=" + this.unit + '}';
	}

	@Override
	public String getSourceName() {
		return (source == null ? null : source.getName());
	}

	@Override
	@SerializeIgnore
	@JsonIgnore
	public Long getLocationId() {
		return location == null ? null : location.getId();
	}

	@Override
	public String getLocationName() {
		return getName();
	}

	@SerializeIgnore
	@JsonIgnore
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	@SerializeIgnore
	@JsonIgnore
	public PriceSource getSource() {
		return source;
	}

	public void setSource(PriceSource source) {
		this.source = source;
	}

	public String getSourceData() {
		return sourceData;
	}

	public void setSourceData(String sourceData) {
		this.sourceData = sourceData;
	}

	public SolarLocation getLocation() {
		return location;
	}

	public void setLocation(SolarLocation location) {
		this.location = location;
	}

}
