/* ==================================================================
 * IbatisParticipantGroupDao.java - Jun 6, 2011 4:12:12 PM
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

import net.solarnetwork.central.dras.dao.ParticipantGroupDao;
import net.solarnetwork.central.dras.dao.ParticipantGroupFilter;
import net.solarnetwork.central.dras.domain.Capability;
import net.solarnetwork.central.dras.domain.Fee;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.ParticipantGroup;

import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ibatis implementation of {@link ParticipantGroupDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisParticipantGroupDao
extends DrasIbatisFilterableDaoSupport<ParticipantGroup, Match, ParticipantGroupFilter>
implements ParticipantGroupDao {

	/**
	 * Default constructor.
	 */
	public IbatisParticipantGroupDao() {
		super(ParticipantGroup.class, Match.class);
	}
	
	@Override
	protected String getFilteredQuery(String filterDomain,
			ParticipantGroupFilter filter) {
		String result = super.getFilteredQuery(filterDomain, filter);
		if ( Boolean.TRUE.equals(filter.getIncludeCapability()) ) {
			result = result +"-" +Capability.class.getSimpleName();
		}
		return result;
	}

	@Override
	protected void postProcessFilterProperties(ParticipantGroupFilter filter, Map<String, Object> sqlProps) {
		// add flags to the query processor for dynamic logic
		StringBuilder fts = new StringBuilder();
		spaceAppend(filter.getName(), fts);
		if ( fts.length() > 0 ) {
			sqlProps.put("fts", fts.toString());
		}
		
		boolean haveLocationJoin = false;
		if ( fts.length() > 0 || filter.getIcp() != null || filter.getGxp() != null || filter.isBox() ) {
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
	protected Long handleUpdate(ParticipantGroup datum) {
		Long capabilityId = handleChildRelation(datum, datum.getCapability(), Capability.class);
		if ( capabilityId != null ) {
			datum.getCapability().setId(capabilityId);
		}
		return super.handleUpdate(datum);
	}

	@Override
	protected Long handleInsert(ParticipantGroup datum) {
		Long capabilityId = handleChildRelation(datum, datum.getCapability(), Capability.class);
		if ( capabilityId != null ) {
			datum.getCapability().setId(capabilityId);
		}
		return super.handleInsert(datum);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Set<Member> getParticipantMembers(Long programId,
			DateTime effectiveDate) {
		return getMemberSet(programId, Participant.class, effectiveDate);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void assignParticipantMembers(Long programId,
			Set<Long> participantIdSet, Long effectiveId) {
		storeMemberSet(programId, Participant.class, participantIdSet, effectiveId);
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
