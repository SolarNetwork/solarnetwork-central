/* ==================================================================
 * AsyncJdbcChargePointActionStatusDaoTests.java - 15/05/2024 9:06:00 am
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

package net.solarnetwork.central.ocpp.dao.jdbc.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;
import net.solarnetwork.central.ocpp.dao.jdbc.AsyncJdbcChargePointActionStatusCount;
import net.solarnetwork.central.ocpp.dao.jdbc.AsyncJdbcChargePointActionStatusDao;
import net.solarnetwork.central.ocpp.dao.jdbc.ChargePointActionStatusUpdate;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.util.StatCounter;

/**
 * Test cases for the {@link AsyncJdbcChargePointActionStatusDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AsyncJdbcChargePointActionStatusDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static Long TEST_USER_ID = UUID.randomUUID().getMostSignificantBits();
	private static Long TEST_CHARGER_ID = UUID.randomUUID().getMostSignificantBits();
	private static String TEST_CHARGER_IDENT = UUID.randomUUID().toString();

	@Autowired
	protected DataSource dataSource;

	private BlockingQueue<ChargePointActionStatusUpdate> queue;
	private AsyncJdbcChargePointActionStatusDao dao;
	private StatCounter statCounter;

	@BeforeEach
	public void setup() {
		queue = new LinkedBlockingQueue<>();
		statCounter = new StatCounter("TestChargePointActionStatusUpdater", "", log, 1,
				AsyncJdbcChargePointActionStatusCount.values());
		dao = new AsyncJdbcChargePointActionStatusDao(dataSource, queue, statCounter);

		setupTestUser(TEST_USER_ID);
		setupTestLocation();
		setupTestNode(TEST_NODE_ID);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID);
		insertCharger(TEST_USER_ID, TEST_NODE_ID, TEST_CHARGER_ID, TEST_CHARGER_IDENT);
	}

	@AfterEach
	public void teardown() {
		dao.serviceDidShutdown();
	}

	private void insertCharger(Long userId, Long nodeId, Long cpId, String cpIdent) {
		jdbcTemplate.update("""
				INSERT INTO solarev.ocpp_charge_point (id, user_id, node_id, ident, vendor, model)
				VALUES (?, ?, ?, ?, 'Test', 'Test')
				""", cpId, userId, nodeId, cpIdent);
	}

	private void insertChargerActionStatus(Long userId, Long cpId, int evseId, int connectorId,
			String action, String messageId, Instant ts) {
		jdbcTemplate.update(
				"""
						INSERT INTO solarev.ocpp_charge_point_action_status (user_id, cp_id, evse_id, conn_id, action, msg_id, ts)
						VALUES (?, ?, ?, ?, ?, ?, ?)
						""",
				userId, cpId, evseId, connectorId, action, messageId, Timestamp.from(ts));
	}

	private List<Map<String, Object>> allChargePointActionStatusData() {
		List<Map<String, Object>> data = jdbcTemplate.queryForList(
				"select * from solarev.ocpp_charge_point_action_status ORDER BY user_id, cp_id, evse_id, conn_id, action");
		log.debug("solarev.ocpp_charge_point_action_status table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	public static interface TestFunction {

		void apply();
	}

	private void test(TestFunction action) {
		try {
			action.apply();
		} finally {
			jdbcTemplate.execute("delete from solarev.ocpp_charge_point_action_status");
			jdbcTemplate.execute("delete from solarev.ocpp_charge_point");
			jdbcTemplate.execute("delete from solaruser.user_node");
			jdbcTemplate.execute("delete from solarnet.sn_node");
			jdbcTemplate.execute("delete from solarnet.sn_loc");
			jdbcTemplate.execute("delete from solaruser.user_user");
		}
	}

	private void thenAssertAllProcessed(long added, long updated, long failed) {
		then(queue).as("Queue emptied").isEmpty();
		then(statCounter).as("ResultsAdded stat tracked")
				.returns(added, (s) -> s.get(AsyncJdbcChargePointActionStatusCount.ResultsAdded))
				.as("UpdatesExecuted stat tracked")
				.returns(updated, (s) -> s.get(AsyncJdbcChargePointActionStatusCount.UpdatesExecuted))
				.as("UpdatesFailed stat tracked")
				.returns(failed, (s) -> s.get(AsyncJdbcChargePointActionStatusCount.UpdatesFailed));
	}

	@Test
	public void insert_nullConnector() {
		// GIVEN
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		TestTransaction.flagForCommit();
		TestTransaction.end();
		dao.serviceDidStartup();

		test(() -> {
			// WHEN
			dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, null, action, messageId, ts);
			dao.shutdownAndWait(Duration.ofSeconds(2));

			// THEN
			List<Map<String, Object>> data = allChargePointActionStatusData();
			assertThat("Table has 1 row", data, hasSize(1));
			Map<String, Object> row = data.get(0);
			assertThat("Row user ID matches", row, hasEntry("user_id", TEST_USER_ID));
			assertThat("Row charger ID matches", row, hasEntry("cp_id", TEST_CHARGER_ID));
			assertThat("Row connector ID matches", row, hasEntry("conn_id", 0));
			assertThat("Row action matches", row.get("action"), is(equalTo(action)));
			assertThat("Row message ID matches", row.get("msg_id"), is(equalTo(messageId)));
			assertThat("Row timestamp matches", row.get("ts"), is(equalTo(Timestamp.from(ts))));

			thenAssertAllProcessed(1L, 1L, 0L);
		});
	}

	@Test
	public void update_nullConnector() {
		// GIVEN
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		// insert earlier record
		insertChargerActionStatus(TEST_USER_ID, TEST_CHARGER_ID, 0, 0, action,
				UUID.randomUUID().toString(), ts.minusSeconds(1));

		TestTransaction.flagForCommit();
		TestTransaction.end();
		dao.serviceDidStartup();

		test(() -> {
			// WHEN
			dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, null, action, messageId, ts);
			dao.shutdownAndWait(Duration.ofSeconds(2));

			// THEN
			List<Map<String, Object>> data = allChargePointActionStatusData();
			assertThat("Table has 1 row", data, hasSize(1));
			Map<String, Object> row = data.get(0);
			assertThat("Row user ID matches", row, hasEntry("user_id", TEST_USER_ID));
			assertThat("Row charger ID matches", row, hasEntry("cp_id", TEST_CHARGER_ID));
			assertThat("Row evse ID matches", row, hasEntry("evse_id", 0));
			assertThat("Row connector ID matches", row, hasEntry("conn_id", 0));
			assertThat("Row action matches", row.get("action"), is(equalTo(action)));
			assertThat("Row message ID matches", row.get("msg_id"), is(equalTo(messageId)));
			assertThat("Row timestamp matches", row.get("ts"), is(equalTo(Timestamp.from(ts))));

			thenAssertAllProcessed(1L, 1L, 0L);
		});
	}

	@Test
	public void insert() {
		// GIVEN
		final var connId = 1;
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		TestTransaction.flagForCommit();
		TestTransaction.end();
		dao.serviceDidStartup();

		test(() -> {

			// WHEN
			dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, connId, action, messageId, ts);
			dao.shutdownAndWait(Duration.ofSeconds(2));

			// THEN
			List<Map<String, Object>> data = allChargePointActionStatusData();
			assertThat("Table has 1 row", data, hasSize(1));
			Map<String, Object> row = data.get(0);
			assertThat("Row user ID matches", row, hasEntry("user_id", TEST_USER_ID));
			assertThat("Row charger ID matches", row, hasEntry("cp_id", TEST_CHARGER_ID));
			assertThat("Row evse ID defaulted", row, hasEntry("evse_id", 0));
			assertThat("Row connector ID matches", row, hasEntry("conn_id", connId));
			assertThat("Row action matches", row.get("action"), is(equalTo(action)));
			assertThat("Row message ID matches", row.get("msg_id"), is(equalTo(messageId)));
			assertThat("Row timestamp matches", row.get("ts"), is(equalTo(Timestamp.from(ts))));

			thenAssertAllProcessed(1L, 1L, 0L);
		});
	}

	@Test
	public void insert_evse() {
		// GIVEN
		final var evseId = 1;
		final var connId = 2;
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		TestTransaction.flagForCommit();
		TestTransaction.end();
		dao.serviceDidStartup();

		test(() -> {

			// WHEN
			dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, evseId, connId, action,
					messageId, ts);
			dao.shutdownAndWait(Duration.ofSeconds(2));

			// THEN
			List<Map<String, Object>> data = allChargePointActionStatusData();
			assertThat("Table has 1 row", data, hasSize(1));
			Map<String, Object> row = data.get(0);
			assertThat("Row user ID matches", row, hasEntry("user_id", TEST_USER_ID));
			assertThat("Row charger ID matches", row, hasEntry("cp_id", TEST_CHARGER_ID));
			assertThat("Row evse ID matches", row, hasEntry("evse_id", evseId));
			assertThat("Row connector ID matches", row, hasEntry("conn_id", connId));
			assertThat("Row action matches", row.get("action"), is(equalTo(action)));
			assertThat("Row message ID matches", row.get("msg_id"), is(equalTo(messageId)));
			assertThat("Row timestamp matches", row.get("ts"), is(equalTo(Timestamp.from(ts))));

			thenAssertAllProcessed(1L, 1L, 0L);
		});
	}

	@Test
	public void update() {
		// GIVEN
		final var connId = 1;
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		// insert earlier record
		insertChargerActionStatus(TEST_USER_ID, TEST_CHARGER_ID, 0, connId, action,
				UUID.randomUUID().toString(), ts.minusSeconds(1));

		TestTransaction.flagForCommit();
		TestTransaction.end();
		dao.serviceDidStartup();

		test(() -> {
			// WHEN
			dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, connId, action, messageId, ts);
			dao.shutdownAndWait(Duration.ofSeconds(2));

			// THEN
			List<Map<String, Object>> data = allChargePointActionStatusData();
			assertThat("Table has 1 row", data, hasSize(1));
			Map<String, Object> row = data.get(0);
			assertThat("Row user ID matches", row, hasEntry("user_id", TEST_USER_ID));
			assertThat("Row charger ID matches", row, hasEntry("cp_id", TEST_CHARGER_ID));
			assertThat("Row evse ID defaulted", row, hasEntry("evse_id", 0));
			assertThat("Row connector ID matches", row, hasEntry("conn_id", connId));
			assertThat("Row action matches", row.get("action"), is(equalTo(action)));
			assertThat("Row message ID matches", row.get("msg_id"), is(equalTo(messageId)));
			assertThat("Row timestamp matches", row.get("ts"), is(equalTo(Timestamp.from(ts))));

			thenAssertAllProcessed(1L, 1L, 0L);
		});
	}

	@Test
	public void update_evse() {
		// GIVEN
		final var evseId = 1;
		final var connId = 2;
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		// insert earlier record
		insertChargerActionStatus(TEST_USER_ID, TEST_CHARGER_ID, evseId, connId, action,
				UUID.randomUUID().toString(), ts.minusSeconds(1));

		TestTransaction.flagForCommit();
		TestTransaction.end();
		dao.serviceDidStartup();

		test(() -> {
			// WHEN
			dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, evseId, connId, action,
					messageId, ts);
			dao.shutdownAndWait(Duration.ofSeconds(2));

			// THEN
			List<Map<String, Object>> data = allChargePointActionStatusData();
			assertThat("Table has 1 row", data, hasSize(1));
			Map<String, Object> row = data.get(0);
			assertThat("Row user ID matches", row, hasEntry("user_id", TEST_USER_ID));
			assertThat("Row charger ID matches", row, hasEntry("cp_id", TEST_CHARGER_ID));
			assertThat("Row evse ID matches", row, hasEntry("evse_id", evseId));
			assertThat("Row connector ID matches", row, hasEntry("conn_id", connId));
			assertThat("Row action matches", row.get("action"), is(equalTo(action)));
			assertThat("Row message ID matches", row.get("msg_id"), is(equalTo(messageId)));
			assertThat("Row timestamp matches", row.get("ts"), is(equalTo(Timestamp.from(ts))));

			thenAssertAllProcessed(1L, 1L, 0L);
		});
	}

	@Test
	public void threaded() {
		// GIVEN
		final var evseId = 1;
		final var connId = 2;

		// populate a handful of actions
		final int actionCount = 5;
		final var actions = new ArrayList<String>(actionCount);
		for ( int i = 0; i < actionCount; i++ ) {
			actions.add(randomString());
		}

		// populate some chargers
		final int chargerCount = 50;
		final var chargerIdentifiers = new ArrayList<String>(chargerCount);
		for ( int i = 0; i < chargerCount; i++ ) {
			String ident = randomString();
			insertCharger(TEST_USER_ID, TEST_NODE_ID, (long) i, ident);
			chargerIdentifiers.add(ident);
		}

		TestTransaction.flagForCommit();
		TestTransaction.end();
		dao.serviceDidStartup();

		final RandomGenerator rng = new SecureRandom();
		final int taskCount = 1000;
		final ExecutorService threadPool = Executors.newFixedThreadPool(8);
		final Map<String, Map<String, String>> chargerToActionToMessageIdMap = new HashMap<>(100);
		try {

			for ( int i = 0; i < taskCount; i++ ) {
				threadPool.submit(() -> {
					String ident = chargerIdentifiers.get(rng.nextInt(chargerCount));
					String action = actions.get(rng.nextInt(actionCount));
					String messageId = UUID.randomUUID().toString();
					synchronized ( chargerToActionToMessageIdMap ) {
						dao.updateActionTimestamp(TEST_USER_ID, ident, evseId, connId, action, messageId,
								Instant.now());
						chargerToActionToMessageIdMap.computeIfAbsent(ident, k -> new HashMap<>())
								.put(action, messageId);
					}
				});
			}

			test(() -> {
				// WHEN
				while ( queue.peek() != null ) {
					try {
						Thread.sleep(Duration.ofSeconds(1));
					} catch ( InterruptedException e ) {
						// ignore
					}
				}
				dao.shutdownAndWait(Duration.ofSeconds(2));

				// THEN
				final int expectedRowCount = (int) chargerToActionToMessageIdMap.values().stream()
						.flatMap(m -> m.values().stream()).count();
				List<Map<String, Object>> data = allChargePointActionStatusData();
				assertThat("Table has expected row count", data, hasSize(expectedRowCount));
				for ( var row : data ) {
					String rowChargerIdent = chargerIdentifiers
							.get(((Long) row.get("cp_id")).intValue());
					String rowAction = (String) row.get("action");
					then(row).as("Row user ID matches").containsEntry("user_id", TEST_USER_ID)
							.as("Row message ID matches last value provided").containsEntry("msg_id",
									chargerToActionToMessageIdMap.get(rowChargerIdent).get(rowAction));
				}

				thenAssertAllProcessed(taskCount, taskCount, 0L);
			});
		} finally {
			threadPool.shutdownNow();
		}
	}
}
