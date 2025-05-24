/* ==================================================================
 * DaoUserOcppBizTests.java - 29/02/2020 8:04:37 pm
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

package net.solarnetwork.central.user.ocpp.biz.dao.test;

import static java.util.Collections.singleton;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusDao;
import net.solarnetwork.central.ocpp.dao.UserSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.central.ocpp.domain.CentralChargeSession;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.central.user.ocpp.biz.dao.DaoUserOcppBiz;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Test cases for the {@link DaoUserOcppBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserOcppBizTests {

	private CentralAuthorizationDao authorizationDao;
	private CentralChargePointDao chargePointDao;
	private CentralChargePointConnectorDao connectorDao;
	private CentralSystemUserDao systemUserDao;
	private CentralChargeSessionDao chargeSessionDao;
	private UserSettingsDao userSettingsDao;
	private ChargePointSettingsDao chargePointSettingsDao;
	private ChargePointStatusDao chargePointStatusDao;
	private ChargePointActionStatusDao chargePointActionStatusDao;
	private PasswordEncoder passwordEncoder;

	private DaoUserOcppBiz biz;

	@BeforeEach
	public void setup() {
		authorizationDao = EasyMock.createMock(CentralAuthorizationDao.class);
		chargePointDao = EasyMock.createMock(CentralChargePointDao.class);
		connectorDao = EasyMock.createMock(CentralChargePointConnectorDao.class);
		chargeSessionDao = EasyMock.createMock(CentralChargeSessionDao.class);
		passwordEncoder = EasyMock.createMock(PasswordEncoder.class);
		userSettingsDao = EasyMock.createMock(UserSettingsDao.class);
		chargePointSettingsDao = EasyMock.createMock(ChargePointSettingsDao.class);
		chargePointStatusDao = EasyMock.createMock(ChargePointStatusDao.class);
		chargePointActionStatusDao = EasyMock.createMock(ChargePointActionStatusDao.class);
		systemUserDao = EasyMock.createMock(CentralSystemUserDao.class);
		biz = new DaoUserOcppBiz(systemUserDao, chargePointDao, connectorDao, authorizationDao,
				chargeSessionDao, userSettingsDao, chargePointSettingsDao, chargePointStatusDao,
				chargePointActionStatusDao, passwordEncoder);
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(authorizationDao, chargePointDao, chargePointStatusDao,
				chargePointActionStatusDao, chargePointSettingsDao, connectorDao, chargeSessionDao,
				passwordEncoder, systemUserDao, userSettingsDao);
	}

	private void replayAll() {
		EasyMock.replay(authorizationDao, chargePointDao, chargePointStatusDao,
				chargePointActionStatusDao, chargePointSettingsDao, connectorDao, chargeSessionDao,
				passwordEncoder, systemUserDao, userSettingsDao);
	}

	@Test
	public void availableAuthorizations() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		List<CentralAuthorization> list = Collections.emptyList();
		expect(authorizationDao.findAllForOwner(userId)).andReturn(list);

		// WHEN
		replayAll();
		Collection<CentralAuthorization> results = biz.authorizationsForUser(userId);

		// THEN
		assertThat("DAO results returned", results, sameInstance(list));
	}

	@Test
	public void authorizationForUser_id() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		CentralAuthorization systemUser = new CentralAuthorization(userId, Instant.now(), "foo");
		Long id = UUID.randomUUID().getLeastSignificantBits();
		expect(authorizationDao.get(userId, id)).andReturn(systemUser);

		// WHEN
		replayAll();
		CentralAuthorization result = biz.authorizationForUser(userId, id);

		// THEN
		assertThat("DAO user returned", result, sameInstance(systemUser));
	}

	@Test
	public void deleteAuthorization() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		Long id = UUID.randomUUID().getLeastSignificantBits();
		authorizationDao.delete(userId, id);

		// WHEN
		replayAll();
		biz.deleteUserAuthorization(userId, id);

		// THEN
	}

	@Test
	public void availableSystemUsers() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		List<CentralSystemUser> list = Collections.emptyList();
		expect(systemUserDao.findAllForOwner(userId)).andReturn(list);

		// WHEN
		replayAll();
		Collection<CentralSystemUser> results = biz.systemUsersForUser(userId);

		// THEN
		assertThat("DAO results returned", results, sameInstance(list));
	}

	@Test
	public void saveSystemUser_createWithProvidedPassword() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		String rawPassword = "bar";
		CentralSystemUser systemUser = new CentralSystemUser(userId, Instant.now(), "foo", rawPassword);

		String encPassword = "encrypted";
		expect(passwordEncoder.encode(rawPassword)).andReturn(encPassword);

		Long id = UUID.randomUUID().getLeastSignificantBits();
		expect(systemUserDao.save(systemUser)).andReturn(id);

		CentralSystemUser savedSystemUser = new CentralSystemUser(systemUser);
		savedSystemUser.setPassword(null);
		expect(systemUserDao.get(id)).andReturn(savedSystemUser);

		// WHEN
		replayAll();

		CentralSystemUser result = biz.saveSystemUser(systemUser);

		// THEN
		assertThat("DAO instance returned", result, sameInstance(savedSystemUser));
		assertThat("Password not returned", result.getPassword(), nullValue());
	}

	@Test
	public void saveSystemUser_createWithGeneratedPassword() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		CentralSystemUser systemUser = new CentralSystemUser(userId, Instant.now(), "foo", null);

		Capture<String> genPasswordCaptor = new Capture<>();
		String encPassword = "encrypted";
		expect(passwordEncoder.encode(capture(genPasswordCaptor))).andReturn(encPassword);

		Long id = UUID.randomUUID().getLeastSignificantBits();
		expect(systemUserDao.save(systemUser)).andReturn(id);

		CentralSystemUser savedSystemUser = new CentralSystemUser(systemUser);
		savedSystemUser.setPassword(null);
		expect(systemUserDao.get(id)).andReturn(savedSystemUser);

		// WHEN
		replayAll();
		CentralSystemUser result = biz.saveSystemUser(systemUser);

		// THEN
		assertThat("DAO instance returned", result, sameInstance(savedSystemUser));
		assertThat("Password not returned", result.getPassword(), equalTo(genPasswordCaptor.getValue()));
	}

	@Test
	public void systemUserForUser_id() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		CentralSystemUser systemUser = new CentralSystemUser(userId, Instant.now(), "foo", null);
		Long id = UUID.randomUUID().getLeastSignificantBits();
		expect(systemUserDao.get(userId, id)).andReturn(systemUser);

		// WHEN
		replayAll();
		CentralSystemUser result = biz.systemUserForUser(userId, id);

		// THEN
		assertThat("DAO user returned", result, sameInstance(systemUser));
	}

	@Test
	public void systemUserForUser_username() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		String username = UUID.randomUUID().toString();
		CentralSystemUser systemUser = new CentralSystemUser(userId, Instant.now(), username, null);
		expect(systemUserDao.getForUsername(userId, username)).andReturn(systemUser);

		// WHEN
		replayAll();
		CentralSystemUser result = biz.systemUserForUser(userId, username);

		// THEN
		assertThat("DAO user returned", result, sameInstance(systemUser));
	}

	@Test
	public void deleteSystemUser() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		Long id = UUID.randomUUID().getLeastSignificantBits();
		systemUserDao.delete(userId, id);

		// WHEN
		replayAll();
		biz.deleteUserSystemUser(userId, id);

		// THEN
	}

	@Test
	public void availableChargePoints() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		List<CentralChargePoint> list = Collections.emptyList();
		expect(chargePointDao.findAllForOwner(userId)).andReturn(list);

		// WHEN
		replayAll();
		Collection<CentralChargePoint> results = biz.chargePointsForUser(userId);

		// THEN
		assertThat("DAO results returned", results, sameInstance(list));
	}

	@Test
	public void availableChargePointConnectors() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		List<CentralChargePointConnector> list = Collections.emptyList();
		expect(connectorDao.findAllForOwner(userId)).andReturn(list);

		// WHEN
		replayAll();
		Collection<CentralChargePointConnector> results = biz.chargePointConnectorsForUser(userId);

		// THEN
		assertThat("DAO results returned", results, sameInstance(list));
	}

	@Test
	public void chargePointConnectorsForUser_id() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		ChargePointConnectorKey id = new ChargePointConnectorKey(
				UUID.randomUUID().getLeastSignificantBits(), 1);
		CentralChargePointConnector entity = new CentralChargePointConnector(id, userId);
		expect(connectorDao.get(userId, id)).andReturn(entity);

		// WHEN
		replayAll();
		CentralChargePointConnector result = biz.chargePointConnectorForUser(userId, id);

		// THEN
		assertThat("DAO user returned", result, sameInstance(entity));
	}

	@Test
	public void deleteChargePointConnector() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		ChargePointConnectorKey id = new ChargePointConnectorKey(
				UUID.randomUUID().getLeastSignificantBits(), 1);
		connectorDao.delete(userId, id);

		// WHEN
		replayAll();
		biz.deleteUserChargePointConnector(userId, id);

		// THEN
	}

	@Test
	public void availableChargePointSettings() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		List<ChargePointSettings> list = Collections.emptyList();
		expect(chargePointSettingsDao.findAllForOwner(userId)).andReturn(list);

		// WHEN
		replayAll();
		Collection<ChargePointSettings> results = biz.chargePointSettingsForUser(userId);

		// THEN
		assertThat("DAO results returned", results, sameInstance(list));
	}

	@Test
	public void chargePointSettingsForUser_id() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		Long id = UUID.randomUUID().getLeastSignificantBits();
		ChargePointSettings entity = new ChargePointSettings(id, userId);
		expect(chargePointSettingsDao.get(userId, id)).andReturn(entity);

		// WHEN
		replayAll();
		ChargePointSettings result = biz.chargePointSettingsForUser(userId, id);

		// THEN
		assertThat("DAO user returned", result, sameInstance(entity));
	}

	@Test
	public void deleteChargePointSettings() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		Long id = UUID.randomUUID().getLeastSignificantBits();
		chargePointSettingsDao.delete(userId, id);

		// WHEN
		replayAll();
		biz.deleteUserChargePointSettings(userId, id);

		// THEN
	}

	@Test
	public void userSettingsForUser() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		UserSettings entity = new UserSettings(userId);
		expect(userSettingsDao.get(userId)).andReturn(entity);

		// WHEN
		replayAll();
		UserSettings result = biz.settingsForUser(userId);

		// THEN
		assertThat("DAO user returned", result, sameInstance(entity));
	}

	@Test
	public void deleteUserSettings() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		userSettingsDao.delete(userId);

		// WHEN
		replayAll();
		biz.deleteUserSettings(userId);

		// THEN
	}

	@Test
	public void chargeSessionForUser() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		UUID sessionId = UUID.randomUUID();
		CentralChargeSession entity = CentralChargeSession.forChargePoint(1);
		expect(chargeSessionDao.get(sessionId, userId)).andReturn(entity);

		// WHEN
		replayAll();
		ChargeSession result = biz.chargeSessionForUser(userId, sessionId);
		assertThat("DAO session returned", result, sameInstance(entity));
	}

	@Test
	public void incompleteChargeSessionsForChargePoint() {
		// GIVEN
		Long userId = UUID.randomUUID().getMostSignificantBits();
		Long chargePointId = UUID.randomUUID().getLeastSignificantBits();
		CentralChargeSession entity = CentralChargeSession.forChargePoint(1);
		expect(chargeSessionDao.getIncompleteChargeSessionsForUserForChargePoint(userId, chargePointId))
				.andReturn(singleton(entity));

		// WHEN
		replayAll();
		Collection<ChargeSession> result = biz.incompleteChargeSessionsForChargePoint(userId,
				chargePointId);
		List<ChargeSession> resultList = result.stream().collect(Collectors.toList());

		// THEN
		assertThat("Result returned", resultList, hasSize(1));
		assertThat("DAO session returned", resultList.get(0), sameInstance(entity));
	}

}
