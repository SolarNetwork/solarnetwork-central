/* ==================================================================
 * SimpleMemberLocationBoxFilter.java - Jun 15, 2011 2:31:58 PM
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
import net.solarnetwork.central.dras.dao.MemberFilter;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Implementation of {@link LocationBoxFilter} and {@link GridLocationFilter},
 * and {@link MemberFilter}.
 * 
 * @author matt
 * @version $Revision$
 */
public class SimpleMemberLocationBoxFilter extends SimpleLocationBoxFilter
implements MemberFilter {

	private static final long serialVersionUID = 8200705635957391580L;

	private Long eventId;
	private Long groupId;
	private Long programId;

	@Override
	public Long getEventId() {
		return eventId;
	}

	@Override
	public Long getProgramId() {
		return programId;
	}

	@Override
	public Long getGroupId() {
		return groupId;
	}

	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		@SuppressWarnings("unchecked")
		Map<String, Object> filter = (Map<String, Object>)super.getFilter();
		if ( eventId != null ) {
			filter.put("eventId", eventId);
		}
		if ( groupId != null ) {
			filter.put("groupId", groupId);
		}
		if ( programId != null ) {
			filter.put("programId", programId);
		}
		return filter;
	}
	
	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}
	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}
	public void setProgramId(Long programId) {
		this.programId = programId;
	}

}
