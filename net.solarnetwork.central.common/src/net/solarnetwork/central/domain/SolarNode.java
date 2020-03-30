/* ===================================================================
 * SolarNode.java
 * 
 * Created Aug 18, 2008 3:11:29 PM
 * 
 * Copyright (c) 2008 Solarnetwork.net Dev Team.
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
 */

package net.solarnetwork.central.domain;

import java.io.Serializable;
import java.util.TimeZone;
import org.joda.time.DateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Domain object for node related info.
 * 
 * @author matt
 * @version 2.0
 */
public class SolarNode extends BaseEntity implements Cloneable, Serializable, NodeIdentity {

	private static final long serialVersionUID = 531103294940393083L;

	private String name = null;
	private Long locationId = null; // the location ID

	@SerializeIgnore
	@JsonIgnore
	private SolarLocation location;

	/**
	 * Default constructor.
	 */
	public SolarNode() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param id
	 *        the ID
	 * @param locationId
	 *        the location ID
	 */
	public SolarNode(Long id, Long locationId) {
		super();
		setId(id);
		setCreated(new DateTime());
		setLocationId(locationId);
	}

	@Override
	public String toString() {
		return "SolarNode{id=" + getId() + ",locationId=" + this.locationId + '}';
	}

	/**
	 * Get a {@link TimeZone} instance for this node's
	 * {@link SolarLocation#getTimeZoneId()}.
	 * 
	 * @return the TimeZone
	 */
	public TimeZone getTimeZone() {
		return (this.location != null && this.location.getTimeZoneId() != null
				? TimeZone.getTimeZone(this.location.getTimeZoneId())
				: null);
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	@JsonIgnore
	public SolarLocation getLocation() {
		return location;
	}

	public void setLocation(SolarLocation location) {
		this.location = location;
		if ( location != null && location.getId() != null ) {
			this.locationId = location.getId();
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
