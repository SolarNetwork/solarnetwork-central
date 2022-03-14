/* ==================================================================
 * MyBatisCentralChargePointConnectorDaoTests.java - 26/02/2020 9:03:49 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.dao.mybatis.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataRetrievalFailureException;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointConnector;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargePointErrorCode;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.ChargePointStatus;
import net.solarnetwork.ocpp.domain.RegistrationStatus;
import net.solarnetwork.ocpp.domain.StatusNotification;

/**
 * Test cases for the {@link MyBatisCentralChargePointConnectorDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisCentralChargePointConnectorDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisCentralChargePointDao chargePointDao;
	private MyBatisCentralChargePointConnectorDao dao;

	private Long userId;
	private Long nodeId;
	private CentralChargePointConnector last;

	@Before
	public void setUp() throws Exception {
		chargePointDao = new MyBatisCentralChargePointDao();
		chargePointDao.setSqlSessionTemplate(getSqlSessionTemplate());

		dao = new MyBatisCentralChargePointConnectorDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());
		last = null;
		UUID uuid = UUID.randomUUID();
		userId = uuid.getMostSignificantBits();
		nodeId = uuid.getLeastSignificantBits();
		setupTestUser(userId);
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);
	}

	private CentralChargePoint createTestChargePoint(String vendor, String model, Long userId,
			Long nodeId) {
		ChargePointInfo info = new ChargePointInfo(UUID.randomUUID().toString());
		info.setChargePointVendor(vendor);
		info.setChargePointModel(model);
		CentralChargePoint cp = new CentralChargePoint(null, userId, nodeId,
				Instant.ofEpochMilli(System.currentTimeMillis()), info);
		cp.setEnabled(true);
		cp.setRegistrationStatus(RegistrationStatus.Accepted);
		cp.setConnectorCount(2);
		return cp;
	}

	private CentralChargePoint createAndSaveTestChargePoint(String vendor, String model, Long userId,
			Long nodeId) {
		CentralChargePoint cp = createTestChargePoint(vendor, model, userId, nodeId);
		return (CentralChargePoint) chargePointDao.get(chargePointDao.save(cp));
	}

	private CentralChargePointConnector createTestConnector(long chargePointId, int connectorId) {
		CentralChargePointConnector cpc = new CentralChargePointConnector(
				new ChargePointConnectorKey(chargePointId, connectorId),
				Instant.ofEpochMilli(System.currentTimeMillis()));
		// @formatter:off
		cpc.setInfo(StatusNotification.builder()
				.withConnectorId(cpc.getId().getConnectorId())
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.ofEpochMilli(System.currentTimeMillis())).build());
		// @formatter:on
		return cpc;
	}

	@Test
	public void insert() {
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar", userId, nodeId);

		CentralChargePointConnector cpc = createTestConnector(cp.getId(), 1);
		ChargePointConnectorKey pk = dao.save(cpc);
		assertThat("PK preserved", pk, equalTo(cpc.getId()));
		last = cpc;
	}

	@Test
	public void insert_withoutInfo() {
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar", userId, nodeId);

		CentralChargePointConnector cpc = createTestConnector(cp.getId(), 1);
		cpc.setInfo(null);

		ChargePointConnectorKey pk = dao.save(cpc);
		assertThat("PK preserved", pk, equalTo(cpc.getId()));
		last = cpc;
	}

	@Test
	public void getByPK() {
		insert();
		CentralChargePointConnector entity = (CentralChargePointConnector) dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("User ID", entity.getUserId(), equalTo(userId));
		assertThat("Connector info", entity.getInfo(), equalTo(last.getInfo()));
	}

	@Test
	public void getByPK_withoutInfo() {
		insert_withoutInfo();
		CentralChargePointConnector entity = (CentralChargePointConnector) dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("User ID", entity.getUserId(), equalTo(userId));
		assertThat("Connector info created with defaults", entity.getInfo(), notNullValue());
		assertThat("Info status", entity.getInfo().getStatus(), equalTo(ChargePointStatus.Unknown));
		assertThat("Info status", entity.getInfo().getTimestamp(), notNullValue());
	}

	@Test
	public void update() {
		insert();
		ChargePointConnector cpc = dao.get(last.getId());
		cpc.setInfo(cpc.getInfo().toBuilder().withStatus(ChargePointStatus.Unavailable)
				.withVendorId(UUID.randomUUID().toString()).build());
		ChargePointConnectorKey pk = dao.save(cpc);
		assertThat("PK unchanged", pk, equalTo(cpc.getId()));

		ChargePointConnector entity = dao.get(pk);
		assertThat("Info status updated", entity.getInfo(), equalTo(cpc.getInfo()));
	}

	@Test
	public void findAll() {
		insert();

		List<ChargePointConnectorKey> expectedKeys = new ArrayList<>();
		expectedKeys.add(last.getId());

		// add another for same charge point
		ChargePointConnector cpc = new ChargePointConnector(
				new ChargePointConnectorKey(last.getId().getChargePointId(), 2),
				Instant.ofEpochMilli(System.currentTimeMillis()));
		expectedKeys.add(cpc.getId());
		// @formatter:off
		cpc.setInfo(StatusNotification.builder()
				.withConnectorId(cpc.getId().getConnectorId())
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.ofEpochMilli(System.currentTimeMillis())).build());
		// @formatter:on
		dao.save(cpc);

		// add another for a different charge point
		insert();

		expectedKeys.add(last.getId());
		expectedKeys.sort(null);

		Collection<ChargePointConnector> results = dao.getAll(null);
		assertThat("Result keys",
				results.stream().map(ChargePointConnector::getId).collect(Collectors.toList()),
				equalTo(expectedKeys));
	}

	@Test
	public void findAllForOwner() {
		ChargePoint cp1 = createAndSaveTestChargePoint("foo", "bar", userId, nodeId);
		ChargePointConnector obj1 = createTestConnector(cp1.getId(), 1);
		obj1 = dao.get(dao.save(obj1));
		ChargePointConnector obj2 = createTestConnector(cp1.getId(), 2);
		obj2 = dao.get(dao.save(obj2));

		Long userId2 = userId - 1;
		Long nodeId2 = nodeId - 1;
		setupTestUser(userId2);
		setupTestNode(nodeId2);
		setupTestUserNode(userId2, nodeId2);
		ChargePoint cp2 = createAndSaveTestChargePoint("foo", "bar", userId2, nodeId2);
		ChargePointConnector obj3 = createTestConnector(cp2.getId(), 1);
		obj3 = dao.get(dao.save(obj3));

		List<ChargePointConnectorKey> expectedKeys = new ArrayList<>();
		expectedKeys.add(obj1.getId());
		expectedKeys.add(obj2.getId());
		expectedKeys.sort(null);

		Collection<CentralChargePointConnector> results = dao.findAllForOwner(userId);
		assertThat("Result keys",
				results.stream().map(ChargePointConnector::getId).collect(Collectors.toList()),
				equalTo(expectedKeys));
	}

	@Test
	public void findForOwnerAndChargePoint() {
		ChargePoint cp1 = createAndSaveTestChargePoint("foo", "bar", userId, nodeId);
		ChargePointConnector obj1 = createTestConnector(cp1.getId(), 1);
		obj1 = dao.get(dao.save(obj1));
		ChargePointConnector obj2 = createTestConnector(cp1.getId(), 2);
		obj2 = dao.get(dao.save(obj2));

		Long userId2 = userId - 1;
		Long nodeId2 = nodeId - 1;
		setupTestUser(userId2);
		setupTestNode(nodeId2);
		setupTestUserNode(userId2, nodeId2);
		ChargePoint cp2 = createAndSaveTestChargePoint("foo", "bar", userId2, nodeId2);
		ChargePointConnector obj3 = createTestConnector(cp2.getId(), 1);
		obj3 = dao.get(dao.save(obj3));

		List<ChargePointConnectorKey> expectedKeys = new ArrayList<>();
		expectedKeys.add(obj1.getId());
		expectedKeys.add(obj2.getId());
		expectedKeys.sort(null);

		// add another for a different charge point
		insert();

		Collection<CentralChargePointConnector> results = dao.findByChargePointId(userId, cp1.getId());
		assertThat("Result keys",
				results.stream().map(ChargePointConnector::getId).collect(Collectors.toList()),
				equalTo(expectedKeys));
	}

	@Test
	public void findForChargePoint() {
		insert();

		List<ChargePointConnectorKey> expectedKeys = new ArrayList<>();
		expectedKeys.add(last.getId());

		// add another for same charge point
		CentralChargePointConnector cpc = new CentralChargePointConnector(
				new ChargePointConnectorKey(last.getId().getChargePointId(), 2),
				Instant.ofEpochMilli(System.currentTimeMillis()));
		expectedKeys.add(cpc.getId());
		// @formatter:off
		cpc.setInfo(StatusNotification.builder()
				.withConnectorId(cpc.getId().getConnectorId())
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.ofEpochMilli(System.currentTimeMillis())).build());
		// @formatter:on
		dao.save(cpc);

		// add another for a different charge point
		insert();

		expectedKeys.sort(null);

		Collection<ChargePointConnector> results = dao
				.findByChargePointId(expectedKeys.get(0).getChargePointId());
		assertThat("Result keys",
				results.stream().map(ChargePointConnector::getId).collect(Collectors.toList()),
				equalTo(expectedKeys));
	}

	@Test
	public void updateStatus() {
		// given
		insert();

		// when
		int result = lastUpdateCount(dao.updateChargePointStatus(last.getId().getChargePointId(),
				last.getId().getConnectorId(), ChargePointStatus.Charging));

		// then
		assertThat("One row updated", result, equalTo(1));
		assertThat("Status updated", dao.get(last.getId()).getInfo().getStatus(),
				equalTo(ChargePointStatus.Charging));
	}

	@Test
	public void updateStatusForChargePoint() {
		// given
		insert();

		// add another for same charge point
		ChargePointConnector cpc = new ChargePointConnector(
				new ChargePointConnectorKey(last.getId().getChargePointId(), 2),
				Instant.ofEpochMilli(System.currentTimeMillis()));

		// @formatter:off
		cpc.setInfo(StatusNotification.builder()
				.withConnectorId(cpc.getId().getConnectorId())
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.ofEpochMilli(System.currentTimeMillis())).build());
		// @formatter:on
		dao.save(cpc);

		// add another for a different charge point, to verify we don't update this
		insert();

		// when
		int result = lastUpdateCount(dao.updateChargePointStatus(cpc.getId().getChargePointId(), 0,
				ChargePointStatus.Charging));

		// then
		assertThat("Two row updated", result, equalTo(2));
		assertThat("Status updated for charge point",
				dao.findByChargePointId(cpc.getId().getChargePointId()).stream()
						.map(c -> c.getInfo().getStatus()).collect(Collectors.toList()),
				contains(ChargePointStatus.Charging, ChargePointStatus.Charging));
		assertThat("Other charge point status unchanged", dao.get(last.getId()).getInfo().getStatus(),
				equalTo(last.getInfo().getStatus()));
	}

	@Test
	public void saveStatusInfo_insert() {
		// given
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar", userId, nodeId);

		// @formatter:off
		StatusNotification info = StatusNotification.builder()
				.withConnectorId(1)
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
				.build();
		// @formatter:on

		// when
		ChargePointConnectorKey pk = dao.saveStatusInfo(cp.getId(), info);

		// then
		assertThat("PK created", pk,
				equalTo(new ChargePointConnectorKey(cp.getId(), info.getConnectorId())));
		ChargePointConnector entity = dao.get(pk);
		assertThat("Info status created", entity.getInfo(), equalTo(info));
	}

	@Test
	public void saveStatusInfo_update() {
		// given
		insert();

		// @formatter:off
		StatusNotification info = last.getInfo().toBuilder()
				.withStatus(ChargePointStatus.Unavailable)
				.withErrorCode(ChargePointErrorCode.GroundFailure)
				.withTimestamp(Instant.ofEpochMilli(System.currentTimeMillis()))
				.build();
		// @formatter:on

		// when
		ChargePointConnectorKey pk = dao.saveStatusInfo(last.getId().getChargePointId(), info);

		// then
		assertThat("PK unchanged", pk, equalTo(last.getId()));

		ChargePointConnector entity = dao.get(pk);
		assertThat("Info status updated", entity.getInfo(), equalTo(info));
	}

	@Test
	public void insert_defaultStatus() {
		// given
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar", userId, nodeId);

		// when
		ChargePointConnector conn = new ChargePointConnector(new ChargePointConnectorKey(cp.getId(), 1),
				Instant.now());
		conn.setInfo(StatusNotification.builder().withConnectorId(1).withTimestamp(conn.getCreated())
				.build());
		ChargePointConnectorKey pk = dao.save(conn);
		getSqlSessionTemplate().flushStatements();

		// then
		assertThat("PK returned", pk, equalTo(new ChargePointConnectorKey(cp.getId(), 1)));

		ChargePointConnector entity = dao.get(pk);
		CentralChargePointConnector expected = new CentralChargePointConnector(conn);
		expected.setInfo(expected.getInfo().toBuilder().withStatus(ChargePointStatus.Unknown).build());
		assertThat("Status defaulted to Unknown", entity.isSameAs(expected), equalTo(true));
	}

	@Test
	public void findByUserAndId() {
		insert();
		CentralChargePointConnector entity = dao.get(userId, last.getId());
		assertThat("Match", entity, equalTo(last));
		assertThat("User ID", entity.getUserId(), equalTo(userId));
	}

	@Test(expected = DataRetrievalFailureException.class)
	public void findByUserAndId_noMatch() {
		insert();
		dao.get(userId, new ChargePointConnectorKey(last.getId().getChargePointId() - 1, 1));
	}

	@Test
	public void deleteByUserAndId() {
		insert();
		dao.delete(userId, last.getId());
		assertThat("No longer found", dao.get(last.getId()), nullValue());
	}

	@Test
	public void deleteById() {
		insert();
		dao.delete(last);
		assertThat("No longer found", dao.get(last.getId()), nullValue());
	}

	@Test(expected = DataRetrievalFailureException.class)
	public void deleteByUserAndId_noMatch() {
		insert();
		dao.delete(userId, new ChargePointConnectorKey(last.getId().getChargePointId() - 1, 1));
	}

}
