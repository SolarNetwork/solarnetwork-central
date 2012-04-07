/* ==================================================================
 * DaoParticipantBiz.java - Jun 12, 2011 3:06:09 PM
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.dras.biz.ParticipantAdminBiz;
import net.solarnetwork.central.dras.biz.ParticipantBiz;
import net.solarnetwork.central.dras.dao.CapabilityDao;
import net.solarnetwork.central.dras.dao.ConstraintDao;
import net.solarnetwork.central.dras.dao.EffectiveDao;
import net.solarnetwork.central.dras.dao.LocationDao;
import net.solarnetwork.central.dras.dao.ParticipantDao;
import net.solarnetwork.central.dras.dao.ParticipantFilter;
import net.solarnetwork.central.dras.dao.ParticipantGroupDao;
import net.solarnetwork.central.dras.dao.ParticipantGroupFilter;
import net.solarnetwork.central.dras.dao.UserDao;
import net.solarnetwork.central.dras.domain.Capability;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Effective;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.ParticipantGroup;
import net.solarnetwork.central.dras.support.CapableParticipant;
import net.solarnetwork.central.dras.support.CapableParticipantGroup;
import net.solarnetwork.central.dras.support.MembershipCommand;
import net.solarnetwork.util.ClassUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO-based implementation of {@link ParticipantBiz} and {@link ParticipantAdminBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
@Service
public class DaoParticipantBiz extends DaoBizSupport
implements ParticipantBiz, ParticipantAdminBiz {

	private final CapabilityDao capabilityDao;
	private final ParticipantDao participantDao;
	private final ParticipantGroupDao participantGroupDao;
	private final ConstraintDao constraintDao;
	
	/**
	 * Constructor.
	 * 
	 * @param effectiveDao the EffectiveDao
	 * @param userDao the UserDao
	 * @param locationDao the LocationDao
	 * @param participantDao the ParticipantDao
	 * @param participantGroupDao the ParticipantGroupDao
	 * @param constraintDao the ConstraintDao
	 */
	@Autowired
	public DaoParticipantBiz(EffectiveDao effectiveDao, UserDao userDao, 
			CapabilityDao capabilityDao, LocationDao locationDao,
			ParticipantDao participantDao, ParticipantGroupDao participantGroupDao,
			ConstraintDao constraintDao) {
		this.capabilityDao = capabilityDao;
		this.effectiveDao = effectiveDao;
		this.userDao = userDao;
		this.locationDao = locationDao;
		this.participantDao = participantDao;
		this.participantGroupDao = participantGroupDao;
		this.constraintDao = constraintDao;
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Match> findParticipants(
			ObjectCriteria<ParticipantFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		FilterResults<Match> matches = participantDao.findFiltered(
				criteria.getSimpleFilter(), sortDescriptors,
				criteria.getResultOffset(), criteria.getResultMax());
		List<Match> result = new ArrayList<Match>(matches.getReturnedResultCount().intValue());
		for ( Match m : matches.getResults() ) {
			result.add(m);
		}
		return result;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Participant getParticipant(Long participantId) {
		return participantDao.get(participantId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public CapableParticipant getCapableParticipant(Long participantId) {
		Participant p = participantDao.get(participantId);
		if ( p == null ) {
			return null;
		}
		CapableParticipant participant = new CapableParticipant();
		participant.setCapability(p.getCapability());
		participant.setParticipant(p);
		participant.setLocationEntity(locationDao.get(p.getLocationId()));
		return participant;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public CapableParticipantGroup getCapableParticipantGroup(
			Long participantGroupId) {
		ParticipantGroup g = participantGroupDao.get(participantGroupId);
		if ( g == null ) {
			return null;
		}
		CapableParticipantGroup participant = new CapableParticipantGroup();
		participant.setCapability(g.getCapability());
		participant.setParticipantGroup(g);
		participant.setLocationEntity(locationDao.get(g.getLocationId()));
		return participant;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Match> findParticipantGroups(
			ObjectCriteria<ParticipantGroupFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		FilterResults<Match> matches = participantGroupDao.findFiltered(
				criteria.getSimpleFilter(), sortDescriptors,
				criteria.getResultOffset(), criteria.getResultMax());
		List<Match> result = new ArrayList<Match>(matches.getReturnedResultCount().intValue());
		for ( Match m : matches.getResults() ) {
			result.add(m);
		}
		return result;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public ParticipantGroup getParticipantGroup(Long participantGroupId) {
		return participantGroupDao.get(participantGroupId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Participant storeParticipant(Participant template) {
		Participant entity;
		if ( template.getId() != null ) {
			entity = participantDao.get(template.getId());
		} else {
			entity = new Participant();
			entity.setCreator(getCurrentUserId());
		}
		ClassUtils.copyBeanProperties(template, entity, null);
		if ( entity.getEnabled() == null ) {
			entity.setEnabled(Boolean.TRUE);
		}
		Long id = participantDao.store(entity);
		return participantDao.get(id);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Capability storeParticipantCapability(
			Long participantId, Capability template) {
		Participant p = participantDao.get(participantId);
		if ( p == null ) {
			return null;
		}
		Capability current = p.getCapability();
		Set<String> ignore = new HashSet<String>();
		ignore.add("id");
		ignore.add("created");
		if ( current != null ) {
			Map<String, Object> currValues = ClassUtils.getBeanProperties(
					current, ignore);
			Map<String, Object> newValues = ClassUtils.getBeanProperties(template, ignore);
			if ( currValues.equals(newValues) ) {
				// nothing has changed, don't create new mapping
				return current;
			}
		}
		Capability entity = (current == null ? new Capability() : current);
		ClassUtils.copyBeanProperties(template, entity, ignore);
		Long capabilityId = capabilityDao.store(entity);
		p.setCapability(capabilityDao.get(capabilityId));
		participantDao.store(p);
		return p.getCapability();
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public ParticipantGroup storeParticipantGroup(ParticipantGroup template) {
		ParticipantGroup entity;
		if ( template.getId() != null ) {
			entity = participantGroupDao.get(template.getId());
		} else {
			entity = new ParticipantGroup();
			entity.setCreator(getCurrentUserId());
		}
		ClassUtils.copyBeanProperties(template, entity, null);
		if ( entity.getEnabled() == null ) {
			entity.setEnabled(Boolean.TRUE);
		}
		if ( entity.getConfirmed() == null ) {
			entity.setConfirmed(Boolean.FALSE);
		}
		Long id = participantGroupDao.store(entity);
		return participantGroupDao.get(id);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Capability storeParticipantGroupCapability(
			Long participantGroupId, Capability template) {
		ParticipantGroup g = participantGroupDao.get(participantGroupId);
		if ( g == null ) {
			return null;
		}
		Capability current = g.getCapability();
		Set<String> ignore = new HashSet<String>();
		ignore.add("id");
		ignore.add("created");
		if ( current != null ) {
			Map<String, Object> currValues = ClassUtils.getBeanProperties(
					current, ignore);
			Map<String, Object> newValues = ClassUtils.getBeanProperties(template, ignore);
			if ( currValues.equals(newValues) ) {
				// nothing has changed, don't create new mapping
				return current;
			}
		}
		Capability entity = (current == null ? new Capability() : current);
		ClassUtils.copyBeanProperties(template, entity, ignore);
		Long capabilityId = capabilityDao.store(entity);
		entity.setId(capabilityId);
		g.setCapability(entity);
		participantGroupDao.store(g);
		return g.getCapability();
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public EffectiveCollection<ParticipantGroup, Member> assignParticipantGroupMembers(
			MembershipCommand membership) {
		return maintainGroupMembership(membership, new MembershipMaintenance<ParticipantGroup, Member>() {
			@Override
			public GenericDao<ParticipantGroup, Long> getDao() {
				return participantGroupDao;
			}
			
			@Override
			public Participant createMember(Long memberId) {
				return new Participant(memberId);
			}

			@Override
			public Set<Member> getMembers(Long parentId, Effective eff) {
				return participantGroupDao.getParticipantMembers(parentId, eff.getEffectiveDate());
			}
			
			@Override
			public void assignMembers(Long parentId, Set<Long> newMembers, Effective eff) {
				participantGroupDao.assignParticipantMembers(parentId, newMembers, eff.getId());
			}

		});
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public EffectiveCollection<Participant, Constraint> storeParticipantConstraints(
			final Long participantId, final List<Constraint> constraints) {
		List<Long> constraintIds = new ArrayList<Long>(
				constraints == null ? 0 : constraints.size());
		if ( constraints != null ) {
			for ( Constraint c : constraints ) {
				constraintIds.add(constraintDao.store(c));
			}
		}
		MembershipCommand membership = new MembershipCommand();
		membership.setParentId(participantId);
		membership.setGroup(constraintIds);
		return maintainGroupMembership(membership, new MembershipMaintenance<Participant, Constraint>() {

			@Override
			public GenericDao<Participant, Long> getDao() {
				return participantDao;
			}

			@Override
			public Constraint createMember(Long memberId) {
				return new Constraint(memberId);
			}

			@Override
			public Set<Constraint> getMembers(Long parentId, Effective eff) {
				return participantDao.getConstraints(parentId, eff.getEffectiveDate());
			}

			@Override
			public void assignMembers(Long parentId, Set<Long> newMembers,
					Effective eff) {
				participantDao.assignConstraints(parentId, newMembers, eff.getId());
			}
		});
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public EffectiveCollection<Participant, Constraint> storeParticipantProgramConstraints(
			final Long participantId, final Long programId, final List<Constraint> constraints) {
		List<Long> constraintIds = new ArrayList<Long>(
				constraints == null ? 0 : constraints.size());
		if ( constraints != null ) {
			for ( Constraint c : constraints ) {
				constraintIds.add(constraintDao.store(c));
			}
		}
		MembershipCommand membership = new MembershipCommand();
		membership.setParentId(participantId);
		membership.setGroup(constraintIds);
		return maintainGroupMembership(membership, new MembershipMaintenance<Participant, Constraint>() {

			@Override
			public GenericDao<Participant, Long> getDao() {
				return participantDao;
			}

			@Override
			public Constraint createMember(Long memberId) {
				return new Constraint(memberId);
			}

			@Override
			public Set<Constraint> getMembers(Long parentId, Effective eff) {
				return participantDao.getParticipantProgramConstraints(parentId, 
						programId, eff.getEffectiveDate());
			}

			@Override
			public void assignMembers(Long parentId, Set<Long> newMembers,
					Effective eff) {
				participantDao.assignParticipantProgramConstraints(parentId, programId, 
						newMembers, eff.getId());
			}
		});
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Constraint> getParticipantConstraints(Long participantId) {
		return participantDao.getConstraints(participantId, null);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Constraint> getParticipantProgramConstraints(Long participantId,
			Long programId) {
		return participantDao.getParticipantProgramConstraints(participantId, programId, null);
	}

}
