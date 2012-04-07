/* ==================================================================
 * SourceLocationFilter.java - Oct 19, 2011 6:48:11 PM
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

package net.solarnetwork.central.support;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SourceLocation;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Criteria for location data tied to a source.
 * 
 * @author matt
 * @version $Revision$
 */
public class SourceLocationFilter implements Serializable, SourceLocation {

	private static final long serialVersionUID = 2772807008317510048L;

	private String source;
	private SolarLocation location;

	/**
	 * Default constructor.
	 */
	public SourceLocationFilter() {
		this(null, null);
	}
	
	/**
	 * Construct with criteria parameters.
	 * 
	 * @param source the source name
	 * @param locationName the location name
	 */
	public SourceLocationFilter(String source, String locationName) {
		this.source = source;
		this.location = new SolarLocation();
		this.location.setName(locationName);
	}
	
	@Override
	public String toString() {
		return "SourceLocationFilter{source=" +source
				+",location=" +location + "}";
	}

	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		Map<String, Object> filter = new LinkedHashMap<String, Object>();
		if ( source != null ) {
			filter.put("source", source);
		}
		if ( location != null ) {
			filter.putAll(location.getFilter());
		}
		return filter;
	}
	
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public SolarLocation getLocation() {
		return location;
	}
	public void setLocation(SolarLocation location) {
		this.location = location;
	}

}
