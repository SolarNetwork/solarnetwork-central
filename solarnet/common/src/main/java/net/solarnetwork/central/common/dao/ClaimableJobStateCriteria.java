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

import org.jspecify.annotations.Nullable;
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
	 * {@link #getClaimableJobStates()} set, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the first state, or {@code null} if not available
	 */
	default @Nullable ClaimableJobState getClaimableJobState() {
		ClaimableJobState[] states = getClaimableJobStates();
		return (states != null && states.length > 0 ? states[0] : null);
	}

	/**
	 * Get a set of claimable job states.
	 * 
	 * @return array of states (may be {@code null})
	 */
	ClaimableJobState @Nullable [] getClaimableJobStates();

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
	 * @return the claimable job states, as key values
	 */
	default String @Nullable [] getClaimableJobStateKeys() {
		ClaimableJobState[] states = getClaimableJobStates();
		if ( states == null ) {
			return null;
		}
		final int len = states.length;
		if ( len < 1 ) {
			return null;
		}
		String[] result = new String[len];
		for ( int i = 0; i < len; i++ ) {
			result[i] = states[i].keyValue();
		}
		return result;
	}

	/**
	 * Get the first claimable job state.
	 * 
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasClaimableJobStateCriteria()} returns {@code true}, to avoid
	 * nullness warnings.
	 * </p>
	 * 
	 * @return the first claimableJobState ID (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default ClaimableJobState claimableJobState() {
		return getClaimableJobState();
	}

	/**
	 * Get an array of claimable job states.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasClaimableJobStateCriteria()} returns {@code true}, to avoid
	 * nullness warnings.
	 * </p>
	 *
	 * @return array of claimableJobState IDs (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default ClaimableJobState[] claimableJobStates() {
		return getClaimableJobStates();
	}

	/**
	 * Get the set of claimable job state keys.
	 * 
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasClaimableJobStateCriteria()} returns {@code true}, to avoid
	 * nullness warnings.
	 * </p>
	 *
	 * @return the claimable job states, as key values (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default String[] claimableJobStateKeys() {
		return getClaimableJobStateKeys();
	}

}
