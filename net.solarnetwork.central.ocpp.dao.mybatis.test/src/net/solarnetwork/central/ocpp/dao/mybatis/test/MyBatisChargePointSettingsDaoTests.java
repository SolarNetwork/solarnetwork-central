/* ==================================================================
 * MyBatisChargePointSettingsDaoTests.java - 27/02/2020 4:47:12 pm
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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisCentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisChargePointSettingsDao;
import net.solarnetwork.central.ocpp.dao.mybatis.MyBatisUserSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.RegistrationStatus;

/**
 * Test cases for the {@link MyBatisChargePointSettingsDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisChargePointSettingsDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisCentralChargePointDao chargePointDao;
	private MyBatisUserSettingsDao userSettingsDao;
	private MyBatisChargePointSettingsDao dao;

	private Long userId;
	private Long nodeId;
	private ChargePointSettings last;

	@Before
	public void setUp() throws Exception {
		chargePointDao = new MyBatisCentralChargePointDao();
		chargePointDao.setSqlSessionTemplate(getSqlSessionTemplate());
		userSettingsDao = new MyBatisUserSettingsDao();
		userSettingsDao.setSqlSessionTemplate(getSqlSessionTemplate());

		dao = new MyBatisChargePointSettingsDao();
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

	private CentralChargePoint createAndSaveTestChargePoint(String vendor, String model) {
		CentralChargePoint cp = createTestChargePoint(vendor, model);
		return (CentralChargePoint) chargePointDao.get(chargePointDao.save(cp));
	}

	private CentralChargePoint createTestChargePoint(String vendor, String model) {
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

	private ChargePointSettings createTestChargePointSettings(Long chargePointId) {
		ChargePointSettings s = new ChargePointSettings(chargePointId,
				Instant.ofEpochMilli(System.currentTimeMillis()));
		s.setPublishToSolarIn(false);
		s.setPublishToSolarFlux(true);
		return s;
	}

	@Test
	public void insert() {
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");
		ChargePointSettings entity = createTestChargePointSettings(cp.getId());
		Long pk = dao.save(entity);
		assertThat("PK preserved", pk, equalTo(entity.getId()));
		last = entity;
	}

	@Test
	public void insert_duplicate() {
		insert();
		ChargePointSettings entity = createTestChargePointSettings(last.getId());
		dao.save(entity);
		getSqlSessionTemplate().flushStatements();
	}

	@Test
	public void getByPK() {
		insert();
		ChargePointSettings entity = dao.get(last.getId());

		assertThat("ID", entity.getId(), equalTo(last.getId()));
		assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
		assertThat("Same", entity.isSameAs(last), equalTo(true));
	}

	@Test
	public void update() {
		insert();
		ChargePointSettings obj = dao.get(last.getId());
		obj.setPublishToSolarFlux(false);
		obj.setPublishToSolarIn(true);
		obj.setSourceIdTemplate("new-template");
		Long pk = dao.save(obj);
		assertThat("PK unchanged", pk, equalTo(obj.getId()));

		ChargePointSettings entity = dao.get(pk);
		assertThat("Entity updated", entity.isSameAs(obj), equalTo(true));
	}

	@Test
	public void findAll() {
		ChargePoint cp1 = createAndSaveTestChargePoint("foo", "bar");
		ChargePoint cp2 = createAndSaveTestChargePoint("bim", "bam");
		UUID uuid = UUID.randomUUID();
		Long nodeId2 = uuid.getLeastSignificantBits();
		setupTestNode(nodeId2);
		setupTestUserNode(userId, nodeId2);

		ChargePointSettings obj1 = createTestChargePointSettings(cp1.getId());
		obj1 = dao.get(dao.save(obj1));
		ChargePointSettings obj2 = new ChargePointSettings(cp2.getId(),
				obj1.getCreated().minusSeconds(60));
		obj2 = dao.get(dao.save(obj2));

		Collection<ChargePointSettings> results = dao.getAll(null);
		List<ChargePointSettings> expected = Arrays.asList(obj1, obj2);
		expected.sort(new Comparator<ChargePointSettings>() {

			@Override
			public int compare(ChargePointSettings o1, ChargePointSettings o2) {
				return o1.compareTo(o2.getId());
			}
		});
		assertThat("Results found in order", results, contains(expected.toArray()));
	}

	@Test
	public void resolveSettings_none() {
		ChargePointSettings entity = dao.resolveSettings(1L);
		assertThat("No settings resolved", entity, nullValue());
	}

	@Test
	public void resolveSettings_chargePointButNoSettings() {
		ChargePoint cp1 = createAndSaveTestChargePoint("foo", "bar");
		ChargePointSettings entity = dao.resolveSettings(cp1.getId());
		assertThat("No settings resolved", entity, nullValue());
	}

	@Test
	public void resolveSettings_onlyUser() {
		ChargePoint cp1 = createAndSaveTestChargePoint("foo", "bar");
		UserSettings us = new UserSettings(userId, Instant.now());
		userSettingsDao.save(us);
		ChargePointSettings entity = dao.resolveSettings(cp1.getId());
		assertThat("Settings resolved", entity, notNullValue());
		assertThat("Resolved values taken from user settings", entity.getSourceIdTemplate(),
				equalTo(us.getSourceIdTemplate()));
	}

	@Test
	public void resolveSettings_onlyChargePoint() {
		insert();
		ChargePointSettings entity = dao.resolveSettings(last.getId());
		assertThat("Settings resolved", entity, notNullValue());
		assertThat("Resolved values taken from charge point settings", entity.isSameAs(last),
				equalTo(true));
	}

	@Test
	public void resolveSettings_userAndChargePoint_defaulted() {
		insert();
		UserSettings us = new UserSettings(userId, Instant.now());
		us.setSourceIdTemplate("/bim/bam");
		userSettingsDao.save(us);
		ChargePointSettings entity = dao.resolveSettings(last.getId());
		assertThat("Settings resolved", entity, notNullValue());

		ChargePointSettings expected = new ChargePointSettings();
		expected.setPublishToSolarIn(last.isPublishToSolarIn());
		expected.setPublishToSolarFlux(last.isPublishToSolarFlux());
		expected.setSourceIdTemplate(us.getSourceIdTemplate());
		assertThat("Resolved values taken from charge point settings with user default",
				entity.isSameAs(expected), equalTo(true));
	}

	@Test
	public void resolveSettings_userAndChargePoint_overridden() {
		insert();
		last.setSourceIdTemplate("/foo/bar");
		dao.save(last);

		UserSettings us = new UserSettings(userId, Instant.now());
		us.setSourceIdTemplate("/bim/bam");
		userSettingsDao.save(us);

		ChargePointSettings entity = dao.resolveSettings(last.getId());
		assertThat("Settings resolved", entity, notNullValue());
		assertThat("Resolved values taken from charge point settings", entity.isSameAs(last),
				equalTo(true));
	}
}
