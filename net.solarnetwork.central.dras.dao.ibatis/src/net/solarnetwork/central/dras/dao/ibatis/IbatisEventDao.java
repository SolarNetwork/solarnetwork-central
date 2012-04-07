/* ==================================================================
 * IbatisEventDao.java - Jun 7, 2011 6:38:01 PM
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

import net.solarnetwork.central.dras.dao.EventDao;
import net.solarnetwork.central.dras.dao.EventFilter;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.EventExecutionTargets;
import net.solarnetwork.central.dras.domain.EventTargets;
import net.solarnetwork.central.dras.domain.Location;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.ParticipantGroup;
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.domain.UserGroup;

import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ibatis implementation of {@link EventDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisEventDao 
extends DrasIbatisFilterableDaoSupport<Event, Match, EventFilter>
implements EventDao {

	/**
	 * Default constructor.
	 */
	public IbatisEventDao() {
		super(Event.class, Match.class);
	}

	@Override
	protected void postProcessFilterProperties(EventFilter filter,
			Map<String, Object> sqlProps) {
		// add flags to the query processor for dynamic logic
		StringBuilder fts = new StringBuilder();
		spaceAppend(filter.getName(), fts);
		if ( fts.length() > 0 ) {
			sqlProps.put("fts", fts.toString());
		}
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Member> resolveUserMembers(Long eventId, DateTime effectiveDate) {
		return getMemberSet(eventId, User.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Member> getUserMembers(Long eventId, DateTime effectiveDate) {
		return getMemberSet(eventId, Member.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignUserMembers(Long eventId, Set<Long> userIdSet, Long effectiveId) {
		storeMemberSet(eventId, Member.class, userIdSet, effectiveId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Member> getUserGroupMembers(Long eventId,
			DateTime effectiveDate) {
		return getMemberSet(eventId, UserGroup.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignUserGroupMembers(Long eventId,
			Set<Long> userGroupIdSet, Long effectiveId) {
		storeMemberSet(eventId, UserGroup.class, userGroupIdSet, effectiveId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Member> getParticipantMembers(Long eventId,
			DateTime effectiveDate) {
		return getMemberSet(eventId, Participant.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignParticipantMembers(Long eventId,
			Set<Long> participantIdSet, Long effectiveId) {
		storeMemberSet(eventId, Participant.class, participantIdSet, effectiveId);
		
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Member> getParticipantGroupMembers(Long eventId,
			DateTime effectiveDate) {
		return getMemberSet(eventId, ParticipantGroup.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignParticipantGroupMembers(Long eventId,
			Set<Long> participantGroupIdSet, Long effectiveId) {
		storeMemberSet(eventId, ParticipantGroup.class, participantGroupIdSet, effectiveId);
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Member> getLocationMembers(Long eventId, DateTime effectiveDate) {
		return getMemberSet(eventId, Location.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignLocationMembers(Long eventId, Set<Long> locationIdSet, Long effectiveId) {
		storeMemberSet(eventId, Location.class, locationIdSet, effectiveId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<EventTargets> getEventTargets(Long eventId, DateTime effectiveDate) {
		return getRelatedSet(eventId, EventTargets.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignEventTargets(Long eventId, Set<Long> targetsIdSet,
			Long effectiveId) {
		storeRelatedSet(eventId, EventTargets.class, targetsIdSet, effectiveId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<EventExecutionTargets> getEventExecutionTargets(Long eventId,
			DateTime effectiveDate) {
		return getRelatedSet(eventId, EventExecutionTargets.class, effectiveDate);
	}

}
