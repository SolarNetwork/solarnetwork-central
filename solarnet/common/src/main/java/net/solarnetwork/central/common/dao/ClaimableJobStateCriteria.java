/* ==================================================================
 * ClaimableJobStateCriteria.java - 10/10/2024 12:15:23 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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
 */

package net.solarnetwork.central.common.dao;

import java.util.SequencedSet;
import net.solarnetwork.central.domain.ClaimableJobState;

/**
 * Search criteria for claimable job state related data.
 * 
 * @author matt
 * @version 1.0
 */
public interface ClaimableJobStateCriteria {

	/**
	 * Get the first claimable job state.
	 * 
	 * <p>
	 * This returns the first available state from the
	 * {@link #getClaimableJobStates()} set, or {@literal null} if not
	 * available.
	 * </p>
	 * 
	 * @return the first state, or {@literal null} if not available
	 */
	default ClaimableJobState getClaimableJobState() {
		SequencedSet<? extends ClaimableJobState> states = getClaimableJobStates();
		return (states != null ? states.getFirst() : null);
	}

	/**
	 * Get a set of claimable job states.
	 * 
	 * @return set of states (may be {@literal null})
	 */
	SequencedSet<? extends ClaimableJobState> getClaimableJobStates();

	/**
	 * Test if this filter has any claimable job state criteria.
	 * 
	 * @return {@literal true} if the state is non-null
	 */
	default boolean hasClaimableJobStateCriteria() {
		return getClaimableJobState() != null;
	}

	/**
	 * Get the set of claimable job state keys.
	 * 
	 * @return the claimable job states, and key values
	 */
	default String[] claimableJobStateKeys() {
		SequencedSet<? extends ClaimableJobState> states = getClaimableJobStates();
		if ( states == null || states.isEmpty() ) {
			return null;
		}
		return states.stream().map(s -> s.keyValue()).toArray(String[]::new);
	}

}
