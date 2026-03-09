/* ==================================================================
 * StaleNodeStateUpdater.java - 13/06/2018 2:55:24 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.jobs;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Job to periodically find "stale" instructions of a given state and update
 * their state to something else.
 * 
 * @author matt
 * @version 2.2
 * @since 1.2
 */
public class StaleNodeStateUpdater extends JobSupport {

	/** The {@code secondsOlder} property default value. */
	public static final int DEFAULT_SECONDS_OLDER = 30;

	/**
	 * The {@code expectedState} property default value.
	 * 
	 * @since 2.2
	 */
	public static final InstructionState DEFAULT_EXPECTED_STATE = InstructionState.Queuing;

	/**
	 * The {@code state} property default value.
	 * 
	 * @since 2.2
	 */
	public static final InstructionState DEFAULT_STATE = InstructionState.Queued;

	private final NodeInstructionDao dao;
	private int secondsOlder = DEFAULT_SECONDS_OLDER;
	private InstructionState expectedState = DEFAULT_EXPECTED_STATE;
	private InstructionState state = DEFAULT_STATE;

	/**
	 * Constructor.
	 * 
	 * @param dao
	 *        The NodeInstructionDao to use.
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public StaleNodeStateUpdater(NodeInstructionDao dao) {
		super("Instruction", "StaleNodeStateUpdater");
		this.dao = requireNonNullArgument(dao, "dao");
	}

	@Override
	public void run() {
		Instant date = Instant.now().minusSeconds(secondsOlder);
		long result = dao.updateStaleInstructionsState(expectedState, date, state);
		if ( result > 0 ) {
			log.info("Updated {} node instructions older than {} ({} seconds ago) from {} to {}", result,
					date, secondsOlder, expectedState, state);
		}
	}

	/**
	 * Set the number of minutes older an instruction must be to update its
	 * state.
	 * 
	 * @param secondsOlder
	 *        the minimum number of seconds older an instruction must be;
	 *        negative values are treated as {@literal 0}
	 */
	public final void setSecondsOlder(int secondsOlder) {
		this.secondsOlder = (secondsOlder > 0 ? secondsOlder : 0);
	}

	/**
	 * The instruction state to look for.
	 * 
	 * @param expectedState
	 *        the state for instructions to be in that should be changed;
	 *        {@code null} will be treated as {@link InstructionState#Queuing}
	 */
	public final void setExpectedState(@Nullable InstructionState expectedState) {
		this.expectedState = (expectedState != null ? expectedState : DEFAULT_EXPECTED_STATE);
	}

	/**
	 * The state to update found instructions to.
	 * 
	 * @param state
	 *        the state to change matching instructions to; {@code null} will be
	 *        treated as {@link InstructionState#Queued}
	 */
	public final void setState(@Nullable InstructionState state) {
		this.state = (state != null ? state : DEFAULT_STATE);
	}

}
