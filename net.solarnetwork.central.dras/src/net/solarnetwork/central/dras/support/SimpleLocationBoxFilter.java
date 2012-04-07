/* ==================================================================
 * SimpleLocationBoxFilter.java - Jun 10, 2011 1:57:27 PM
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

package net.solarnetwork.central.dras.support;

import java.util.Map;

import net.solarnetwork.central.dras.dao.GridLocationFilter;
import net.solarnetwork.central.dras.dao.LocationBoxFilter;
import net.solarnetwork.central.dras.domain.Location;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Implementation of {@link LocationBoxFilter} and {@link GridLocationFilter}.
 * 
 * @author matt
 * @version $Revision$
 */
public class SimpleLocationBoxFilter extends Location
implements LocationBoxFilter, GridLocationFilter {

	private static final long serialVersionUID = -9071275185735360991L;

	private Boolean enabled;
	private Double boxLatitude;
	private Double boxLongitude;
	private Long userId;

	@Override
	public boolean isBox() {
		return getLatitude() != null 
				&& getLongitude() != null
				&& getBoxLatitude() != null
				&& getBoxLongitude() != null;
	}
	
	@Override
	public Double getBoxLatitude() {
		return boxLatitude;
	}
	
	@Override
	public Double getBoxLongitude() {
		return boxLongitude;
	}
	
	@Override
	public final Long getUserId() {
		return userId;
	}

	@Override
	public final void setUserId(Long userId) {
		this.userId = userId;
	}

	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		@SuppressWarnings("unchecked")
		Map<String, Object> filter = (Map<String, Object>)super.getFilter();
		if ( boxLatitude != null ) {
			filter.put("boxLatitude", boxLatitude);
		}
		if ( boxLongitude != null ) {
			filter.put("boxLongitude", boxLongitude);
		}
		if ( enabled != null ) {
			filter.put("enabled", enabled);
		}
		if ( userId != null ) {
			filter.put("userId", userId);
		}
		return filter;
	}

	public void setBoxLatitude(Double boxLatitude) {
		this.boxLatitude = boxLatitude;
	}
	public void setBoxLongitude(Double boxLongitude) {
		this.boxLongitude = boxLongitude;
	}
	public Boolean getEnabled() {
		return enabled;
	}
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

}
