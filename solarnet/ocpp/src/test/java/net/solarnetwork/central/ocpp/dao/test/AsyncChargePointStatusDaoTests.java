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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.ocpp.dao.AsyncChargePointStatusDao;
import net.solarnetwork.central.ocpp.dao.AsyncChargePointStatusDao.PendingCharger;
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
 * @version 1.2
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

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.SECONDS),
			ZoneOffset.UTC);
	private Queue<PendingCharger> chargers;
	private AsyncChargePointStatusDao dao;

	@BeforeEach
	public void setup() {
		chargers = new DelayQueueSet<>();
		dao = new AsyncChargePointStatusDao(clock, scheduler, delegate, chargers);
	}

	@Test
	public void findFiltered() throws IOException {
		// GIVEN
		final var filter = new BasicOcppCriteria();
		final List<SortDescriptor> sorts = List.of();
		final Long offset = 0L;
		final Integer max = 1;

		final var daoResults = new BasicFilterResults<ChargePointStatus, UserLongCompositePK>(List.of());
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
		final List<SortDescriptor> sorts = List.of();
		final Long offset = 0L;
		final Integer max = 1;

		// WHEN
		dao.findFilteredStream(filter, processor, sorts, offset, max);

		// THEN
		then(delegate).should().findFilteredStream(same(filter), same(processor), same(sorts),
				same(offset), same(max));
	}

	@Test
	public void updateStatus_connected() {
		// GIVEN
		final Long userId = randomLong();
		final String chargePointIdentifier = randomString();
		final String connectedTo = randomString();
		final String sessionId = randomString();
		final Instant connectionDate = Instant.now();
		final boolean connected = true;

		// WHEN
		dao.updateConnectionStatus(userId, chargePointIdentifier, connectedTo, sessionId, connectionDate,
				connected);

		// THEN
		// @formatter:off
		then(scheduler).should().schedule(same(dao), eq(clock.instant().plus(dao.getDelay())));
		then(delegate).shouldHaveNoInteractions();
		and.then(chargers)
			.as("Charger buffered")
			.hasSize(1)
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
		dao.updateConnectionStatus(userId, chargePointIdentifier, connectedTo, sessionId, connectionDate,
				connected);

		// WHEN
		// jump ahead in time
		clock.add(dao.getDelay());
		dao.run();

		// THEN
		// @formatter:off
		then(delegate).should().updateConnectionStatus(userId, chargePointIdentifier, connectedTo,
				sessionId, connectionDate, connected);
		and.then(chargers)
			.as("Chargers emptied")
			.isEmpty()
			;
		// only the initial flush scheduled, nothing new to schedule
		then(scheduler).should().schedule(same(dao), any(Instant.class));
		then(scheduler).shouldHaveNoMoreInteractions();
		// @formatter:on
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
		dao.updateConnectionStatus(userId, chargePointIdentifier, connectedTo, sessionId, connectionDate,
				connected);

		// add another newer update, too new to flush
		clock.add(dao.getDelay().dividedBy(2));
		final Long userId2 = randomLong();
		final String chargePointIdentifier2 = randomString();
		dao.updateConnectionStatus(userId2, chargePointIdentifier2, randomString(), randomString(),
				Instant.now(), false);

		// WHEN
		// jump ahead in time to when the first update is due
		clock.add(dao.getDelay().dividedBy(2));
		dao.run();

		// THEN
		// @formatter:off
		then(delegate).should().updateConnectionStatus(userId, chargePointIdentifier, connectedTo,
				sessionId, connectionDate, connected);
		then(delegate).shouldHaveNoMoreInteractions();
		and.then(chargers)
			.as("Chargers reduced by 1")
			.hasSize(1)
			;
		// need to re-schedule as more statuses to flush
		then(scheduler).should().schedule(same(dao), eq(clock.instant().plus(dao.getDelay())));
		// @formatter:on
	}

	/**
	 * A charger connection status row, as stored by the delegate DAO.
	 */
	private record StatusRow(String connectedTo, String sessionId) {

	}

	/**
	 * Make the delegate DAO behave like the real SQL: a connected update
	 * always replaces the row, and a disconnected update only clears the row
	 * when it is for the stored instance and session.
	 *
	 * @return the simulated rows, keyed by charger identifier
	 */
	private Map<String, StatusRow> givenSimulatedStatusRows() {
		final Map<String, StatusRow> rows = new HashMap<>(4);
		willAnswer(inv -> {
			final String cpId = inv.getArgument(1);
			final String connectedTo = inv.getArgument(2);
			final String sessionId = inv.getArgument(3);
			final boolean connected = inv.getArgument(5);
			if ( connected ) {
				rows.put(cpId, new StatusRow(connectedTo, sessionId));
			} else {
				rows.computeIfPresent(cpId,
						(_, row) -> row.connectedTo().equals(connectedTo)
								&& row.sessionId().equals(sessionId) ? new StatusRow(null, null)
										: row);
			}
			return null;
		}).given(delegate).updateConnectionStatus(any(), any(), any(), any(), any(), anyBoolean());
		return rows;
	}

	private void connect(Long userId, String cpId, String instance, String sessionId) {
		dao.updateConnectionStatus(userId, cpId, instance, sessionId, clock.instant(), true);
	}

	private void disconnect(Long userId, String cpId, String instance, String sessionId) {
		dao.updateConnectionStatus(userId, cpId, instance, sessionId, clock.instant(), false);
	}

	private void flushAfterDelay() {
		clock.add(dao.getDelay());
		dao.run();
	}

	@Test
	public void coalesce_connectThenDisconnectSameSession() {
		// GIVEN
		final Long userId = randomLong();
		final String cpId = randomString();

		// WHEN
		// charger connects and drops again within the delay
		connect(userId, cpId, "i1", "s1");
		disconnect(userId, cpId, "i1", "s1");
		flushAfterDelay();

		// THEN
		// the connection was never written, so neither update needs to be
		then(delegate).shouldHaveNoInteractions();
	}

	@Test
	public void coalesce_disconnectThenReconnect() {
		// GIVEN
		final Map<String, StatusRow> rows = givenSimulatedStatusRows();
		final Long userId = randomLong();
		final String cpId = randomString();
		rows.put(cpId, new StatusRow("i1", "s1"));

		// WHEN
		// charger drops and quickly reconnects within the delay
		disconnect(userId, cpId, "i1", "s1");
		connect(userId, cpId, "i1", "s2");
		flushAfterDelay();

		// THEN
		// @formatter:off
		and.then(rows.get(cpId))
			.as("Charger connected to the new session")
			.isEqualTo(new StatusRow("i1", "s2"))
			;
		// @formatter:on
	}

	@Test
	public void coalesce_reconnectThenLateDisconnect() {
		// GIVEN
		final Map<String, StatusRow> rows = givenSimulatedStatusRows();
		final Long userId = randomLong();
		final String cpId = randomString();
		rows.put(cpId, new StatusRow("i1", "s1"));

		// WHEN
		// new session established before the old session's close is processed
		connect(userId, cpId, "i1", "s2");
		disconnect(userId, cpId, "i1", "s1");
		flushAfterDelay();

		// THEN
		// @formatter:off
		and.then(rows.get(cpId))
			.as("Charger connected to the new session")
			.isEqualTo(new StatusRow("i1", "s2"))
			;
		// @formatter:on
	}

	@Test
	public void coalesce_staleDisconnectThenDisconnect() {
		// GIVEN
		final Map<String, StatusRow> rows = givenSimulatedStatusRows();
		final Long userId = randomLong();
		final String cpId = randomString();
		rows.put(cpId, new StatusRow("i1", "s1"));

		// WHEN
		// a late close for an older session, then the current session closes
		disconnect(userId, cpId, "i1", "s0");
		disconnect(userId, cpId, "i1", "s1");
		flushAfterDelay();

		// THEN
		// @formatter:off
		and.then(rows.get(cpId))
			.as("Charger disconnected")
			.isEqualTo(new StatusRow(null, null))
			;
		// @formatter:on
	}

	@Test
	public void coalesce_disconnectThenStaleDisconnect() {
		// GIVEN
		final Map<String, StatusRow> rows = givenSimulatedStatusRows();
		final Long userId = randomLong();
		final String cpId = randomString();
		rows.put(cpId, new StatusRow("i1", "s1"));

		// WHEN
		// the current session closes, then a late close for an older session
		disconnect(userId, cpId, "i1", "s1");
		disconnect(userId, cpId, "i1", "s0");
		flushAfterDelay();

		// THEN
		// @formatter:off
		and.then(rows.get(cpId))
			.as("Charger disconnected")
			.isEqualTo(new StatusRow(null, null))
			;
		// @formatter:on
	}

	@Test
	public void coalesce_latestWrittenAtFirstDeadline() {
		// GIVEN
		final Map<String, StatusRow> rows = givenSimulatedStatusRows();
		final Long userId = randomLong();
		final String cpId = randomString();

		// WHEN
		connect(userId, cpId, "i1", "s1");
		clock.add(dao.getDelay().dividedBy(2));
		connect(userId, cpId, "i1", "s2");

		// flush when the first update is due, before the second update's own delay
		clock.add(dao.getDelay().dividedBy(2));
		dao.run();

		// THEN
		// @formatter:off
		and.then(rows.get(cpId))
			.as("Latest status written when first update due, so updates are not postponed")
			.isEqualTo(new StatusRow("i1", "s2"))
			;
		// @formatter:on
	}

	@Test
	public void shutdown_flushesPendingBeforeDelay() {
		// GIVEN
		final Map<String, StatusRow> rows = givenSimulatedStatusRows();
		final Long userId = randomLong();
		final String cpId = randomString();
		rows.put(cpId, new StatusRow("i1", "s1"));

		// WHEN
		// charger disconnected as the app shuts down
		disconnect(userId, cpId, "i1", "s1");
		dao.serviceDidShutdown();

		// THEN
		// @formatter:off
		and.then(rows.get(cpId))
			.as("Pending status written on shutdown, even though its delay has not elapsed")
			.isEqualTo(new StatusRow(null, null))
			;
		// @formatter:on
	}

}
