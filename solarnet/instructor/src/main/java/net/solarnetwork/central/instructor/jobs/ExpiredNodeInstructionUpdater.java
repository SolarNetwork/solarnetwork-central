/* ==================================================================
 * ExpiredNodeInstructionUpdater.java - 20/03/2025 5:00:07 pm
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

package net.solarnetwork.central.instructor.jobs;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Instant;
import java.time.InstantSource;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Transition expired instructions to a completed state.
 * 
 * @author matt
 * @version 2.0
 */
public class ExpiredNodeInstructionUpdater extends JobSupport {

	private final InstantSource clock;
	private final NodeInstructionDao dao;
	private final InstructionState resultState;
	private final Map<String, Object> resultParameters;

	/**
	 * Constructor.
	 * 
	 * @param dao
	 *        the DAO to use
	 * @param resultParameters
	 *        the result parameter to use
	 */
	public ExpiredNodeInstructionUpdater(NodeInstructionDao dao, Map<String, Object> resultParameters) {
		this(Clock.systemUTC(), dao, InstructionState.Declined, resultParameters);
	}

	/**
	 * Constructor.
	 * 
	 * @param clock
	 *        the clock to use
	 * @param dao
	 *        the DAO to use
	 * @param resultState
	 *        the result state to use
	 * @param resultParameters
	 *        the result parameter to use
	 */
	public ExpiredNodeInstructionUpdater(InstantSource clock, NodeInstructionDao dao,
			InstructionState resultState, Map<String, Object> resultParameters) {
		super();
		setGroupId("Instruction");
		this.clock = requireNonNullArgument(clock, "clock");
		this.dao = requireNonNullArgument(dao, "dao");
		this.resultState = requireNonNullArgument(resultState, "resultState");
		this.resultParameters = requireNonNullArgument(resultParameters, "resultParameters");
	}

	@Override
	public void run() {
		Instant date = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		NodeInstruction criteria = new NodeInstruction();
		criteria.setExpirationDate(date);
		criteria.getInstruction().setState(resultState);
		criteria.getInstruction().setResultParameters(resultParameters);
		long result = dao.transitionExpiredInstructions(criteria);
		if ( result > 0 ) {
			log.info("Transitioned {} node instructions that expired before {} to {}", result, date,
					resultState);
		}
	}

}
