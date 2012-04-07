/* ==================================================================
 * IbatisParticipantDao.java - Jun 6, 2011 12:57:45 PM
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
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.solarnetwork.central.dras.dao.ParticipantDao;
import net.solarnetwork.central.dras.dao.ParticipantFilter;
import net.solarnetwork.central.dras.domain.Capability;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Fee;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Participant;

/**
 * Ibatis implementation of {@link ParticipantDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisParticipantDao
extends DrasIbatisFilterableDaoSupport<Participant, Match, ParticipantFilter> 
implements ParticipantDao {

	/**
	 * Default constructor.
	 */
	public IbatisParticipantDao() {
		super(Participant.class, Match.class);
	}

	@Override
	protected String getFilteredQuery(String filterDomain,
			ParticipantFilter filter) {
		String result = super.getFilteredQuery(filterDomain, filter);
		if ( Boolean.TRUE.equals(filter.getIncludeCapability()) ) {
			result = result +"-" +Capability.class.getSimpleName();
		}
		return result;
	}

	@Override
	protected void postProcessFilterProperties(ParticipantFilter filter, Map<String, Object> sqlProps) {
		// add flags to the query processor for dynamic logic
		StringBuilder fts = new StringBuilder();
		spaceAppend(filter.getName(), fts);
		if ( fts.length() > 0 ) {
			sqlProps.put("fts", fts.toString());
		}
		
		boolean haveLocationJoin = false;
		if ( filter.getIcp() != null || filter.getGxp() != null || filter.isBox() ) {
			haveLocationJoin = true;
			sqlProps.put("hasLocationJoin", Boolean.TRUE);
		}
		boolean haveEventJoin = false;
		if ( filter.getEventId() != null ) {
			haveEventJoin = true;
			sqlProps.put("hasEventJoin", Boolean.TRUE);
		}
		if ( haveLocationJoin || haveEventJoin) {
			sqlProps.put("hasJoin", Boolean.TRUE);
		}
	}
	
	@Override
	protected Long handleUpdate(Participant datum) {
		Long capabilityId = handleChildRelation(datum, datum.getCapability(), Capability.class);
		if ( capabilityId != null ) {
			datum.getCapability().setId(capabilityId);
		}
		return super.handleUpdate(datum);
	}

	@Override
	protected Long handleInsert(Participant datum) {
		Long capabilityId = handleChildRelation(datum, datum.getCapability(), Capability.class);
		if ( capabilityId != null ) {
			datum.getCapability().setId(capabilityId);
		}
		return super.handleInsert(datum);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Constraint> getConstraints(Long participantId,
			DateTime effectiveDate) {
		return getRelatedSet(participantId, Constraint.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignConstraints(Long participantId,
			Set<Long> constraintIdSet, Long effectiveId) {
		storeRelatedSet(participantId, Constraint.class, constraintIdSet, effectiveId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Constraint> getParticipantProgramConstraints(Long participantId,
			Long programId, DateTime effectiveDate) {
		Map<String, Object> props = new HashMap<String, Object>(1);
		props.put("programId", programId);
		return getRelatedSet(participantId, Constraint.class, effectiveDate, props);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignParticipantProgramConstraints(Long participantId,
			Long programId, Set<Long> constraintIdSet, Long effectiveId) {
		Map<String, Object> props = new HashMap<String, Object>(1);
		props.put("programId", programId);
		storeRelatedSet(participantId, Constraint.class, constraintIdSet, effectiveId, props);
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
