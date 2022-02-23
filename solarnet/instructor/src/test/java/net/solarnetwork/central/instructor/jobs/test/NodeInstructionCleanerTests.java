/* ==================================================================
 * NodeInstructionCleanerTests.java - 24/02/2022 12:01:07 PM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.time.Instant;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.jobs.NodeInstructionCleaner;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link NodeInstructionCleaner} class.
 * 
 * @author matt
 * @version 1.0
 */
public class NodeInstructionCleanerTests {

	private NodeInstructionDao instructionDao;
	private NodeInstructionCleaner job;

	@Before
	public void setup() {
		instructionDao = EasyMock.createMock(NodeInstructionDao.class);
		job = new NodeInstructionCleaner(instructionDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(instructionDao);
	}

	private void replayAll() {
		EasyMock.replay(instructionDao);
	}

	@Test
	public void execute() {
		// GIVEN
		job.setDaysOlder(1);
		job.setAbandonedDaysOlder(10);

		Instant start = Instant.now();
		expect(instructionDao.purgeCompletedInstructions(assertWith(new Assertion<Instant>() {

			@Override
			public void check(Instant arg) throws Throwable {
				assertThat("About 1 day older", MINUTES.between(start.minus(1, DAYS), arg), is(0L));
			}

		}))).andReturn(1L);

		expect(instructionDao.purgeIncompleteInstructions(assertWith(new Assertion<Instant>() {

			@Override
			public void check(Instant arg) throws Throwable {
				assertThat("About 10 days older", MINUTES.between(start.minus(10, DAYS), arg), is(0L));
			}

		}))).andReturn(1L);

		// WHEN
		replayAll();
		job.run();
	}
}
