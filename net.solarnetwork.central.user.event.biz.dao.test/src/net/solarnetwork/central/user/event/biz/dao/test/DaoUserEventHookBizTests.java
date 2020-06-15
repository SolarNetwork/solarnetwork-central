/* ==================================================================
 * DaoUserEventHookBizTests.java - 11/06/2020 10:55:36 am
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

package net.solarnetwork.central.user.event.biz.dao.test;

import static java.time.Instant.now;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.central.user.event.biz.dao.DaoUserEventHookBiz;
import net.solarnetwork.central.user.event.dao.UserNodeEventHookConfigurationDao;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;

/**
 * Test cases for the {@link DaoUserEventHookBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserEventHookBizTests {

	private UserNodeEventHookConfigurationDao nodeEventHookConfigurationDao;
	private DaoUserEventHookBiz biz;

	@Before
	public void setup() {
		nodeEventHookConfigurationDao = EasyMock.createMock(UserNodeEventHookConfigurationDao.class);
		biz = new DaoUserEventHookBiz(nodeEventHookConfigurationDao);
	}

	private void replayAll() {
		EasyMock.replay(nodeEventHookConfigurationDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(nodeEventHookConfigurationDao);
	}

	@Test
	public void listConfigurationsForUser() {
		// GIVEN
		final Long userId = 1L;
		List<UserNodeEventHookConfiguration> confs = new ArrayList<>();
		expect(nodeEventHookConfigurationDao.findConfigurationsForUser(userId)).andReturn(confs);

		// WHEN
		replayAll();
		List<UserNodeEventHookConfiguration> result = biz.configurationsForUser(userId,
				UserNodeEventHookConfiguration.class);

		// THEN
		assertThat("DAO result returned", result, sameInstance(confs));
	}

	@Test
	public void configurationForId() {
		// GIVEN
		final Long userId = 1L;
		final Long confId = 2L;
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(confId, userId, now());
		expect(nodeEventHookConfigurationDao.get(eq(new UserLongPK(userId, confId)))).andReturn(conf);

		// WHEN
		replayAll();
		UserNodeEventHookConfiguration result = biz.configurationForUser(userId,
				UserNodeEventHookConfiguration.class, confId);

		// THEN
		assertThat("DAO result returned", result, sameInstance(conf));
	}

	@Test
	public void saveConfiguration() {
		// GIVEN
		final Long userId = 1L;
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(null, userId, now());
		final UserLongPK pk = new UserLongPK(userId, 2L);
		expect(nodeEventHookConfigurationDao.save(conf)).andReturn(pk);

		// WHEN
		replayAll();
		UserLongPK result = biz.saveConfiguration(conf);

		// THEN
		assertThat("PK result matches", result, sameInstance(pk));
	}

	@Test
	public void deleteConfiguration() {
		// GIVEN
		final Long userId = 1L;
		final Long confId = 2L;
		UserNodeEventHookConfiguration conf = new UserNodeEventHookConfiguration(confId, userId, now());
		nodeEventHookConfigurationDao.delete(conf);

		// WHEN
		replayAll();
		biz.deleteConfiguration(conf);

		// THEN
	}
}
