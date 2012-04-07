/* ==================================================================
 * SimpleParticipantFilter.java - Jun 10, 2011 5:00:32 PM
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

import net.solarnetwork.central.dras.dao.ParticipantFilter;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Implementation of {@link ParticipantFilter}.
 * 
 * @author matt
 * @version $Revision$
 */
public class SimpleParticipantFilter extends SimpleMemberLocationBoxFilter
implements ParticipantFilter {

	private static final long serialVersionUID = -5841598130655705700L;

	private Boolean includeCapability;

	@Override
	public Boolean getIncludeCapability() {
		return includeCapability;
	}
	
	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		@SuppressWarnings("unchecked")
		Map<String, Object> filter = (Map<String, Object>)super.getFilter();
		if ( includeCapability != null ) {
			filter.put("includeCapability", includeCapability);
		}
		return filter;
	}
	
	public void setIncludeCapability(Boolean includeCapability) {
		this.includeCapability = includeCapability;
	}

}
