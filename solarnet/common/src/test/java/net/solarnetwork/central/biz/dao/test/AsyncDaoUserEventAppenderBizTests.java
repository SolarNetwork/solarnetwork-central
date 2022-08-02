/* ==================================================================
 * AsyncDaoUserEventAppenderBizTests.java - 2/08/2022 3:22:51 pm
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

package net.solarnetwork.central.biz.dao.test;

import static java.util.Arrays.sort;
import static java.util.UUID.randomUUID;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.time.Instant;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz;
import net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz.UserEventStats;
import net.solarnetwork.central.common.dao.UserEventAppenderDao;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.util.StatCounter;

/**
 * Test cases for the {@link AsyncDaoUserEventAppenderBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class AsyncDaoUserEventAppenderBizTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private TimeBasedGenerator uuidGenerator;
	private ExecutorService executor;
	private UserEventAppenderDao dao;
	private PriorityBlockingQueue<UserEvent> queue;
	private StatCounter stats;
	private AsyncDaoUserEventAppenderBiz biz;

	@BeforeEach
	public void setup() {
		uuidGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
		executor = Executors.newFixedThreadPool(3);
		dao = EasyMock.createMock(UserEventAppenderDao.class);
		queue = new PriorityBlockingQueue<>(64, AsyncDaoUserEventAppenderBiz.TIME_SORT);
		stats = new StatCounter("AsyncDaoUserEventAppender",
				"net.solarnetwork.central.biz.dao.AsyncDaoUserEventAppenderBiz",
				LoggerFactory.getLogger(AsyncDaoUserEventAppenderBiz.class), 10,
				UserEventStats.values());
		biz = new AsyncDaoUserEventAppenderBiz(executor, dao, queue, stats);
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(dao);
	}

	private void replayAll() {
		EasyMock.replay(dao);
	}

	@Test
	public void add() {
		// GIVEN
		UserEvent event = new UserEvent(randomUUID().getMostSignificantBits(), Instant.now(),
				uuidGenerator.generate(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
				"{\"foo\":123}");

		dao.add(event);

		// WHEN
		replayAll();
		biz.add(event);

		// THEN
		biz.serviceDidShutdown();
	}

	@Test
	public void add_concurrent() throws InterruptedException {
		// GIVEN
		final int taskCount = 100;
		Capture<UserEvent> eventsCaptor = new Capture<>(CaptureType.ALL);
		dao.add(capture(eventsCaptor));
		expectLastCall().times(taskCount);

		// WHEN
		replayAll();
		Queue<UserEvent> events = new ConcurrentLinkedQueue<>();
		ExecutorService exec = Executors.newWorkStealingPool(6);
		for ( int i = 0; i < taskCount; i++ ) {
			exec.submit(new Runnable() {

				@Override
				public void run() {
					UserEvent event = new UserEvent(randomUUID().getMostSignificantBits(), Instant.now(),
							uuidGenerator.generate(), UUID.randomUUID().toString(),
							UUID.randomUUID().toString(), "{\"foo\":123}");
					log.info("Adding event {}", event);
					events.add(event);
					biz.add(event);
				}
			});
		}
		exec.shutdown();
		exec.awaitTermination(1, TimeUnit.MINUTES);

		// THEN
		biz.serviceDidShutdown();
		assertThat("Stats added count", stats.get(UserEventStats.EventsAdded),
				is(equalTo((long) taskCount)));
		assertThat("Stats stored count", stats.get(UserEventStats.EventsStored),
				is(equalTo((long) taskCount)));

		UserEvent[] added = events.toArray(UserEvent[]::new);
		sort(added, Identity.sortByIdentity());

		UserEvent[] stored = eventsCaptor.getValues().toArray(UserEvent[]::new);
		sort(stored, Identity.sortByIdentity());

		assertThat("Added and stored the same", stored, arrayContaining(added));
	}

}
