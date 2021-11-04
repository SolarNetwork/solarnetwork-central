/* ==================================================================
 * DaoUserExpireBizTests.java - 10/07/2018 11:42:51 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.expire.biz.dao.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.user.expire.biz.dao.DaoUserExpireBiz;
import net.solarnetwork.central.user.expire.dao.ExpireUserDataConfigurationDao;
import net.solarnetwork.central.user.expire.domain.ExpireUserDataConfiguration;

/**
 * Test cases for the {@link DaoUserExpireBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserExpireBizTests {

	private static final Long TEST_USER_ID = -1L;

	private ExpireUserDataConfigurationDao dataConfigurationDao;

	private DaoUserExpireBiz biz;

	@Before
	public void setup() {
		dataConfigurationDao = EasyMock.createMock(ExpireUserDataConfigurationDao.class);

		biz = new DaoUserExpireBiz(dataConfigurationDao);
	}

	private void replayAll() {
		EasyMock.replay(dataConfigurationDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(dataConfigurationDao);
	}

	@Test
	public void findConfigurationsForUser() {
		// given
		ExpireUserDataConfiguration config = new ExpireUserDataConfiguration();
		List<ExpireUserDataConfiguration> configs = Arrays.asList(config);
		expect(dataConfigurationDao.findConfigurationsForUser(TEST_USER_ID)).andReturn(configs);

		// when
		replayAll();
		List<ExpireUserDataConfiguration> result = biz.configurationsForUser(TEST_USER_ID,
				ExpireUserDataConfiguration.class);

		// then
		assertThat("Results match", result, sameInstance(configs));
	}

	@Test
	public void getConfiguration() {
		// given
		Long configId = UUID.randomUUID().getMostSignificantBits();
		ExpireUserDataConfiguration config = new ExpireUserDataConfiguration();
		expect(dataConfigurationDao.get(configId, TEST_USER_ID)).andReturn(config);

		// when
		replayAll();
		ExpireUserDataConfiguration result = biz.configurationForUser(TEST_USER_ID,
				ExpireUserDataConfiguration.class, configId);

		// then
		assertThat("Results match", result, sameInstance(config));
	}

	@Test
	public void countsForConfiguration() {
		// given
		DatumRecordCounts counts = new DatumRecordCounts();
		Capture<ExpireUserDataConfiguration> configCaptor = new Capture<>();
		expect(dataConfigurationDao.countExpiredDataForConfiguration(capture(configCaptor)))
				.andReturn(counts);

		// when
		replayAll();
		ExpireUserDataConfiguration config = new ExpireUserDataConfiguration();
		DatumRecordCounts result = biz.countExpiredDataForConfiguration(config);

		// then
		assertThat("Results match", result, sameInstance(counts));
		assertThat("Config passed", configCaptor.getValue(), sameInstance(config));
	}
}
