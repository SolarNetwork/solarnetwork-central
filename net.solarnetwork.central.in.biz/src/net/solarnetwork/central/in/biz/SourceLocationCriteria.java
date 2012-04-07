/* ==================================================================
 * SourceLocationCriteria.java - Oct 19, 2011 6:48:11 PM
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

import net.solarnetwork.central.domain.SolarLocation;

/**
 * Criteria for location data tied to a source.
 * 
 * @author matt
 * @version $Revision$
 */
public class SourceLocationCriteria implements Serializable {

	private static final long serialVersionUID = -3643865522563204865L;

	private String sourceName;
	private SolarLocation location;

	/**
	 * Default constructor.
	 */
	public SourceLocationCriteria() {
		this(null, null);
	}
	
	/**
	 * Construct with criteria parameters.
	 * 
	 * @param sourceName the source name
	 * @param locationName the location name
	 */
	public SourceLocationCriteria(String sourceName, String locationName) {
		this.sourceName = sourceName;
		this.location = new SolarLocation();
		this.location.setName(locationName);
	}
	
	@Override
	public String toString() {
		return "SourceLocationCriteria{source=" +sourceName
				+",location=" +location + "}";
	}

	public String getSourceName() {
		return sourceName;
	}
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}
	public SolarLocation getLocation() {
		return location;
	}
	public void setLocation(SolarLocation location) {
		this.location = location;
	}

}
