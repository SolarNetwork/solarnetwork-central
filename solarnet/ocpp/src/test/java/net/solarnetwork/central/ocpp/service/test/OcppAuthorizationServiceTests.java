/* ==================================================================
 * OcppAuthorizationServiceTests.java - 19/02/2024 6:52:19 am
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

package net.solarnetwork.central.ocpp.service.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.service.OcppAuthorizationService;
import net.solarnetwork.ocpp.domain.Authorization;
import net.solarnetwork.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;

/**
 * Test cases for the {@link OcppAuthorizationService} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class OcppAuthorizationServiceTests {

	@Mock
	private CentralAuthorizationDao authorizationDao;

	@Mock
	private CentralChargePointDao chargePointDao;

	private OcppAuthorizationService service;

	@BeforeEach
	public void setup() {
		service = new OcppAuthorizationService(authorizationDao, chargePointDao);
	}

	@Test
	public void auth_ok() {
		// GIVEN
		Long userId = randomLong();
		Long nodeId = randomLong();
		ChargePointIdentity identity = new ChargePointIdentity(randomString(), userId.toString());
		String idTag = randomString();

		// look up charger
		CentralChargePoint cp = new CentralChargePoint(randomLong(), userId, nodeId);
		given(chargePointDao.getForIdentity(identity)).willReturn(cp);

		// look up auth
		Authorization auth = new Authorization(randomLong());
		auth.setToken(idTag);
		auth.setEnabled(true);
		auth.setExpiryDate(Instant.now().plus(1L, ChronoUnit.DAYS));
		auth.setParentId(randomString());
		given(authorizationDao.getForToken(userId, idTag)).willReturn(auth);

		// WHEN
		AuthorizationInfo result = service.authorize(identity, idTag);

		// THEN
		// @formatter:off
		then(result)
			.as("Result provided")
			.isNotNull()
			.as("ID is token")
			.returns(idTag, AuthorizationInfo::getId)
			.as("Status is accepted")
			.returns(AuthorizationStatus.Accepted, AuthorizationInfo::getStatus)
			.as("Expiry copied from Authorization")
			.returns(auth.getExpiryDate(), AuthorizationInfo::getExpiryDate)
			.as("Parent ID copied from Authorization")
			.returns(auth.getParentId(), AuthorizationInfo::getParentId)
			;
		// @formatter:on
	}

	@Test
	public void auth_expired() {
		// GIVEN
		Long userId = randomLong();
		Long nodeId = randomLong();
		ChargePointIdentity identity = new ChargePointIdentity(randomString(), userId.toString());
		String idTag = randomString();

		// look up charger
		CentralChargePoint cp = new CentralChargePoint(randomLong(), userId, nodeId);
		given(chargePointDao.getForIdentity(identity)).willReturn(cp);

		// look up auth
		Authorization auth = new Authorization(randomLong());
		auth.setToken(idTag);
		auth.setEnabled(true);
		auth.setExpiryDate(Instant.now().plus(-1L, ChronoUnit.DAYS));
		auth.setParentId(randomString());
		given(authorizationDao.getForToken(userId, idTag)).willReturn(auth);

		// WHEN
		AuthorizationInfo result = service.authorize(identity, idTag);

		// THEN
		// @formatter:off
		then(result)
			.as("Result provided")
			.isNotNull()
			.as("ID is token")
			.returns(idTag, AuthorizationInfo::getId)
			.as("Status is expired")
			.returns(AuthorizationStatus.Expired, AuthorizationInfo::getStatus)
			.as("Expiry copied from Authorization")
			.returns(auth.getExpiryDate(), AuthorizationInfo::getExpiryDate)
			.as("Parent ID copied from Authorization")
			.returns(auth.getParentId(), AuthorizationInfo::getParentId)
			;
		// @formatter:on
	}

	@Test
	public void auth_disabled() {
		// GIVEN
		Long userId = randomLong();
		Long nodeId = randomLong();
		ChargePointIdentity identity = new ChargePointIdentity(randomString(), userId.toString());
		String idTag = randomString();

		// look up charger
		CentralChargePoint cp = new CentralChargePoint(randomLong(), userId, nodeId);
		given(chargePointDao.getForIdentity(identity)).willReturn(cp);

		// look up auth
		Authorization auth = new Authorization(randomLong());
		auth.setToken(idTag);
		auth.setEnabled(false);
		auth.setExpiryDate(Instant.now().plus(1L, ChronoUnit.DAYS));
		auth.setParentId(randomString());
		given(authorizationDao.getForToken(userId, idTag)).willReturn(auth);

		// WHEN
		AuthorizationInfo result = service.authorize(identity, idTag);

		// THEN
		// @formatter:off
		then(result)
			.as("Result provided")
			.isNotNull()
			.as("ID is token")
			.returns(idTag, AuthorizationInfo::getId)
			.as("Status is blocked")
			.returns(AuthorizationStatus.Blocked, AuthorizationInfo::getStatus)
			.as("Expiry copied from Authorization")
			.returns(auth.getExpiryDate(), AuthorizationInfo::getExpiryDate)
			.as("Parent ID copied from Authorization")
			.returns(auth.getParentId(), AuthorizationInfo::getParentId)
			;
		// @formatter:on
	}

	@Test
	public void auth_noChargePoint() {
		// GIVEN
		Long userId = randomLong();
		ChargePointIdentity identity = new ChargePointIdentity(randomString(), userId.toString());
		String idTag = randomString();

		// look up charger
		given(chargePointDao.getForIdentity(identity)).willReturn(null);

		// WHEN
		AuthorizationInfo result = service.authorize(identity, idTag);

		// THEN
		// @formatter:off
		then(result)
			.as("Result provided")
			.isNotNull()
			.as("ID is token")
			.returns(idTag, AuthorizationInfo::getId)
			.as("Status is expired")
			.returns(AuthorizationStatus.Invalid, AuthorizationInfo::getStatus)
			.as("Expiry not available")
			.returns(null, AuthorizationInfo::getExpiryDate)
			.as("Parent ID not available")
			.returns(null, AuthorizationInfo::getParentId)
			;
		// @formatter:on
	}

	@Test
	public void auth_noAuth() {
		// GIVEN
		Long userId = randomLong();
		Long nodeId = randomLong();
		ChargePointIdentity identity = new ChargePointIdentity(randomString(), userId.toString());
		String idTag = randomString();

		// look up charger
		CentralChargePoint cp = new CentralChargePoint(randomLong(), userId, nodeId);
		given(chargePointDao.getForIdentity(identity)).willReturn(cp);

		// look up auth
		given(authorizationDao.getForToken(userId, idTag)).willReturn(null);

		// WHEN
		AuthorizationInfo result = service.authorize(identity, idTag);

		// THEN
		// @formatter:off
		then(result)
			.as("Result provided")
			.isNotNull()
			.as("ID is token")
			.returns(idTag, AuthorizationInfo::getId)
			.as("Status is expired")
			.returns(AuthorizationStatus.Invalid, AuthorizationInfo::getStatus)
			.as("Expiry not available")
			.returns(null, AuthorizationInfo::getExpiryDate)
			.as("Parent ID not available")
			.returns(null, AuthorizationInfo::getParentId)
			;
		// @formatter:on
	}

}
