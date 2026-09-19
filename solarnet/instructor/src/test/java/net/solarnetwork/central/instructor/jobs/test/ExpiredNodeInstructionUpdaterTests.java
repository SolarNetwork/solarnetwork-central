/* ==================================================================
 * ExpiredNodeInstructionUpdaterTests.java - 20/03/2025 5:07:26 pm
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

package net.solarnetwork.central.instructor.jobs.test;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.jobs.ExpiredNodeInstructionUpdater;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Test cases for the {@link ExpiredNodeInstructionUpdater} class.
 * 
 * @author matt
 * @version 2.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class ExpiredNodeInstructionUpdaterTests {

	@Mock
	private NodeInstructionDao dao;

	@Captor
	private ArgumentCaptor<NodeInstruction> instructionCaptor;

	private final Clock clock = Clock
			.fixed(Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(123L), ZoneOffset.UTC);

	@Test
	public void execute() {
		// GIVEN
		final var endState = InstructionState.Declined;
		final var endParams = Map.of("message", (Object) "foo");
		var job = new ExpiredNodeInstructionUpdater(clock, dao, endState, endParams);

		given(dao.transitionExpiredInstructions(any())).willReturn(1);

		// WHEN
		job.run();

		// THEN
		then(dao).should().transitionExpiredInstructions(instructionCaptor.capture());

		// @formatter:off
		and.then(instructionCaptor.getValue().getInstruction())
			.as("Expiration date is 'now' truncated to seconds")
			.returns(clock.instant().truncatedTo(SECONDS), from(Instruction::getExpirationDate))
			.as("Final state as configured on job")
			.returns(endState, from(Instruction::getState))
			.as("Final parameters as configured on job")
			.returns(endParams, from(Instruction::getResultParameters))
			;
		// @formatter:on
	}

}
