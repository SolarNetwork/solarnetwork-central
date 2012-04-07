/* ==================================================================
 * UserGroupDao.java - Jun 5, 2011 3:24:46 PM
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

package net.solarnetwork.central.dras.dao;

import java.util.Set;

import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.UserGroup;

import org.joda.time.DateTime;

/**
 * DAO API for UserGroup entities.
 * 
 * @author matt
 * @version $Revision$
 */
public interface UserGroupDao extends GenericDao<UserGroup, Long>, 
FilterableDao<Match, Long, UserGroupFilter> {

	/**
	 * Get the set of group members.
	 * 
	 * @param groupId the group ID to get the members of
	 * @param effectiveDate the effective date, or <em>null</em> for
	 * the current date
	 * @return set of User members, never <em>null</em>
	 */
	Set<Member> getMembers(Long groupId, DateTime effectiveDate);
	
	/**
	 * Assign all members of a group.
	 * 
	 * @param groupId the group ID to assign members to
	 * @param userIdSet the set of user IDs that define the group membership
	 * @param effectiveId the effective ID
	 */
	void assignMembers(Long groupId, Set<Long> userIdSet, Long effectiveId);
	
}
