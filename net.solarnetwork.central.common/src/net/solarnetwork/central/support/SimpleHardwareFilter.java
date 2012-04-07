/* ==================================================================
 * SimpleHardwareFilter.java - Sep 29, 2011 2:28:12 PM
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

import java.util.LinkedHashMap;
import java.util.Map;

import net.solarnetwork.central.domain.HardwareFilter;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Simple implementation of {@link HardwareFilter}.
 * 
 * @author matt
 * @version $Revision$
 */
public class SimpleHardwareFilter implements HardwareFilter {

	private Long hardwareId;
	private String name;
	
	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		Map<String, Object> f = new LinkedHashMap<String, Object>(2);
		if ( name != null ) {
			f.put("name", name);
		}
		if ( hardwareId != null ) {
			f.put("hardwareId", hardwareId);
		}
		return f;
	}

	@Override
	public Long getHardwareId() {
		return hardwareId;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setHardwareId(Long hardwareId) {
		this.hardwareId = hardwareId;
	}
	public void setName(String name) {
		this.name = name;
	}

}
