/* ==================================================================
 * UserNodeInstructionTaskEntityInput.java - 12/11/2025 10:35:00 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import java.util.Set;
import jakarta.validation.constraints.NotNull;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.validation.StartStopClaimableJobState;

/**
 * DTO for cloud control instruction task state.
 *
 * @author matt
 * @version 1.0
 */
public class UserNodeInstructionTaskStateInput {

	@NotNull
	@StartStopClaimableJobState
	private BasicClaimableJobState state;

	private Set<BasicClaimableJobState> requiredStates;

	/**
	 * Constructor.
	 */
	public UserNodeInstructionTaskStateInput() {
		super();
	}

	/**
	 * Get the job state.
	 *
	 * @return the state
	 */
	public final BasicClaimableJobState getState() {
		return state;
	}

	/**
	 * Set the job state.
	 *
	 * @param state
	 *        the state to set
	 */
	public final void setState(BasicClaimableJobState state) {
		this.state = state;
	}

	/**
	 * Get a list of states the job must have in order to perform an update.
	 *
	 * @return the states, or {@literal null}
	 */
	public final Set<BasicClaimableJobState> getRequiredStates() {
		return requiredStates;
	}

	/**
	 * Set a list of states the job must have in order to perform an update.
	 *
	 * @param requiredStates
	 *        the states to set, or {@literal null}
	 */
	public final void setRequiredStates(Set<BasicClaimableJobState> requiredStates) {
		this.requiredStates = requiredStates;
	}

}
