/* ==================================================================
 * IbatisProgramDao.java - Jun 3, 2011 3:36:52 PM
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

import net.solarnetwork.central.dras.dao.ProgramDao;
import net.solarnetwork.central.dras.dao.ProgramFilter;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.EventRule;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.Program;
import net.solarnetwork.central.dras.domain.User;

import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ibatis implementation of {@link ProgramDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisProgramDao extends DrasIbatisFilterableDaoSupport<Program, Match, ProgramFilter>
implements ProgramDao {

	/**
	 * Default constructor.
	 */
	public IbatisProgramDao() {
		super(Program.class, Match.class);
	}

	@Override
	protected void postProcessFilterProperties(ProgramFilter filter, Map<String, Object> sqlProps) {
		// add flags to the query processor for dynamic logic
		StringBuilder fts = new StringBuilder();
		spaceAppend(filter.getName(), fts);
		if ( fts.length() > 0 ) {
			sqlProps.put("fts", fts.toString());
		}
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Member> resolveUserMembers(Long programId, DateTime effectiveDate) {
		return getMemberSet(programId, User.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Member> getUserMembers(Long programId, DateTime effectiveDate) {
		return getMemberSet(programId, Member.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignUserMembers(final Long programId, final Set<Long> userIdSet,
			final Long effectiveId) {
		storeMemberSet(programId, Member.class, userIdSet, effectiveId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Member> getParticipantMembers(Long programId,
			DateTime effectiveDate) {
		return getMemberSet(programId, Participant.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignParticipantMembers(final Long programId,
			final Set<Long> participantIdSet, final Long effectiveId) {
		storeMemberSet(programId, Participant.class, participantIdSet, effectiveId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<EventRule> getProgramEventRules(Long programId,
			DateTime effectiveDate) {
		return getRelatedSet(programId, EventRule.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignProgramEventRules(Long programId,
			Set<Long> eventRuleIdSet, Long effectiveId) {
		storeRelatedSet(programId, EventRule.class, eventRuleIdSet, effectiveId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Constraint> getConstraints(Long programId, DateTime effectiveDate) {
		return getRelatedSet(programId, Constraint.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignConstraints(Long programId, Set<Long> constraintIdSet,
			Long effectiveId) {
		storeRelatedSet(programId, Constraint.class, constraintIdSet, effectiveId);
	}
	
}
