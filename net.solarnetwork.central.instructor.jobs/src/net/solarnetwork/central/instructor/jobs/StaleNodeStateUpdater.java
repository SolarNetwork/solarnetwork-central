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

import org.joda.time.DateTime;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to periodically find "stale" instructions of a given state and update
 * their state to something else.
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public class StaleNodeStateUpdater extends JobSupport {

	/** The default value for the {@code secondsOlder} property. */
	public static final int DEFAULT_SECONDS_OLDER = 30;

	private final NodeInstructionDao dao;
	private int secondsOlder;
	private InstructionState expectedState;
	private InstructionState state;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        The EventAdmin to use.
	 * @param dao
	 *        The NodeInstructionDao to use.
	 */
	public StaleNodeStateUpdater(EventAdmin eventAdmin, NodeInstructionDao dao) {
		super(eventAdmin);
		this.dao = dao;
		setSecondsOlder(DEFAULT_SECONDS_OLDER);
		setExpectedState(null);
		setState(null);
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		DateTime date = new DateTime().minusSeconds(secondsOlder);
		long result = dao.updateStaleInstructionsState(expectedState, date, state);
		if ( result > 0 ) {
			log.info("Updated {} node instructions older than {} ({} seconds ago) from {} to {}", result,
					date, secondsOlder, expectedState, state);
		}
		return true;
	}

	/**
	 * Set the number of minutes older an instruction must be to update its
	 * state.
	 * 
	 * @param secondsOlder
	 *        the minimum number of seconds older an instruction must be;
	 *        negative values are treated as {@literal 0}
	 */
	public void setSecondsOlder(int secondsOlder) {
		if ( secondsOlder < 0 ) {
			secondsOlder = 0;
		}
		this.secondsOlder = secondsOlder;
	}

	/**
	 * The instruction state to look for.
	 * 
	 * @param expectedState
	 *        the state for instructions to be in that should be changed;
	 *        {@literal null} will be treated as
	 *        {@link InstructionState#Queuing}
	 */
	public void setExpectedState(InstructionState expectedState) {
		if ( expectedState == null ) {
			expectedState = InstructionState.Queuing;
		}
		this.expectedState = expectedState;
	}

	/**
	 * The state to update found instructions to.
	 * 
	 * @param state
	 *        the state to change matching instructions to; {@literal null} will
	 *        be treated as {@link InstructionState#Queued}
	 */
	public void setState(InstructionState state) {
		if ( state == null ) {
			state = InstructionState.Queued;
		}
		this.state = state;
	}

}
