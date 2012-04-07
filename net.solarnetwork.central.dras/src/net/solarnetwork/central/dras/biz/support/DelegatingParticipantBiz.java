/* ==================================================================
 * DelegatingParticipantBiz.java - Jun 25, 2011 6:27:24 PM
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

package net.solarnetwork.central.dras.biz.support;

import java.util.List;
import java.util.Set;

import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.dras.biz.ParticipantBiz;
import net.solarnetwork.central.dras.dao.ParticipantFilter;
import net.solarnetwork.central.dras.dao.ParticipantGroupFilter;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.ParticipantGroup;
import net.solarnetwork.central.dras.support.CapableParticipant;
import net.solarnetwork.central.dras.support.CapableParticipantGroup;

/**
 * Delegating {@link ParticipantBiz}, to support AOP with OSGi services.
 * 
 * @author matt
 * @version $Revision$
 */
public class DelegatingParticipantBiz implements ParticipantBiz {

	private final ParticipantBiz delegate;
	
	/**
	 * Constructor.
	 * 
	 * @param delegate the delegate
	 */
	public DelegatingParticipantBiz(ParticipantBiz delegate) {
		this.delegate = delegate;
	}

	@Override
	public List<Match> findParticipants(
			ObjectCriteria<ParticipantFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		return delegate.findParticipants(criteria, sortDescriptors);
	}

	@Override
	public Participant getParticipant(Long participantId) {
		return delegate.getParticipant(participantId);
	}

	@Override
	public CapableParticipant getCapableParticipant(Long participantId) {
		return delegate.getCapableParticipant(participantId);
	}

	@Override
	public List<Match> findParticipantGroups(
			ObjectCriteria<ParticipantGroupFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		return delegate.findParticipantGroups(criteria, sortDescriptors);
	}

	@Override
	public ParticipantGroup getParticipantGroup(Long participantGroupId) {
		return delegate.getParticipantGroup(participantGroupId);
	}

	@Override
	public CapableParticipantGroup getCapableParticipantGroup(
			Long participantGroupId) {
		return delegate.getCapableParticipantGroup(participantGroupId);
	}

	@Override
	public Set<Constraint> getParticipantConstraints(Long participantId) {
		return delegate.getParticipantConstraints(participantId);
	}

	@Override
	public Set<Constraint> getParticipantProgramConstraints(Long participantId,
			Long programId) {
		return delegate.getParticipantProgramConstraints(participantId,
				programId);
	}
	
}
