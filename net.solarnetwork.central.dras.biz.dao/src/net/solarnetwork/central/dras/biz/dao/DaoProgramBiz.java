/* ==================================================================
 * DaoProgramBiz.java - Jun 11, 2011 7:12:20 PM
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
import net.solarnetwork.central.dras.biz.ProgramAdminBiz;
import net.solarnetwork.central.dras.biz.ProgramBiz;
import net.solarnetwork.central.dras.dao.ConstraintDao;
import net.solarnetwork.central.dras.dao.EffectiveDao;
import net.solarnetwork.central.dras.dao.ProgramDao;
import net.solarnetwork.central.dras.dao.ProgramFilter;
import net.solarnetwork.central.dras.dao.UserDao;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Effective;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.Program;
import net.solarnetwork.central.dras.support.MembershipCommand;
import net.solarnetwork.util.ClassUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO-based implementation of {@link ProgramBiz} and {@link ProgramAdminBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
@Service
public class DaoProgramBiz extends DaoBizSupport implements ProgramBiz, ProgramAdminBiz {

	private final ProgramDao programDao;
	private final ConstraintDao constraintDao;
	
	/**
	 * Constructor.
	 * 
	 * @param effectiveDao the EffectiveDao
	 * @param programDao the ProgramDao
	 * @param userDao the UserDao
	 * @param constraintDao the ConstraintDao
	 */
	@Autowired
	public DaoProgramBiz(EffectiveDao effectiveDao, ProgramDao programDao, 
			UserDao userDao, ConstraintDao constraintDao) {
		this.effectiveDao = effectiveDao;
		this.programDao = programDao;
		this.userDao = userDao;
		this.constraintDao = constraintDao;
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Match> findPrograms(ObjectCriteria<ProgramFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		FilterResults<Match> matches = programDao.findFiltered(
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
	public Program getProgram(Long programId) {
		return programDao.get(programId);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Program storeProgram(Program template) {
		Program entity;
		if ( template.getId() != null ) {
			entity = programDao.get(template.getId());
		} else {
			entity = new Program();
			entity.setCreator(getCurrentUserId());
		}
		ClassUtils.copyBeanProperties(template, entity, null);
		if ( entity.getEnabled() == null ) {
			entity.setEnabled(Boolean.TRUE);
		}
		if ( entity.getPriority() == null ) {
			entity.setPriority(0);
		}
		Long id = programDao.store(entity);
		Program res = programDao.get(id);
		return res;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public EffectiveCollection<Program, Member> assignParticipantMembers(
			MembershipCommand membership) {
		return maintainGroupMembership(membership, new MembershipMaintenance<Program, Member>() {

			@Override
			public GenericDao<Program, Long> getDao() {
				return programDao;
			}

			@Override
			public Member createMember(Long memberId) {
				return new Participant(memberId);
			}

			@Override
			public Set<Member> getMembers(Long parentId, Effective eff) {
				return programDao.getParticipantMembers(parentId, eff.getEffectiveDate());
			}

			@Override
			public void assignMembers(Long parentId, Set<Long> newMembers,
					Effective eff) {
				programDao.assignParticipantMembers(parentId, newMembers, eff.getId());
			}

		});
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public EffectiveCollection<Program, Constraint> storeProgramConstraints(
			Long programId, List<Constraint> constraints) {
		List<Long> constraintIds = new ArrayList<Long>(
				constraints == null ? 0 : constraints.size());
		if ( constraints != null ) {
			for ( Constraint c : constraints ) {
				constraintIds.add(constraintDao.store(c));
			}
		}
		MembershipCommand membership = new MembershipCommand();
		membership.setParentId(programId);
		membership.setGroup(constraintIds);
		return maintainGroupMembership(membership, new MembershipMaintenance<Program, Constraint>() {

			@Override
			public GenericDao<Program, Long> getDao() {
				return programDao;
			}

			@Override
			public Constraint createMember(Long memberId) {
				return new Constraint(memberId);
			}

			@Override
			public Set<Constraint> getMembers(Long parentId, Effective eff) {
				return programDao.getConstraints(parentId, eff.getEffectiveDate());
			}

			@Override
			public void assignMembers(Long parentId, Set<Long> newMembers,
					Effective eff) {
				programDao.assignConstraints(parentId, newMembers, eff.getId());
			}
		});
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Constraint> getProgramConstraints(Long programId) {
		return programDao.getConstraints(programId, null);
	}

}
