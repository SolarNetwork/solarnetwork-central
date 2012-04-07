/* ==================================================================
 * DaoEventBiz.java - Jun 15, 2011 7:47:30 PM
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

package net.solarnetwork.central.dras.biz.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.dras.biz.EventAdminBiz;
import net.solarnetwork.central.dras.biz.EventBiz;
import net.solarnetwork.central.dras.dao.EffectiveDao;
import net.solarnetwork.central.dras.dao.EventDao;
import net.solarnetwork.central.dras.dao.EventFilter;
import net.solarnetwork.central.dras.dao.UserDao;
import net.solarnetwork.central.dras.domain.Effective;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.ParticipantGroup;
import net.solarnetwork.central.dras.support.MembershipCommand;
import net.solarnetwork.util.ClassUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO-based implementation of {@link EventBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
@Service
public class DaoEventBiz extends DaoBizSupport implements EventBiz, EventAdminBiz {

	private final EventDao eventDao;
	
	/**
	 * Construct with values.
	 * 
	 * @param eventDao the EventDao
	 */
	@Autowired
	public DaoEventBiz(EventDao eventDao, EffectiveDao effectiveDao, UserDao userDao) {
		this.eventDao = eventDao;
		this.effectiveDao = effectiveDao;
		this.userDao = userDao;
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Event getEvent(Long eventId) {
		return eventDao.get(eventId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Match> findEvents(ObjectCriteria<EventFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		FilterResults<Match> matches =  eventDao.findFiltered(
				criteria.getSimpleFilter(), sortDescriptors, 
				criteria.getResultOffset(), criteria.getResultMax());
		List<Match> result = new ArrayList<Match>(matches.getReturnedResultCount().intValue());
		for ( Match m : matches.getResults() ) {
			result.add(m);
		}
		return result;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Event storeEvent(Event template) {
		Event entity;
		if ( template.getId() != null ) {
			entity = eventDao.get(template.getId());
		} else {
			entity = new Event();
			entity.setCreator(getCurrentUserId());
		}
		ClassUtils.copyBeanProperties(template, entity, null);
		if ( entity.getEnabled() == null ) {
			entity.setEnabled(Boolean.TRUE);
		}
		Long id = eventDao.store(entity);
		Event res = eventDao.get(id);
		return res;
	}

	@Override
	public EffectiveCollection<Event, Member> assignMembers(Long eventId,
			MembershipCommand participants, MembershipCommand participantGroups) {
		if ( eventId == null || (participants == null && participantGroups == null) ) {
			return null;
		}
		
		EffectiveCollection<Event, ? extends Member> parts = null;
		if ( participants != null ) {
			// create copies of input to mutate
			MembershipCommand p = (MembershipCommand)participants.clone();
			p.setParentId(eventId);
			
			// assign participants
			parts =  maintainGroupMembership(p, new MembershipMaintenance<Event, Member>() {
	
				@Override
				public GenericDao<Event, Long> getDao() {
					return eventDao;
				}
	
				@Override
				public Member createMember(Long memberId) {
					return new Participant(memberId);
				}
	
				@Override
				public Set<Member> getMembers(Long parentId, Effective eff) {
					return eventDao.getParticipantMembers(parentId, eff.getEffectiveDate());
				}
	
				@Override
				public void assignMembers(Long parentId, Set<Long> newMembers,
						Effective eff) {
					eventDao.assignParticipantMembers(parentId, newMembers, eff.getId());
				}
	
			});
		}
		
		EffectiveCollection<Event, ? extends Member> groups = null;
		if ( participantGroups != null ) {
			MembershipCommand g = (MembershipCommand)participantGroups.clone();
			g.setParentId(eventId);
			// using same Effective as for participants, assign participant groups
			if ( parts != null && parts.getEffective() != null ) {
				g.setEffectiveId(parts.getEffective().getId());
			}
			groups =  maintainGroupMembership(g, new MembershipMaintenance<Event, Member>() {
	
				@Override
				public GenericDao<Event, Long> getDao() {
					return eventDao;
				}
	
				@Override
				public Member createMember(Long memberId) {
					return new ParticipantGroup(memberId);
				}
	
				@Override
				public Set<Member> getMembers(Long parentId, Effective eff) {
					return eventDao.getParticipantGroupMembers(parentId, eff.getEffectiveDate());
				}
	
				@Override
				public void assignMembers(Long parentId, Set<Long> newMembers,
						Effective eff) {
					eventDao.assignParticipantGroupMembers(parentId, newMembers, eff.getId());
				}
	
			});
		}
		
		List<Member> combinedMembers = new ArrayList<Member>(
				(parts == null ? 0 : parts.getCollection().size())
				+(groups == null ? 0 : groups.getCollection().size())
				+1);
		if ( parts != null ) {
			combinedMembers.addAll(parts.getCollection());
		}
		if ( groups != null ) {
			combinedMembers.addAll(groups.getCollection());
		}
		
		EffectiveCollection<Event, Member> result = new EffectiveCollection<Event, Member>(
				parts.getEffective(), parts.getObject(), combinedMembers);
		return result;
	}

}
