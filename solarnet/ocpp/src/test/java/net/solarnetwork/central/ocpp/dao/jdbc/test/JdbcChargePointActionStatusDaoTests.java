/* ==================================================================
 * JdbcChargePointActionStatusDaoTests.java - 18/11/2022 6:15:48 am
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

package net.solarnetwork.central.ocpp.dao.jdbc.test;

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.ocpp.dao.BasicOcppCriteria;
import net.solarnetwork.central.ocpp.dao.jdbc.JdbcChargePointActionStatusDao;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatus;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatusKey;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link JdbcChargePointActionStatusDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargePointActionStatusDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private Long TEST_USER_ID = UUID.randomUUID().getMostSignificantBits();
	private Long TEST_NODE_ID = UUID.randomUUID().getMostSignificantBits();
	private Long TEST_CHARGER_ID = UUID.randomUUID().getMostSignificantBits();
	private String TEST_CHARGER_IDENT = UUID.randomUUID().toString();

	private MutableClock clock;
	private JdbcChargePointActionStatusDao dao;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.HOURS), ZoneOffset.UTC);
		dao = new JdbcChargePointActionStatusDao(jdbcTemplate);
		setupTestUser(TEST_USER_ID);
		setupTestLocation();
		setupTestNode(TEST_NODE_ID);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID);
		insertCharger(TEST_USER_ID, TEST_NODE_ID, TEST_CHARGER_ID, TEST_CHARGER_IDENT);
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

	@Test
	public void insert_nullConnector() {
		// GIVEN
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		// WHEN
		dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, null, action, messageId, ts);

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

		// WHEN
		dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, null, action, messageId, ts);

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
	}

	@Test
	public void insert() {
		// GIVEN
		final var connId = 1;
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		// WHEN
		dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, connId, action, messageId, ts);

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
	}

	@Test
	public void insert_evse() {
		// GIVEN
		final var evseId = 1;
		final var connId = 2;
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		// WHEN
		dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, evseId, connId, action, messageId,
				ts);

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

		// WHEN
		dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, connId, action, messageId, ts);

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

		// WHEN
		dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, evseId, connId, action, messageId,
				ts);

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
	}

	@Test
	public void findFiltered_dateRange() {
		// GIVEN
		final Instant start = clock.instant();
		final int count = 5;
		final List<String> messageIds = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			String messageId = UUID.randomUUID().toString();
			messageIds.add(messageId);
			dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, i, "Action%d".formatted(i + 1),
					messageId, clock.instant());
			clock.add(1, ChronoUnit.SECONDS);
		}

		allChargePointActionStatusData();

		// WHEN
		BasicOcppCriteria f = new BasicOcppCriteria();
		f.setUserId(TEST_USER_ID);
		f.setStartDate(start.plusSeconds(1));
		f.setEndDate(start.plusSeconds(3));

		FilterResults<ChargePointActionStatus, ChargePointActionStatusKey> result = dao.findFiltered(f);
		assertThat("Results returned for query", result, is(notNullValue()));
		assertThat("Results with a and b returned", result.getReturnedResultCount(), is(equalTo(2)));
		List<ChargePointActionStatus> resultList = StreamSupport.stream(result.spliterator(), false)
				.toList();
		ChargePointActionStatus status = resultList.get(0);
		assertThat("Status 1 user ID", status.getUserId(), is(equalTo(TEST_USER_ID)));
		assertThat("Status 1 charger ID", status.getChargePointId(), is(equalTo(TEST_CHARGER_ID)));
		assertThat("Status 1 connector ID", status.getConnectorId(), is(equalTo(1)));
		assertThat("Status 1 action", status.getAction(), is(equalTo("Action2")));
		assertThat("Status 1 message ID", status.getMessageId(), is(equalTo(messageIds.get(1))));
		assertThat("Status 1 timestamp", status.getTimestamp(), is(equalTo(start.plusSeconds(1))));

		status = resultList.get(1);
		assertThat("Status 2 user ID", status.getUserId(), is(equalTo(TEST_USER_ID)));
		assertThat("Status 2 charger ID", status.getChargePointId(), is(equalTo(TEST_CHARGER_ID)));
		assertThat("Status 2 connector ID", status.getConnectorId(), is(equalTo(2)));
		assertThat("Status 2 action", status.getAction(), is(equalTo("Action3")));
		assertThat("Status 2 message ID", status.getMessageId(), is(equalTo(messageIds.get(2))));
		assertThat("Status 2 timestamp", status.getTimestamp(), is(equalTo(start.plusSeconds(2))));
	}

	@Test
	public void findFiltered_dateRange_evse() {
		// GIVEN
		final Instant start = clock.instant();
		final int count = 5;
		for ( int evseId = 1; evseId <= count; evseId++ ) {
			for ( int connId = 1; connId <= count; connId++ ) {
				dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, evseId, connId,
						"Action%d.%d".formatted(evseId, connId),
						"Message%d.%d".formatted(evseId, connId), clock.instant());
				clock.add(1, ChronoUnit.SECONDS);
			}
		}

		allChargePointActionStatusData();

		// WHEN
		final int evseId = 2;
		BasicOcppCriteria f = new BasicOcppCriteria();
		f.setUserId(TEST_USER_ID);
		f.setEvseId(evseId);

		FilterResults<ChargePointActionStatus, ChargePointActionStatusKey> result = dao.findFiltered(f);
		// @formatter:off
		then(result)
			.as("Results returned for query")
			.isNotNull()
			.as("Results for all connectors in EVSE returned")
			.hasSize(count)
			;
		// @formatter:on

		for ( int i = 0; i < count; i++ ) {
			// @formatter:off
			then(result).element(i)
				.as("User ID")
				.returns(TEST_USER_ID, from(ChargePointActionStatus::getUserId))
				.as("Charger ID")
				.returns(TEST_CHARGER_ID, from(ChargePointActionStatus::getChargePointId))
				.as("EVSE ID")
				.returns(evseId, from(ChargePointActionStatus::getEvseId))
				.as("Results ordered by connector ID")
				.returns(i+1, from(ChargePointActionStatus::getConnectorId))
				.as("Action")
				.returns("Action%d.%d".formatted(evseId, i+1), from(ChargePointActionStatus::getAction))
				.as("Message ID")
				.returns("Message%d.%d".formatted(evseId, i+1), from(ChargePointActionStatus::getMessageId))
				.as("Timestamp")
				.returns(start.plusSeconds(5*(evseId - 1)+i), from(ChargePointActionStatus::getTimestamp))
				;
		}
	}

}
