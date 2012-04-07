/* ==================================================================
 * DaoUserBiz.java - Jun 8, 2011 5:17:00 PM
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.dras.biz.UserAdminBiz;
import net.solarnetwork.central.dras.biz.UserBiz;
import net.solarnetwork.central.dras.dao.ConstraintDao;
import net.solarnetwork.central.dras.dao.EffectiveDao;
import net.solarnetwork.central.dras.dao.ProgramDao;
import net.solarnetwork.central.dras.dao.UserDao;
import net.solarnetwork.central.dras.dao.UserFilter;
import net.solarnetwork.central.dras.dao.UserGroupDao;
import net.solarnetwork.central.dras.dao.UserGroupFilter;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Effective;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.domain.UserGroup;
import net.solarnetwork.central.dras.domain.UserRole;
import net.solarnetwork.central.dras.support.MembershipCommand;
import net.solarnetwork.central.dras.support.SimpleProgramFilter;
import net.solarnetwork.central.dras.support.UserInformation;
import net.solarnetwork.util.ClassUtils;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO-based implementation of {@link UserBiz} and {@link UserAdminBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
@Service
public class DaoUserBiz extends DaoBizSupport implements UserBiz, UserAdminBiz {

	private final ProgramDao programDao;
	private final UserGroupDao userGroupDao;
	private final ConstraintDao constraintDao;
	
	/**
	 * Construct with values.
	 * 
	 * @param effectiveDao the EffectiveDao
	 * @param programDao the ProgramDao
	 * @param userDao the UserDao
	 * @param userGroupDao the UserGroupDao
	 * @param constraintDao the ConstraintDao
	 */
	@Autowired
	public DaoUserBiz(EffectiveDao effectiveDao, ProgramDao programDao, 
			UserDao userDao, UserGroupDao userGroupDao, ConstraintDao constraintDao) {
		this.effectiveDao = effectiveDao;
		this.programDao = programDao;
		this.userDao = userDao;
		this.userGroupDao = userGroupDao;
		this.constraintDao = constraintDao;
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<UserRole> getAllUserRoles() {
		return userDao.getAllUserRoles();
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public User getUser(Long userId) {
		return userDao.get(userId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public UserInformation getUserInfo(Long userId) {
		// TODO move this logic into single DAO call
		
		UserInformation info = new UserInformation();
		info.setUser(userDao.get(userId));
		info.setRoles(userDao.getUserRoles(userId));
		
		SimpleProgramFilter filter = new SimpleProgramFilter();
		filter.setUserId(userId);
		FilterResults<Match> programs = programDao.findFiltered(filter, null, null, null);
		Set<Long> ids = new LinkedHashSet<Long>();
		for ( Match m : programs.getResults() ) {
			ids.add(m.getId());
		}
		info.setPrograms(ids);
		return info;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<UserRole> getUserRoles(Long userId) {
		return userDao.getUserRoles(userId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Match> findUsers(ObjectCriteria<UserFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		FilterResults<Match> matches = userDao.findFiltered(
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
	public UserGroup getUserGroup(Long groupId) {
		return userGroupDao.get(groupId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Match> findUserGroups(
			ObjectCriteria<UserGroupFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		FilterResults<Match> matches = userGroupDao.findFiltered(
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
	public User storeUser(User template, Set<String> roles, Set<Long> programs) {
		User entity;
		if ( template.getId() != null ) {
			entity = userDao.get(template.getId());
		} else {
			entity = new User();
		}
		
		boolean changedPassword = false;
		if ( entity.getPassword() != null && template.getPassword() != null ) {
			String digest = DigestUtils.sha256Hex(template.getPassword());
			if ( digest != entity.getPassword() ) {
				changedPassword = true;
			}
		} else if ( template.getPassword() != null ) {
			// providing a password when wasn't set before... must digest
			changedPassword = true;
		}
		ClassUtils.copyBeanProperties(template, entity, null);
		if ( entity.getEnabled() == null ) {
			entity.setEnabled(Boolean.TRUE);
		}
		if ( changedPassword ) {
			entity.setPassword(DigestUtils.sha256Hex(entity.getPassword()));
		}
		
		Long newUserId = userDao.store(entity);
		userDao.assignUserRoles(newUserId, roles);
		if ( programs != null ) {
			Effective eff = createEffective(null);
			boolean updated = false;
			for ( Long programId : programs ) {
				Set<Member> members = programDao.getUserMembers(programId, null);
				Set<Long> newMembers = new HashSet<Long>(members.size()+1);
				for ( Member m : members ) {
					newMembers.add(m.getId());
				}
				if ( !newMembers.contains(newUserId) ) {
					newMembers.add(newUserId);
					programDao.assignUserMembers(programId, newMembers, eff.getId());
					updated = true;
				}
			}
			if ( !updated ) {
				// we don't need that Effective
				effectiveDao.delete(eff);
			}
		}
		return userDao.get(newUserId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public UserGroup storeUserGroup(UserGroup template) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public EffectiveCollection<UserGroup, Member> assignUserGroupMembers(
			final MembershipCommand membership) {
		return maintainGroupMembership(membership, new MembershipMaintenance<UserGroup, Member>() {
			@Override
			public GenericDao<UserGroup, Long> getDao() {
				return userGroupDao;
			}
			
			@Override
			public Member createMember(Long memberId) {
				return new User(memberId);
			}

			@Override
			public Set<Member> getMembers(Long parentId, Effective eff) {
				return userGroupDao.getMembers(parentId, eff.getEffectiveDate());
			}
			
			@Override
			public void assignMembers(Long parentId, Set<Long> newMembers, Effective eff) {
				userGroupDao.assignMembers(parentId, newMembers, eff.getId());
			}

		});
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public EffectiveCollection<User, Constraint> storeUserConstraints(
			final Long userId, final List<Constraint> constraints) {
		List<Long> constraintIds = new ArrayList<Long>(
				constraints == null ? 0 : constraints.size());
		if ( constraints != null ) {
			for ( Constraint c : constraints ) {
				constraintIds.add(constraintDao.store(c));
			}
		}
		MembershipCommand membership = new MembershipCommand();
		membership.setParentId(userId);
		membership.setGroup(constraintIds);
		return maintainGroupMembership(membership, new MembershipMaintenance<User, Constraint>() {
			
			@Override
			public GenericDao<User, Long> getDao() {
				return userDao;
			}

			@Override
			public Constraint createMember(Long memberId) {
				return new Constraint(memberId);
			}

			@Override
			public Set<Constraint> getMembers(Long parentId, Effective eff) {
				return userDao.getConstraints(parentId, eff.getEffectiveDate());
			}

			@Override
			public void assignMembers(Long parentId, Set<Long> newMembers,
					Effective eff) {
				userDao.assignConstraints(parentId, newMembers, eff.getId());
			}
		});
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public EffectiveCollection<User, Constraint> storeUserProgramConstraints(
			final Long userId, final Long programId, final List<Constraint> constraints) {
		List<Long> constraintIds = new ArrayList<Long>(
				constraints == null ? 0 : constraints.size());
		if ( constraints != null ) {
			for ( Constraint c : constraints ) {
				constraintIds.add(constraintDao.store(c));
			}
		}
		MembershipCommand membership = new MembershipCommand();
		membership.setParentId(userId);
		membership.setGroup(constraintIds);
		return maintainGroupMembership(membership, new MembershipMaintenance<User, Constraint>() {
			
			@Override
			public GenericDao<User, Long> getDao() {
				return userDao;
			}

			@Override
			public Constraint createMember(Long memberId) {
				return new Constraint(memberId);
			}

			@Override
			public Set<Constraint> getMembers(Long parentId, Effective eff) {
				return userDao.getUserProgramConstraints(parentId, programId, eff.getEffectiveDate());
			}

			@Override
			public void assignMembers(Long parentId, Set<Long> newMembers,
					Effective eff) {
				userDao.assignUserProgramConstraints(parentId, programId, newMembers, eff.getId());
			}
		});
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Constraint> getUserConstraints(Long userId) {
		return userDao.getConstraints(userId, null);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Constraint> getUserProgramConstraints(
			Long userId, Long programId) {
		return userDao.getUserProgramConstraints(userId, programId, null);
	}

}
