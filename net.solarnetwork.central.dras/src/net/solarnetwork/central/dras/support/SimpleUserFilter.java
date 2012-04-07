/* ==================================================================
 * SimpleUserFilter.java - Jun 8, 2011 7:30:19 PM
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

import java.util.List;
import java.util.Map;

import net.solarnetwork.central.dras.dao.UserFilter;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Implementation of {@link UserFilter}.
 * 
 * @author matt
 * @version $Revision$
 */
public class SimpleUserFilter extends SimpleLocationBoxFilter implements UserFilter {

	private static final long serialVersionUID = -2134675927716815897L;

	private String uniqueId;
	private String username;
	private List<String> roles;
	private List<Long> userGroups;

	@Override
	public String getUniqueId() {
		return uniqueId;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public List<String> getRoles() {
		return roles;
	}

	@Override
	public List<Long> getUserGroups() {
		return userGroups;
	}

	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		@SuppressWarnings("unchecked")
		Map<String, Object> filter = (Map<String, Object>)super.getFilter();
		if ( uniqueId != null ) {
			filter.put("uniqueId", uniqueId);
		}
		if ( username != null ) {
			filter.put("username", username);
		}
		if ( roles != null && roles.size() > 0 ) {
			filter.put("roles", roles);
		}
		if ( userGroups != null && userGroups.size() > 0 ) {
			filter.put("userGroupIds", userGroups);
		}
		return filter;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
	public void setUserGroups(List<Long> userGroups) {
		this.userGroups = userGroups;
	}

}
