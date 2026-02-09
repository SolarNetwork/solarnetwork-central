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
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusDao;
import net.solarnetwork.central.ocpp.dao.UserSettingsDao;
import net.solarnetwork.central.ocpp.domain.BasicOcppFilter;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.central.ocpp.domain.CentralChargePointFilter;
import net.solarnetwork.central.ocpp.domain.CentralChargeSession;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.central.user.ocpp.biz.dao.DaoUserOcppBiz;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargeSession;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Test cases for the {@link DaoUserOcppBiz} class.
 * 
 * @author matt
 * @version 1.1
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("static-access")
public class DaoUserOcppBizTests {

	@Mock
	private CentralAuthorizationDao authorizationDao;

	@Mock
	private CentralChargePointDao chargePointDao;

	@Mock
	private CentralChargePointConnectorDao connectorDao;

	@Mock
	private CentralSystemUserDao systemUserDao;

	@Mock
	private CentralChargeSessionDao chargeSessionDao;

	@Mock
	private UserSettingsDao userSettingsDao;

	@Mock
	private ChargePointSettingsDao chargePointSettingsDao;

	@Mock
	private ChargePointStatusDao chargePointStatusDao;

	@Mock
	private ChargePointActionStatusDao chargePointActionStatusDao;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Captor
	private ArgumentCaptor<String> passwordCaptor;

	@Captor
	private ArgumentCaptor<CentralChargePointFilter> chargePointFilterCaptor;

	private DaoUserOcppBiz biz;

	@BeforeEach
	public void setup() {
		biz = new DaoUserOcppBiz(systemUserDao, chargePointDao, connectorDao, authorizationDao,
				chargeSessionDao, userSettingsDao, chargePointSettingsDao, chargePointStatusDao,
				chargePointActionStatusDao, passwordEncoder);
	}

	@Test
	public void availableAuthorizations() {
		// GIVEN
		final Long userId = randomLong();
		List<CentralAuthorization> list = Collections.emptyList();
		given(authorizationDao.findAllForOwner(userId)).willReturn(list);

		// WHEN
		Collection<CentralAuthorization> results = biz.authorizationsForUser(userId);

		// THEN
		// @formatter:off
		and.then(results)
			.as("DAO result returned")
			.isSameAs(list)
			;
		// @formatter:on
	}

	@Test
	public void authorizationForUser_id() {
		// GIVEN
		final Long userId = randomLong();
		CentralAuthorization systemUser = new CentralAuthorization(userId, Instant.now(), "foo");
		final Long id = randomLong();
		given(authorizationDao.get(userId, id)).willReturn(systemUser);

		// WHEN
		CentralAuthorization result = biz.authorizationForUser(userId, id);

		// THEN
		// @formatter:off
		and.then(result)
			.as("DAO result returned")
			.isSameAs(systemUser)
			;
		// @formatter:on
	}

	@Test
	public void deleteAuthorization() {
		// GIVEN
		final Long userId = randomLong();
		final Long id = randomLong();

		// WHEN
		biz.deleteUserAuthorization(userId, id);

		// THEN
		then(authorizationDao).should().delete(userId, id);
	}

	@Test
	public void availableSystemUsers() {
		// GIVEN
		final Long userId = randomLong();
		List<CentralSystemUser> list = Collections.emptyList();
		given(systemUserDao.findAllForOwner(userId)).willReturn(list);

		// WHEN
		Collection<CentralSystemUser> results = biz.systemUsersForUser(userId);

		// THEN
		// @formatter:off
		and.then(results)
			.as("DAO result returned")
			.isSameAs(list)
			;
		// @formatter:on
	}

	@Test
	public void saveSystemUser_createWithProvidedPassword() {
		// GIVEN
		final Long userId = randomLong();
		String rawPassword = "bar";
		CentralSystemUser systemUser = new CentralSystemUser(userId, Instant.now(), "foo", rawPassword);

		String encPassword = "encrypted";
		given(passwordEncoder.encode(rawPassword)).willReturn(encPassword);

		final Long id = randomLong();
		given(systemUserDao.save(systemUser)).willReturn(id);

		CentralSystemUser savedSystemUser = new CentralSystemUser(systemUser);
		savedSystemUser.setPassword(null);
		given(systemUserDao.get(id)).willReturn(savedSystemUser);

		// WHEN

		CentralSystemUser result = biz.saveSystemUser(systemUser);

		// THEN
		// @formatter:off
		and.then(result)
			.as("DAO result returned")
			.isSameAs(savedSystemUser)
			.as("Password not returned")
			.returns(null, from(CentralSystemUser::getPassword))
			;
		// @formatter:on
	}

	@Test
	public void saveSystemUser_createWithGeneratedPassword() {
		// GIVEN
		final Long userId = randomLong();
		CentralSystemUser systemUser = new CentralSystemUser(userId, Instant.now(), "foo", null);

		String encPassword = "encrypted";
		given(passwordEncoder.encode(any())).willReturn(encPassword);

		final Long id = randomLong();
		given(systemUserDao.save(systemUser)).willReturn(id);

		CentralSystemUser savedSystemUser = new CentralSystemUser(systemUser);
		savedSystemUser.setPassword(null);
		given(systemUserDao.get(id)).willReturn(savedSystemUser);

		// WHEN
		CentralSystemUser result = biz.saveSystemUser(systemUser);

		// THEN
		// @formatter:off
		then(passwordEncoder).should().encode(passwordCaptor.capture());
		and.then(result.getPassword())
			.as("Password returned")
			.isEqualTo(passwordCaptor.getValue())
			;
		// @formatter:on

		and.then(result).as("DAO instance returned").isSameAs(savedSystemUser);
	}

	@Test
	public void systemUserForUser_id() {
		// GIVEN
		final Long userId = randomLong();
		CentralSystemUser systemUser = new CentralSystemUser(userId, Instant.now(), "foo", null);
		final Long id = randomLong();
		given(systemUserDao.get(userId, id)).willReturn(systemUser);

		// WHEN
		CentralSystemUser result = biz.systemUserForUser(userId, id);

		// THEN
		// @formatter:off
		and.then(result)
			.as("DAO result returned")
			.isSameAs(systemUser)
			;
		// @formatter:on
	}

	@Test
	public void systemUserForUser_username() {
		// GIVEN
		final Long userId = randomLong();
		String username = UUID.randomUUID().toString();
		CentralSystemUser systemUser = new CentralSystemUser(userId, Instant.now(), username, null);
		given(systemUserDao.getForUsername(userId, username)).willReturn(systemUser);

		// WHEN
		CentralSystemUser result = biz.systemUserForUser(userId, username);

		// THEN
		// @formatter:off
		and.then(result)
			.as("DAO result returned")
			.isSameAs(systemUser)
			;
		// @formatter:on
	}

	@Test
	public void deleteSystemUser() {
		// GIVEN
		final Long userId = randomLong();
		final Long id = randomLong();

		// WHEN
		biz.deleteUserSystemUser(userId, id);

		// THEN
		then(systemUserDao).should().delete(userId, id);
	}

	@Test
	public void availableChargePoints() {
		// GIVEN
		final Long userId = randomLong();
		List<CentralChargePoint> list = Collections.emptyList();
		given(chargePointDao.findAllForOwner(userId)).willReturn(list);

		// WHEN
		Collection<CentralChargePoint> results = biz.chargePointsForUser(userId);

		// THEN
		// @formatter:off
		and.then(results)
			.as("DAO result returned")
			.isSameAs(list)
			;
		// @formatter:on
	}

	@Test
	public void listChargePoints_page() {
		// GIVEN
		final Long userId = randomLong();
		final var filter = new BasicOcppFilter();
		filter.setMax(randomInt());
		filter.setOffset(randomLong());

		final var daoResults = new BasicFilterResults<CentralChargePoint, Long>(List.of());
		given(chargePointDao.findFiltered(any())).willReturn(daoResults);

		// WHEN
		FilterResults<CentralChargePoint, Long> results = biz.listChargePointsForUser(userId, filter);

		// THEN
		// @formatter:off
		then(chargePointDao).should().findFiltered(chargePointFilterCaptor.capture());
		and.then(chargePointFilterCaptor.getValue())
			.as("Copy of filter passed to DAO")
			.isNotSameAs(filter)
			.as("User ID set on filter")
			.returns(new Long[] {userId}, from(CentralChargePointFilter::getUserIds))
			.as("Max copied to filter")
			.returns(filter.getMax(), from(CentralChargePointFilter::getMax))
			.as("Offset copied to filter")
			.returns(filter.getOffset(), from(CentralChargePointFilter::getOffset))
			;
		
		and.then(results)
			.as("DAO result returned")
			.isSameAs(daoResults)
			;
		// @formatter:on
	}

	@Test
	public void availableChargePointConnectors() {
		// GIVEN
		final Long userId = randomLong();
		List<CentralChargePointConnector> list = Collections.emptyList();
		given(connectorDao.findAllForOwner(userId)).willReturn(list);

		// WHEN
		Collection<CentralChargePointConnector> results = biz.chargePointConnectorsForUser(userId);

		// THEN
		// @formatter:off
		and.then(results)
			.as("DAO result returned")
			.isSameAs(list)
			;
		// @formatter:on
	}

	@Test
	public void chargePointConnectorsForUser_id() {
		// GIVEN
		final Long userId = randomLong();
		ChargePointConnectorKey id = new ChargePointConnectorKey(
				UUID.randomUUID().getLeastSignificantBits(), 1);
		CentralChargePointConnector entity = new CentralChargePointConnector(id, userId);
		given(connectorDao.get(userId, id)).willReturn(entity);

		// WHEN
		CentralChargePointConnector result = biz.chargePointConnectorForUser(userId, id);

		// THEN
		// @formatter:off
		and.then(result)
			.as("DAO result returned")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void deleteChargePointConnector() {
		// GIVEN
		final Long userId = randomLong();
		final ChargePointConnectorKey id = new ChargePointConnectorKey(
				UUID.randomUUID().getLeastSignificantBits(), 1);

		// WHEN
		biz.deleteUserChargePointConnector(userId, id);

		// THEN
		then(connectorDao).should().delete(userId, id);
	}

	@Test
	public void availableChargePointSettings() {
		// GIVEN
		final Long userId = randomLong();
		List<ChargePointSettings> list = Collections.emptyList();
		given(chargePointSettingsDao.findAllForOwner(userId)).willReturn(list);

		// WHEN
		Collection<ChargePointSettings> results = biz.chargePointSettingsForUser(userId);

		// THEN
		// @formatter:off
		and.then(results)
			.as("DAO result returned")
			.isSameAs(list)
			;
		// @formatter:on
	}

	@Test
	public void chargePointSettingsForUser_id() {
		// GIVEN
		final Long userId = randomLong();
		final Long id = randomLong();
		ChargePointSettings entity = new ChargePointSettings(id, userId);
		given(chargePointSettingsDao.get(userId, id)).willReturn(entity);

		// WHEN
		ChargePointSettings result = biz.chargePointSettingsForUser(userId, id);

		// THEN
		// @formatter:off
		and.then(result)
			.as("DAO result returned")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void deleteChargePointSettings() {
		// GIVEN
		final Long userId = randomLong();
		final Long id = randomLong();

		// WHEN
		biz.deleteUserChargePointSettings(userId, id);

		// THEN
		then(chargePointSettingsDao).should().delete(userId, id);
	}

	@Test
	public void userSettingsForUser() {
		// GIVEN
		final Long userId = randomLong();
		UserSettings entity = new UserSettings(userId);
		given(userSettingsDao.get(userId)).willReturn(entity);

		// WHEN
		UserSettings result = biz.settingsForUser(userId);

		// THEN
		// @formatter:off
		and.then(result)
			.as("DAO result returned")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void deleteUserSettings() {
		// GIVEN
		final Long userId = randomLong();

		// WHEN
		biz.deleteUserSettings(userId);

		// THEN
		then(userSettingsDao).should().delete(userId);
	}

	@Test
	public void chargeSessionForUser() {
		// GIVEN
		final Long userId = randomLong();
		UUID sessionId = UUID.randomUUID();
		CentralChargeSession entity = CentralChargeSession.forChargePoint(1);
		given(chargeSessionDao.get(sessionId, userId)).willReturn(entity);

		// WHEN
		ChargeSession result = biz.chargeSessionForUser(userId, sessionId);

		// THEN
		// @formatter:off
		and.then(result)
			.as("DAO result returned")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void incompleteChargeSessionsForChargePoint() {
		// GIVEN
		final Long userId = randomLong();
		final Long chargePointId = randomLong();
		CentralChargeSession entity = CentralChargeSession.forChargePoint(1);
		given(chargeSessionDao.getIncompleteChargeSessionsForUserForChargePoint(userId, chargePointId))
				.willReturn(singleton(entity));

		// WHEN
		Collection<ChargeSession> result = biz.incompleteChargeSessionsForChargePoint(userId,
				chargePointId);

		// THEN
		// @formatter:off
		and.then(result)
			.as("DAO result returned")
			.hasSize(1)
			.element(0)
			.as("DAO result returned")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void chargePointConnectorsForUserAndChargePoint() {
		// GIVEN
		final Long userId = randomLong();
		final long chargePointId = randomLong();

		final List<CentralChargePointConnector> daoResult = new ArrayList<>();
		given(connectorDao.findByChargePointId(userId, chargePointId)).willReturn(daoResult);

		// WHEN
		Collection<CentralChargePointConnector> result = biz.chargePointConnectorsForUser(userId,
				chargePointId);

		// THEN
		// @formatter:off
		and.then(result)
			.as("DAO result returned")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}
}
