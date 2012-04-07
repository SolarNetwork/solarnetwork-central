/* ==================================================================
 * IbatisUserDao.java - Jun 3, 2011 3:36:52 PM
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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.solarnetwork.central.dras.dao.ProgramDao;
import net.solarnetwork.central.dras.dao.UserDao;
import net.solarnetwork.central.dras.dao.UserFilter;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Fee;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.domain.UserContact;
import net.solarnetwork.central.dras.domain.UserRole;

import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ibatis.sqlmap.client.event.RowHandler;

/**
 * Ibatis implementation of {@link ProgramDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisUserDao extends DrasIbatisFilterableDaoSupport<User, Match, UserFilter>
implements UserDao {

	/**
	 * Default constructor.
	 */
	public IbatisUserDao() {
		super(User.class, Match.class);
	}
	
	@Override
	protected void postProcessFilterProperties(UserFilter filter, Map<String, Object> sqlProps) {
		// add flags to the query processor for dynamic logic
		boolean haveJoin = false;
		boolean userGroupJoin = false;
		if ( filter.getRoles()  != null && filter.getRoles().size() > 0 ){
			haveJoin = true;
		}
		if ( filter.getUserGroups() != null && filter.getUserGroups().size() > 0 ) {
			haveJoin = true;
			userGroupJoin = true;
		}
		if ( filter.isBox() ) {
			haveJoin = true;
			userGroupJoin = true;
		}
		StringBuilder fts = new StringBuilder();
		spaceAppend(filter.getUsername(), fts);
		if ( fts.length() > 0 ) {
			sqlProps.put("fts", fts.toString());
		}
		if ( userGroupJoin ) {
			sqlProps.put("hasUserGroupJoin", Boolean.TRUE);
		}
		if ( haveJoin ) {
			sqlProps.put("hasJoin", Boolean.TRUE);
		}
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<UserRole> getAllUserRoles() {
		final Set<UserRole> roles = new LinkedHashSet<UserRole>(10);
		getSqlMapClientTemplate().queryWithRowHandler("findall-UserRole", new RowHandler() {
			@Override
			public void handleRow(Object valueObject) {
				roles.add((UserRole)valueObject);
			}
		});
		return roles;
	}

	@Override
	protected Long handleUpdate(User datum) {
		Long result = super.handleUpdate(datum);
		handleRelation(result, datum.getContactInfo(), UserContact.class, null);
		return result;
	}

	@Override
	protected Long handleInsert(User datum) {
		Long result = super.handleInsert(datum);
		handleRelation(result, datum.getContactInfo(), UserContact.class, null);
		return result;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<UserRole> getUserRoles(Long userId) {
		return getRelatedSet(userId, UserRole.class, null);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignUserRoles(Long userId, Set<String> roles) {
		storeRelatedSet(userId, UserRole.class, roles, null);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Constraint> getConstraints(Long userId, DateTime effectiveDate) {
		return getRelatedSet(userId, Constraint.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignConstraints(Long userId, Set<Long> constraintIdSet,
			Long effectiveId) {
		storeRelatedSet(userId, Constraint.class, constraintIdSet, effectiveId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Constraint> getUserProgramConstraints(Long userId,
			Long programId, DateTime effectiveDate) {
		Map<String, Object> props = new HashMap<String, Object>(1);
		props.put("programId", programId);
		return getRelatedSet(userId, Constraint.class, effectiveDate, props);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignUserProgramConstraints(Long userId, Long programId,
			Set<Long> constraintIdSet, Long effectiveId) {
		Map<String, Object> props = new HashMap<String, Object>(1);
		props.put("programId", programId);
		storeRelatedSet(userId, Constraint.class, constraintIdSet, effectiveId, props);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Fee getFee(Long userId, DateTime effectiveDate) {
		return getRelated(userId, effectiveDate, Fee.class);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void setFee(Long userId, Long feeId, Long effectiveId) {
		setRelated(userId, feeId, effectiveId, Fee.class);
	}

}
