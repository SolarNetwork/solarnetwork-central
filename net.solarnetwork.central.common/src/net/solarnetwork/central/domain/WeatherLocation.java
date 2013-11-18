/* ==================================================================
 * WeatherLocation.java - Oct 19, 2011 7:01:41 PM
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
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Information about a specific weather location.
 * 
 * @author matt
 * @version 1.1
 */
public class WeatherLocation extends BaseEntity implements Cloneable, Serializable, SourceLocationMatch {

	private static final long serialVersionUID = -3594930550501316172L;

	private SolarLocation location;
	private WeatherSource source;
	private String sourceData;

	@Override
	public String toString() {
		return "WeatherLocation{id=" + getId() + ",source=" + this.source + ",location=" + this.location
				+ '}';
	}

	@Override
	@SerializeIgnore
	@JsonIgnore
	public String getSourceName() {
		return source == null ? null : source.getName();
	}

	@Override
	@SerializeIgnore
	@JsonIgnore
	public Long getLocationId() {
		return location == null ? null : location.getId();
	}

	@Override
	@SerializeIgnore
	@JsonIgnore
	public String getLocationName() {
		return location == null ? null : location.getName();
	}

	public SolarLocation getLocation() {
		return location;
	}

	public void setLocation(SolarLocation location) {
		this.location = location;
	}

	public WeatherSource getSource() {
		return source;
	}

	public void setSource(WeatherSource source) {
		this.source = source;
	}

	public String getSourceData() {
		return sourceData;
	}

	public void setSourceData(String sourceData) {
		this.sourceData = sourceData;
	}

}
