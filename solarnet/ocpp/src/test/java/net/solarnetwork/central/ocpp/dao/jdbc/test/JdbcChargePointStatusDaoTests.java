/* ==================================================================
 * JdbcChargePointStatusDaoTests.java - 17/11/2022 11:50:47 am
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.ocpp.dao.BasicOcppCriteria;
import net.solarnetwork.central.ocpp.dao.jdbc.JdbcChargePointStatusDao;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link JdbcChargePointStatusDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargePointStatusDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private Long TEST_USER_ID = UUID.randomUUID().getMostSignificantBits();
	private Long TEST_NODE_ID = UUID.randomUUID().getMostSignificantBits();
	private Long TEST_CHARGER_ID = UUID.randomUUID().getMostSignificantBits();
	private String TEST_CHARGER_IDENT = UUID.randomUUID().toString();

	private MutableClock clock;
	private JdbcChargePointStatusDao dao;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.HOURS), ZoneOffset.UTC);
		dao = new JdbcChargePointStatusDao(jdbcTemplate);
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

	private void insertChargerStatus(Long userId, Long cpId, String connectedTo, Instant connectedDate) {
		jdbcTemplate.update(
				"""
						INSERT INTO solarev.ocpp_charge_point_status (user_id, cp_id, connected_to, connected_date)
						VALUES (?, ?, ?, ?)
						""",
				userId, cpId, connectedTo, Timestamp.from(connectedDate));
	}

	private List<Map<String, Object>> allChargePointStatusData() {
		List<Map<String, Object>> data = jdbcTemplate
				.queryForList("select * from solarev.ocpp_charge_point_status ORDER BY user_id, cp_id");
		log.debug("solarev.ocpp_charge_point_status table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	@Test
	public void update_connected() {
		// GIVEN
		final String instanceId = UUID.randomUUID().toString();
		final Instant connDate = Instant.now().truncatedTo(ChronoUnit.HOURS);

		// WHEN
		dao.updateConnectionStatus(TEST_USER_ID, TEST_CHARGER_IDENT, instanceId, connDate);

		// THEN
		List<Map<String, Object>> data = allChargePointStatusData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID matches", row, hasEntry("user_id", TEST_USER_ID));
		assertThat("Row charger ID matches", row, hasEntry("cp_id", TEST_CHARGER_ID));
		assertThat("Row connected to matches instance ID", row.get("connected_to"),
				is(equalTo(instanceId)));
		assertThat("Row connected date matches", row.get("connected_date"),
				is(equalTo(Timestamp.from(connDate))));
	}

	@Test
	public void update_disconnected() {
		// GIVEN
		final Instant connDate = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final String instanceId = UUID.randomUUID().toString();
		insertChargerStatus(TEST_USER_ID, TEST_CHARGER_ID, instanceId, connDate);

		// WHEN
		dao.updateConnectionStatus(TEST_USER_ID, TEST_CHARGER_IDENT, instanceId, null);

		// THEN
		List<Map<String, Object>> data = allChargePointStatusData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID matches", row, hasEntry("user_id", TEST_USER_ID));
		assertThat("Row charger ID matches", row, hasEntry("cp_id", TEST_CHARGER_ID));
		assertThat("Row connected to missing", row.get("connected_to"), is(nullValue()));
		assertThat("Row connected date unchanged", row.get("connected_date"),
				is(equalTo(Timestamp.from(connDate))));
	}

	@Test
	public void findFiltered_dateRange() {
		// GIVEN
		final Instant start = clock.instant();
		final int count = 5;
		final List<String> chargerIdents = new ArrayList<>(count);
		final List<String> instanceIds = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			String chargerIdent = UUID.randomUUID().toString();
			chargerIdents.add(chargerIdent);
			insertCharger(TEST_USER_ID, TEST_NODE_ID, (long) i, chargerIdent);
			String instanceId = UUID.randomUUID().toString();
			instanceIds.add(instanceId);
			dao.updateConnectionStatus(TEST_USER_ID, chargerIdent, instanceId, clock.instant());
			clock.add(1, ChronoUnit.SECONDS);
		}

		allChargePointStatusData();

		// WHEN
		BasicOcppCriteria f = new BasicOcppCriteria();
		f.setUserId(TEST_USER_ID);
		f.setStartDate(start.plusSeconds(1));
		f.setEndDate(start.plusSeconds(3));

		FilterResults<ChargePointStatus, UserLongCompositePK> result = dao.findFiltered(f);
		assertThat("Results returned for query", result, is(notNullValue()));
		assertThat("Results with a and b returned", result.getReturnedResultCount(), is(equalTo(2)));
		List<ChargePointStatus> resultList = StreamSupport.stream(result.spliterator(), false).toList();
		ChargePointStatus status = resultList.get(0);
		assertThat("Status 1 user ID", status.getUserId(), is(equalTo(TEST_USER_ID)));
		assertThat("Status 1 charger ID", status.getChargePointId(), is(equalTo(1L)));
		assertThat("Status 1 connector ID", status.getConnectedTo(), is(equalTo(instanceIds.get(1))));
		assertThat("Status 1 timestamp", status.getConnectedDate(), is(equalTo(start.plusSeconds(1))));

		status = resultList.get(1);
		assertThat("Status 2 user ID", status.getUserId(), is(equalTo(TEST_USER_ID)));
		assertThat("Status 2 charger ID", status.getChargePointId(), is(equalTo(2L)));
		assertThat("Status 1 connector ID", status.getConnectedTo(), is(equalTo(instanceIds.get(2))));
		assertThat("Status 1 timestamp", status.getConnectedDate(), is(equalTo(start.plusSeconds(2))));
	}

}
