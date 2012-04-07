/* ==================================================================
 * IbatisUserGroupDao.java - Jun 5, 2011 3:30:29 PM
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

package net.solarnetwork.central.dras.dao.ibatis;

import java.util.Map;
import java.util.Set;

import net.solarnetwork.central.dras.dao.UserGroupDao;
import net.solarnetwork.central.dras.dao.UserGroupFilter;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.UserGroup;

import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ibatis implementation of {@link UserGroupDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisUserGroupDao 
extends DrasIbatisFilterableDaoSupport<UserGroup, Match, UserGroupFilter>
implements UserGroupDao {

	/**
	 * Default constructor.
	 */
	public IbatisUserGroupDao() {
		super(UserGroup.class, Match.class);
	}

	@Override
	protected void postProcessFilterProperties(UserGroupFilter filter, Map<String, Object> sqlProps) {
		// add flags to the query processor for dynamic logic
		StringBuilder fts = new StringBuilder();
		spaceAppend(filter.getName(), fts);
		if ( fts.length() > 0 ) {
			sqlProps.put("fts", fts.toString());
		}
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Member> getMembers(Long groupId, DateTime effectiveDate) {
		return getMemberSet(groupId, Member.class, effectiveDate);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignMembers(final Long groupId, final Set<Long> userIdSet,
			final Long effectiveId) {
		storeMemberSet(groupId, Member.class, userIdSet, effectiveId);
	}

}
