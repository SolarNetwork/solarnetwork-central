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

import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
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
 * @version 2.0
 */
public class MyBatisChargePointSettingsDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisCentralChargePointDao chargePointDao;
	private MyBatisUserSettingsDao userSettingsDao;
	private MyBatisChargePointSettingsDao dao;

	private Long userId;
	private Long nodeId;
	private ChargePointSettings last;

	@BeforeEach
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
		return createAndSaveTestChargePoint(vendor, model, userId, nodeId);
	}

	private CentralChargePoint createAndSaveTestChargePoint(String vendor, String model, Long userId,
			Long nodeId) {
		CentralChargePoint cp = createTestChargePoint(vendor, model, userId, nodeId);
		return (CentralChargePoint) chargePointDao.get(chargePointDao.save(cp));
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

	private ChargePointSettings createTestChargePointSettings(Long chargePointId, Long userId) {
		ChargePointSettings s = new ChargePointSettings(chargePointId, userId,
				Instant.ofEpochMilli(System.currentTimeMillis()));
		s.setPublishToSolarIn(false);
		s.setPublishToSolarFlux(true);
		return s;
	}

	@Test
	public void insert() {
		ChargePoint cp = createAndSaveTestChargePoint("foo", "bar");
		ChargePointSettings entity = createTestChargePointSettings(cp.getId(), userId);
		Long pk = dao.save(entity);
		assertThat("PK preserved", pk, equalTo(entity.getId()));
		last = entity;
	}

	@Test
	public void insert_duplicate() {
		insert();
		ChargePointSettings entity = createTestChargePointSettings(last.getId(), userId);
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
	public void update_wrongUser() {
		insert();
		ChargePointSettings obj = dao.get(last.getId());

		ChargePointSettings bad = new ChargePointSettings(obj.getChargePointId(), -1L, obj.getCreated());
		bad.setPublishToSolarFlux(false);
		bad.setPublishToSolarIn(true);
		bad.setSourceIdTemplate("new-template");
		Long pk = dao.save(bad);
		assertThat("PK unchanged", pk, equalTo(obj.getId()));

		ChargePointSettings entity = dao.get(pk);
		assertThat("Entity NOT updated from mis-matched user ID", entity.isSameAs(obj), equalTo(true));
	}

	@Test
	public void findAll() {
		ChargePoint cp1 = createAndSaveTestChargePoint("foo", "bar");
		ChargePoint cp2 = createAndSaveTestChargePoint("bim", "bam");
		UUID uuid = UUID.randomUUID();
		Long nodeId2 = uuid.getLeastSignificantBits();
		setupTestNode(nodeId2);
		setupTestUserNode(userId, nodeId2);

		ChargePointSettings obj1 = createTestChargePointSettings(cp1.getId(), userId);
		obj1 = dao.get(dao.save(obj1));
		ChargePointSettings obj2 = new ChargePointSettings(cp2.getId(), userId,
				obj1.getCreated().minusSeconds(60));
		obj2 = dao.get(dao.save(obj2));

		Collection<ChargePointSettings> results = dao.getAll(null);
		List<ChargePointSettings> expected = Arrays.asList(obj1, obj2);
		Collections.sort(expected);
		assertThat("Results found in order", results, contains(expected.toArray()));
	}

	@Test
	public void findAllForOwner() {
		ChargePoint cp1 = createAndSaveTestChargePoint("foo", "bar");
		ChargePoint cp2 = createAndSaveTestChargePoint("bim", "bam");
		UUID uuid = UUID.randomUUID();
		Long nodeId2 = uuid.getLeastSignificantBits();
		setupTestNode(nodeId2);
		setupTestUserNode(userId, nodeId2);

		ChargePointSettings obj1 = createTestChargePointSettings(cp1.getId(), userId);
		obj1 = dao.get(dao.save(obj1));
		ChargePointSettings obj2 = new ChargePointSettings(cp2.getId(), userId,
				obj1.getCreated().minusSeconds(60));
		obj2 = dao.get(dao.save(obj2));

		Long userId2 = userId - 1;
		Long nodeId3 = nodeId - 1;
		setupTestUser(userId2);
		setupTestNode(nodeId3);
		setupTestUserNode(userId2, nodeId3);
		ChargePoint cp3 = createAndSaveTestChargePoint("foo", "bar", userId2, nodeId3);
		ChargePointSettings obj3 = createTestChargePointSettings(cp3.getId(), userId2);
		obj3 = dao.get(dao.save(obj3));

		Collection<ChargePointSettings> results = dao.findAllForOwner(userId);
		List<ChargePointSettings> expected = Arrays.asList(obj1, obj2);
		Collections.sort(expected);
		assertThat("Results found in order", results, contains(expected.toArray()));
	}

	@Test
	public void resolveSettings_none() {
		ChargePointSettings entity = dao.resolveSettings(userId, 1L);
		assertThat("No settings resolved", entity, nullValue());
	}

	@Test
	public void resolveSettings_chargePointButNoSettings() {
		ChargePoint cp1 = createAndSaveTestChargePoint("foo", "bar");
		ChargePointSettings entity = dao.resolveSettings(userId, cp1.getId());
		assertThat("No settings resolved", entity, nullValue());
	}

	@Test
	public void resolveSettings_onlyUser() {
		ChargePoint cp1 = createAndSaveTestChargePoint("foo", "bar");
		UserSettings us = new UserSettings(userId, Instant.now());
		userSettingsDao.save(us);
		ChargePointSettings entity = dao.resolveSettings(userId, cp1.getId());
		assertThat("Settings resolved", entity, notNullValue());
		assertThat("Resolved values taken from user settings", entity.getSourceIdTemplate(),
				equalTo(us.getSourceIdTemplate()));
	}

	@Test
	public void resolveSettings_onlyChargePoint() {
		insert();
		ChargePointSettings entity = dao.resolveSettings(userId, last.getId());
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
		ChargePointSettings entity = dao.resolveSettings(userId, last.getId());
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

		ChargePointSettings entity = dao.resolveSettings(userId, last.getId());
		assertThat("Settings resolved", entity, notNullValue());
		assertThat("Resolved values taken from charge point settings", entity.isSameAs(last),
				equalTo(true));
	}

	@Test
	public void findByUserAndId() {
		insert();
		ChargePointSettings entity = dao.get(userId, last.getId());
		assertThat("Match", entity, equalTo(last));
		assertThat("User ID", entity.getUserId(), equalTo(userId));
	}

	@Test
	public void findByUserAndId_noMatch() {
		insert();
		thenExceptionOfType(DataRetrievalFailureException.class)
				.isThrownBy(() -> dao.get(userId, last.getId() - 1));
	}

	@Test
	public void deleteByUserAndId() {
		insert();
		dao.delete(userId, last.getId());
		assertThat("No longer found", dao.get(last.getId()), nullValue());
	}

	@Test
	public void deleteByUserAndId_noMatch() {
		insert();
		thenExceptionOfType(DataRetrievalFailureException.class)
				.isThrownBy(() -> dao.delete(userId, last.getId() - 1));
	}

}
