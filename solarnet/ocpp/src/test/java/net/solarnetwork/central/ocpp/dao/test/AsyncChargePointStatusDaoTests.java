/* ==================================================================
 * AsyncChargePointStatusDaoTests.java - 2/07/2024 2:55:12 pm
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

package net.solarnetwork.central.ocpp.dao.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.ocpp.dao.AsyncChargePointStatusDao;
import net.solarnetwork.central.ocpp.dao.AsyncChargePointStatusDao.StatusUpdate;
import net.solarnetwork.central.ocpp.dao.BasicOcppCriteria;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusDao;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;
import net.solarnetwork.central.support.DelayQueueSet;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Test cases for the {@link AsyncChargePointStatusDao} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class AsyncChargePointStatusDaoTests {

	@Mock
	private TaskScheduler scheduler;

	@Mock
	private ChargePointStatusDao delegate;

	@Mock
	private FilteredResultsProcessor<ChargePointStatus> processor;

	@Mock
	private ScheduledFuture<?> future;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.SECONDS),
			ZoneOffset.UTC);
	private Queue<StatusUpdate> statuses;
	private AsyncChargePointStatusDao dao;

	@BeforeEach
	public void setup() {
		statuses = new DelayQueueSet<>();
		dao = new AsyncChargePointStatusDao(clock, scheduler, delegate, statuses);
	}

	@Test
	public void findFiltered() throws IOException {
		// GIVEN
		final var filter = new BasicOcppCriteria();
		final List<SortDescriptor> sorts = Collections.emptyList();
		final Integer offset = 0;
		final Integer max = 1;

		final var daoResults = new BasicFilterResults<ChargePointStatus, UserLongCompositePK>(
				Collections.emptyList());
		given(delegate.findFiltered(any(), any(), any(), any())).willReturn(daoResults);

		// WHEN
		var result = dao.findFiltered(filter, sorts, offset, max);

		// THEN
		and.then(result).as("DAO result returned").isSameAs(daoResults);
	}

	@Test
	public void findFilteredStream() throws IOException {
		// GIVEN
		var filter = new BasicOcppCriteria();
		final List<SortDescriptor> sorts = Collections.emptyList();
		final Integer offset = 0;
		final Integer max = 1;

		// WHEN
		dao.findFilteredStream(filter, processor, sorts, offset, max);

		// THEN
		then(delegate).should().findFilteredStream(same(filter), same(processor), same(sorts),
				same(offset), same(max));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void updateStatus_connected() {
		// GIVEN
		final Long userId = randomLong();
		final String chargePointIdentifier = randomString();
		final String connectedTo = randomString();
		final String sessionId = randomString();
		final Instant connectionDate = Instant.now();
		final boolean connected = true;

		given(scheduler.schedule(any(), any(Instant.class))).willReturn((ScheduledFuture) future);

		// WHEN
		dao.updateConnectionStatus(userId, chargePointIdentifier, connectedTo, sessionId, connectionDate,
				connected);

		// THEN
		// @formatter:off
		then(scheduler).should().schedule(same(dao), eq(clock.instant().plus(dao.getDelay())));
		and.then(statuses)
			.as("Status buffered")
			.hasSize(1)
			.first()
			.as("User ID same as passed in method")
			.returns(userId, from(StatusUpdate::getUserId))
			.as("Charge point ID same as passed in method")
			.returns(chargePointIdentifier, from(StatusUpdate::getChargePointIdentifier))
			.as("Connected to same as passed in method")
			.returns(connectedTo, from(StatusUpdate::getConnectedTo))
			.as("Session ID same as passed in method")
			.returns(sessionId, from(StatusUpdate::getSessionId))
			.as("Connection date same as passed in method")
			.returns(connectionDate, from(StatusUpdate::getConnectionDate))
			.as("Connected flag same as passed in method")
			.returns(connected, from(StatusUpdate::isConnected))
			;
		// @formatter:on
	}

	@Test
	public void flush_last() {
		// GIVEN
		final Long userId = randomLong();
		final String chargePointIdentifier = randomString();
		final String connectedTo = randomString();
		final String sessionId = randomString();
		final Instant connectionDate = Instant.now();
		final boolean connected = true;
		statuses.add(dao.updateFor(clock.instant(), userId, chargePointIdentifier, connectedTo,
				sessionId, connectionDate, connected));

		// WHEN
		// jump ahead in time
		clock.add(dao.getDelay());
		dao.run();

		// THEN
		then(delegate).should().updateConnectionStatus(userId, chargePointIdentifier, connectedTo,
				sessionId, connectionDate, connected);

		// @formatter:off
		and.then(statuses)
			.as("Status emptied")
			.isEmpty()
			;
		// @formatter:on

		// nothing new to schedule
		then(scheduler).shouldHaveNoMoreInteractions();
	}

	@Test
	public void flush_notLast() {
		// GIVEN
		final Long userId = randomLong();
		final String chargePointIdentifier = randomString();
		final String connectedTo = randomString();
		final String sessionId = randomString();
		final Instant connectionDate = Instant.now();
		final boolean connected = true;
		statuses.add(dao.updateFor(clock.instant(), userId, chargePointIdentifier, connectedTo,
				sessionId, connectionDate, connected));

		// add another newer update, too new to flush
		final Long userId2 = randomLong();
		final String chargePointIdentifier2 = randomString();
		final String connectedTo2 = randomString();
		final String sessionId2 = randomString();
		final Instant connectionDate2 = Instant.now();
		final boolean connected2 = false;
		statuses.add(dao.updateFor(clock.instant().plusSeconds(1), userId2, chargePointIdentifier2,
				connectedTo2, sessionId2, connectionDate2, connected2));

		// WHEN
		// jump ahead in time
		clock.add(dao.getDelay());
		dao.run();

		// THEN
		then(delegate).should().updateConnectionStatus(userId, chargePointIdentifier, connectedTo,
				sessionId, connectionDate, connected);

		// @formatter:off
		and.then(statuses)
			.as("Status reduced by 1")
			.hasSize(1)
			.first()
			.as("User ID same as passed in method")
			.returns(userId2, from(StatusUpdate::getUserId))
			.as("Charge point ID same as passed in method")
			.returns(chargePointIdentifier2, from(StatusUpdate::getChargePointIdentifier))
			.as("Connected to same as passed in method")
			.returns(connectedTo2, from(StatusUpdate::getConnectedTo))
			.as("Session ID same as passed in method")
			.returns(sessionId2, from(StatusUpdate::getSessionId))
			.as("Connection date same as passed in method")
			.returns(connectionDate2, from(StatusUpdate::getConnectionDate))
			.as("Connected flag same as passed in method")
			.returns(connected2, from(StatusUpdate::isConnected))
			;
		// @formatter:on

		// need to re-schedule as more statuses to flush
		then(scheduler).should().schedule(same(dao), eq(clock.instant().plus(dao.getDelay())));
	}
}
