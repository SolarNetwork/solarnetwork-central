/* ==================================================================
 * DuplicateMessageFilterTests.java - 9/03/2025 12:49:00 pm
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

package net.solarnetwork.central.support.test;

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import net.solarnetwork.central.support.DuplicateMessageFilter;

/**
 * Test cases for the {@link DuplicateMessageFilter} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DuplicateMessageFilterTests {

	private ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) LoggerFactory
			.getLogger(DuplicateMessageFilterTests.class);

	private DuplicateMessageFilter filter;

	@BeforeEach
	public void setup() {
		filter = new DuplicateMessageFilter();
		filter.setAllowedRepetitions(0);
		filter.setCacheSize(2);
		filter.setExpiration(60_000L);
		filter.setLevel("info");
	}

	@Test
	public void denyDuplicate_noArguments() {
		// GIVEN
		LoggingEvent evt1 = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test", null, null);
		LoggingEvent evt1a = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test", null, null);

		filter.start();

		// WHEN
		// @formatter:off
		then(filter.decide(evt1))
			.as("First call with message is allowed")
			.isEqualTo(FilterReply.NEUTRAL)
			;
		then(filter.decide(evt1a))
			.as("Second call with message is denied")
			.isEqualTo(FilterReply.DENY)
			;
		// @formatter:on
	}

	@Test
	public void denyDuplicate_withArguments() {
		// GIVEN
		LoggingEvent evt1 = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test {} and {}", null, new Object[] { 1, 2 });
		LoggingEvent evt2 = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test {} and {}", null, new Object[] { 1, 1 });
		LoggingEvent evt1a = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test {} and {}", null, new Object[] { 1, 2 });

		filter.start();

		// WHEN
		// @formatter:off
		then(filter.decide(evt1))
			.as("First call with message 1 is allowed")
			.isEqualTo(FilterReply.NEUTRAL)
			;
		then(filter.decide(evt2))
			.as("First call with message 2 is allowed because arguments differ")
			.isEqualTo(FilterReply.NEUTRAL)
			;
		then(filter.decide(evt1a))
			.as("Second call with same message 1 is denied")
			.isEqualTo(FilterReply.DENY)
			;
		// @formatter:on
	}

	@Test
	public void denyDuplicate_withArrayArguments() {
		// GIVEN
		LoggingEvent evt1 = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test {}", null, new Object[] { new Integer[] { 1, 2 } });
		LoggingEvent evt2 = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test {}", null, new Object[] { new Integer[] { 2, 3 } });
		LoggingEvent evt1a = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test {}", null, new Object[] { new Integer[] { 1, 2 } });

		filter.start();

		// WHEN
		// @formatter:off
		then(filter.decide(evt1))
			.as("First call with message 1 is allowed")
			.isEqualTo(FilterReply.NEUTRAL)
			;
		then(filter.decide(evt2))
			.as("First call with message 2 is allowed because arguments differ")
			.isEqualTo(FilterReply.NEUTRAL)
			;
		then(filter.decide(evt1a))
			.as("Second call with same message 1 is denied")
			.isEqualTo(FilterReply.DENY)
			;
		// @formatter:on
	}

	@Test
	public void denyDuplicate_cacheOverflow() {
		// GIVEN
		LoggingEvent evt1 = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test 1", null, null);
		LoggingEvent evt2 = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test 2", null, null);
		LoggingEvent evt3 = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test 3", null, null);

		LoggingEvent evt1a = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test 1", null, null);

		filter.start();

		// WHEN
		// @formatter:off
		then(filter.decide(evt1))
			.as("First call with message 1 is allowed")
			.isEqualTo(FilterReply.NEUTRAL)
			;
		then(filter.decide(evt2))
			.as("First call with message 2 is allowed")
			.isEqualTo(FilterReply.NEUTRAL)
			;
		then(filter.decide(evt3))
			.as("First call with message 3 is allowed")
			.isEqualTo(FilterReply.NEUTRAL)
			;
		then(filter.decide(evt1a))
			.as("First call with message 3 is allowed because LRU cache overflow kicked out evt1")
			.isEqualTo(FilterReply.NEUTRAL)
			;
		// @formatter:on
	}

	@Test
	public void denyDuplicate_expire() throws InterruptedException {
		// GIVEN
		LoggingEvent evt1 = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test 1", null, null);
		LoggingEvent evt1a = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test 1", null, null);
		LoggingEvent evt1b = new LoggingEvent(DuplicateMessageFilterTests.class.getName(), log,
				Level.INFO, "Test 1", null, null);

		filter.setExpiration(100);
		filter.start();

		// WHEN
		// @formatter:off
		then(filter.decide(evt1))
			.as("1st call with message 1 is allowed")
			.isEqualTo(FilterReply.NEUTRAL)
			;
		
		Thread.sleep(200);
		
		then(filter.decide(evt1a))
			.as("2nd call with message 1 is allowed because cached message expired")
			.isEqualTo(FilterReply.NEUTRAL)
			;
		then(filter.decide(evt1b))
			.as("3rd call with message 1 is denied because cached message not expired")
			.isEqualTo(FilterReply.DENY)
			;
		// @formatter:on
	}

}
