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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
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
 * @version 1.0
 */
public class MyBatisCentralChargePointConnectorDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisCentralChargePointDao chargePointDao;
	private MyBatisCentralChargePointConnectorDao dao;

	private Long userId;
	private CentralChargePointConnector last;

	@Before
	public void setUp() throws Exception {
		chargePointDao = new MyBatisCentralChargePointDao();
		chargePointDao.setSqlSessionTemplate(getSqlSessionTemplate());

		dao = new MyBatisCentralChargePointConnectorDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());
		last = null;
		userId = UUID.randomUUID().getMostSignificantBits();
		setupTestUser(userId);
	}

	private CentralChargePoint createTestChargePoint(String vendor, String model) {
		ChargePointInfo info = new ChargePointInfo(UUID.randomUUID().toString());
		info.setChargePointVendor(vendor);
		info.setChargePointModel(model);
		CentralChargePoint cp = new CentralChargePoint(null, userId,
				Instant.ofEpochMilli(System.currentTimeMillis()), info);
		cp.setEnabled(true);
		cp.setRegistrationStatus(RegistrationStatus.Accepted);
		cp.setConnectorCount(2);
		return cp;
	}

	private CentralChargePoint createAndSaveTestChargePoint(String vendor, String model) {
		CentralChargePoint cp = createTestChargePoint(vendor, model);
		return (CentralChargePoint) chargePointDao.get(chargePointDao.save(cp));
	}

	@Test
	public void insert() {
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");

		CentralChargePointConnector cpc = new CentralChargePointConnector(
				new ChargePointConnectorKey(cp.getId(), 1),
				Instant.ofEpochMilli(System.currentTimeMillis()));
		// @formatter:off
		cpc.setInfo(StatusNotification.builder()
				.withConnectorId(cpc.getId().getConnectorId())
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.ofEpochMilli(System.currentTimeMillis())).build());
		// @formatter:on
		ChargePointConnectorKey pk = dao.save(cpc);
		assertThat("PK preserved", pk, equalTo(cpc.getId()));
		last = cpc;
	}

	@Test
	public void getByPK() {
		insert();
		ChargePointConnector entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Connector info", entity.getInfo(), equalTo(last.getInfo()));
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
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");

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

}
