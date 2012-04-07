/* ==================================================================
 * CapableObject.java - Jun 14, 2011 12:28:37 PM
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

package net.solarnetwork.central.dras.domain;

import java.io.Serializable;

import net.solarnetwork.central.domain.Identity;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.util.SerializeIgnore;

import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * An object with a related {@link Capability} an 
 * {@net.solarnetwork.central.dras.domain.Location} entity.
 * 
 * @author matt
 * @version $Revision$
 */
public class CapableObject<T extends Identity<Long>> 
implements Serializable, CapabilityInformation {

	private static final long serialVersionUID = 4871881968034752995L;

	private Capability capability;
	private net.solarnetwork.central.dras.domain.Location location;
	private T object;
	
	// Match API
	
	@Override
	public Long getId() {
		return object == null ? null : object.getId();
	}
	
	@Override
	public int compareTo(Long o) {
		return object == null ? 0 : object.compareTo(o);
	}
	
	// CapabilityInformation API
	
	@Override
	@SerializeIgnore
	public String getName() {
		if ( object == null ) {
			return null;
		}
		PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(object);
		if ( bean.isReadableProperty("name") ) {
			Object o = bean.getPropertyValue("name");
			if ( o != null ) {
				return o.toString();
			}
		}
		return null;
	}

	@Override
	public Location getLocation() {
		return location;
	}
	
	@Override
	@SerializeIgnore
	public Long getGenerationCapacityWatts() {
		return capability == null ? null : capability.getGenerationCapacityWatts();
	}

	@Override
	@SerializeIgnore
	public Long getStorageCapacityWattHours() {
		return capability == null ? null : capability.getStorageCapacityWattHours();
	}

	@Override
	@SerializeIgnore
	public Long getShedCapacityWatts() {
		return capability == null ? null : capability.getShedCapacityWatts();
	}

	@Override
	@SerializeIgnore
	public Long getShedCapacityWattHours() {
		return capability == null ? null : capability.getShedCapacityWattHours();
	}

	@Override
	@SerializeIgnore
	public String getDemandResponseKind() {
		return capability == null ? null : capability.getDemandResponseKind();
	}

	@Override
	@SerializeIgnore
	public Long getVarCapacityVoltAmps() {
		return capability == null ? null : capability.getVarCapacityVoltAmps();
	}

	@SerializeIgnore
	public net.solarnetwork.central.dras.domain.Location getLocationEntity() {
		return location;
	}
	public void setLocationEntity(net.solarnetwork.central.dras.domain.Location location) {
		this.location = location;
	}

	@SerializeIgnore
	public T getObject() {
		return object;
	}
	public void setObject(T object) {
		this.object = object;
	}

	public Capability getCapability() {
		return capability;
	}
	public void setCapability(Capability capability) {
		this.capability = capability;
	}
	
}
