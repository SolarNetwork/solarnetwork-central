/* ===================================================================
 * SolarNode.java
 *
 * Created Aug 18, 2008 3:11:29 PM
 *
 * Copyright (c) 2008 SolarNetwork.net Dev Team.
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

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.TimeZone;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.dao.BaseEntity;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * Domain object for node related info.
 *
 * @author matt
 * @version 2.0
 */
public class SolarNode extends BaseEntity implements Cloneable, Serializable, NodeIdentity {

	@Serial
	private static final long serialVersionUID = 531103294940393083L;

	private @Nullable String name;
	private @Nullable Long locationId;

	@SerializeIgnore
	@JsonIgnore
	private @Nullable SolarLocation location;

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
	public SolarNode(@Nullable Long id, @Nullable Long locationId) {
		super();
		setId(id);
		setCreated(Instant.now());
		setLocationId(locationId);
	}

	@Override
	public SolarNode clone() {
		return (SolarNode) super.clone();
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
	public final @Nullable TimeZone getTimeZone() {
		return (this.location != null && this.location.getTimeZoneId() != null
				? TimeZone.getTimeZone(this.location.getTimeZoneId())
				: null);
	}

	public final @Nullable Long getLocationId() {
		return locationId;
	}

	public final void setLocationId(@Nullable Long locationId) {
		this.locationId = locationId;
	}

	@JsonIgnore
	public final @Nullable SolarLocation getLocation() {
		return location;
	}

	public final void setLocation(@Nullable SolarLocation location) {
		this.location = location;
		if ( location != null && location.getId() != null ) {
			this.locationId = location.getId();
		}
	}

	public final @Nullable String getName() {
		return name;
	}

	public final void setName(@Nullable String name) {
		this.name = name;
	}

}
