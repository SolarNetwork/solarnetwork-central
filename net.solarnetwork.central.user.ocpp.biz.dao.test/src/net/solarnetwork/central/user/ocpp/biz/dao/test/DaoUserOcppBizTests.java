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

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.user.ocpp.biz.dao.DaoUserOcppBiz;

/**
 * Test cases for the {@link DaoUserOcppBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserOcppBizTests {

	private CentralAuthorizationDao authorizationDao;
	private CentralChargePointDao chargePointDao;
	private CentralSystemUserDao systemUserDao;

	private DaoUserOcppBiz biz;

	@Before
	public void setup() {
		authorizationDao = EasyMock.createMock(CentralAuthorizationDao.class);
		chargePointDao = EasyMock.createMock(CentralChargePointDao.class);
		systemUserDao = EasyMock.createMock(CentralSystemUserDao.class);
		biz = new DaoUserOcppBiz(systemUserDao, chargePointDao, authorizationDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(authorizationDao, chargePointDao, systemUserDao);
	}

	private void replayAll() {
		EasyMock.replay(authorizationDao, chargePointDao, systemUserDao);
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
}
