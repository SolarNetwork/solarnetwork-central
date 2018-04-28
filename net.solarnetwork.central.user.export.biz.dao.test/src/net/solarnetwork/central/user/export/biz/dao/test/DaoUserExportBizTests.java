/* ==================================================================
 * DaoUserExportBizTests.java - 24/04/2018 10:41:30 AM
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

package net.solarnetwork.central.user.export.biz.dao.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.export.biz.dao.DaoUserExportBiz;
import net.solarnetwork.central.user.export.dao.UserDataConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDestinationConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserOutputConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskPK;

/**
 * Test cases for the {@link UserExportBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserExportBizTests {

	private static final Long TEST_USER_ID = -1L;
	private static final Long TEST_NODE_ID = -2L;
	private static final Long TEST_NODE_ID_2 = -3L;
	private static final AtomicLong ID_GENERATOR = new AtomicLong(-999L);

	private UserDatumExportConfigurationDao configurationDao;
	private UserDataConfigurationDao dataConfigurationDao;
	private UserDestinationConfigurationDao destConfigurationDao;
	private UserOutputConfigurationDao outputConfigurationDao;
	private UserDatumExportTaskInfoDao taskDao;
	private UserNodeDao userNodeDao;
	private GeneralNodeDatumDao generalNodeDatumDao;

	private DaoUserExportBiz biz;

	@Before
	public void setup() {
		configurationDao = EasyMock.createMock(UserDatumExportConfigurationDao.class);
		dataConfigurationDao = EasyMock.createMock(UserDataConfigurationDao.class);
		destConfigurationDao = EasyMock.createMock(UserDestinationConfigurationDao.class);
		outputConfigurationDao = EasyMock.createMock(UserOutputConfigurationDao.class);
		taskDao = EasyMock.createMock(UserDatumExportTaskInfoDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		generalNodeDatumDao = EasyMock.createMock(GeneralNodeDatumDao.class);

		biz = new DaoUserExportBiz(configurationDao, dataConfigurationDao, destConfigurationDao,
				outputConfigurationDao, taskDao, userNodeDao, generalNodeDatumDao);
	}

	private void replayAll() {
		EasyMock.replay(configurationDao, dataConfigurationDao, destConfigurationDao,
				outputConfigurationDao, taskDao, userNodeDao, generalNodeDatumDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(configurationDao, dataConfigurationDao, destConfigurationDao,
				outputConfigurationDao, taskDao, userNodeDao, generalNodeDatumDao);
	}

	private UserDatumExportConfiguration createConfiguration() {
		UserDatumExportConfiguration config = new UserDatumExportConfiguration();
		config.setId(ID_GENERATOR.decrementAndGet());
		config.setName("Config" + config.getId());
		config.setUserId(TEST_USER_ID);
		config.setSchedule(ScheduleType.Hourly);
		return config;
	}

	@Test
	public void submitTask() {
		// given
		DateTime now = new DateTime();
		UserDatumExportConfiguration config = createConfiguration();
		expect(userNodeDao.findNodeIdsForUser(TEST_USER_ID))
				.andReturn(Collections.singleton(TEST_NODE_ID));

		Capture<UserDatumExportTaskInfo> taskCaptor = new Capture<>();

		DateTime exportDate = ScheduleType.Hourly.exportDate(now);
		expect(taskDao.store(capture(taskCaptor)))
				.andReturn(new UserDatumExportTaskPK(TEST_USER_ID, ScheduleType.Hourly, exportDate));

		// when
		replayAll();
		UserDatumExportTaskInfo task = biz.submitDatumExportConfiguration(config, exportDate);

		// then
		assertThat("Task created", task, notNullValue());

		assertThat("Task user ID", task.getUserId(), equalTo(TEST_USER_ID));
		assertThat("Task schedule", task.getScheduleType(), equalTo(ScheduleType.Hourly));
		assertThat("Task date", task.getExportDate(), equalTo(exportDate));
		assertThat("Task config available", task.getConfig(), notNullValue());
		assertThat("Config name", task.getConfig().getName(), equalTo(config.getName()));
		assertThat("Node ID populated",
				task.getConfig().getDataConfiguration().getDatumFilter().getNodeIds(),
				arrayContaining(TEST_NODE_ID));
	}

	@Test
	public void submitTaskResolveSourceIdPattern() {
		// given
		biz.setPathMatcher(new AntPathMatcher());

		DateTime now = new DateTime();
		UserDatumExportConfiguration config = createConfiguration();
		UserDataConfiguration dataConfig = new UserDataConfiguration();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setSourceId("/test/**");
		dataConfig.setFilter(filter);
		config.setUserDataConfiguration(dataConfig);

		expect(userNodeDao.findNodeIdsForUser(TEST_USER_ID))
				.andReturn(Collections.singleton(TEST_NODE_ID));

		DateTime exportDate = ScheduleType.Hourly.exportDate(now);

		Set<String> allSourceIds = new LinkedHashSet<>(
				Arrays.asList("/foo/bar", "/test/foo", "/test/bar"));
		expect(generalNodeDatumDao.getAvailableSources(TEST_NODE_ID, exportDate,
				ScheduleType.Hourly.nextExportDate(exportDate))).andReturn(allSourceIds);

		Capture<UserDatumExportTaskInfo> taskCaptor = new Capture<>();

		expect(taskDao.store(capture(taskCaptor)))
				.andReturn(new UserDatumExportTaskPK(TEST_USER_ID, ScheduleType.Hourly, exportDate));

		// when
		replayAll();
		UserDatumExportTaskInfo task = biz.submitDatumExportConfiguration(config, exportDate);

		// then
		assertThat("Task created", task, notNullValue());

		assertThat("Task user ID", task.getUserId(), equalTo(TEST_USER_ID));
		assertThat("Task schedule", task.getScheduleType(), equalTo(ScheduleType.Hourly));
		assertThat("Task date", task.getExportDate(), equalTo(exportDate));
		assertThat("Task config available", task.getConfig(), notNullValue());
		assertThat("Config name", task.getConfig().getName(), equalTo(config.getName()));
		assertThat("Node ID populated",
				task.getConfig().getDataConfiguration().getDatumFilter().getNodeIds(),
				arrayContaining(TEST_NODE_ID));
		assertThat("Source IDs populated",
				task.getConfig().getDataConfiguration().getDatumFilter().getSourceIds(),
				arrayContaining("/test/foo", "/test/bar"));
	}

	@Test
	public void submitTaskResolveSourceIdPatternMultipleNodes() {
		// given
		biz.setPathMatcher(new AntPathMatcher());

		DateTime now = new DateTime();
		UserDatumExportConfiguration config = createConfiguration();
		UserDataConfiguration dataConfig = new UserDataConfiguration();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setSourceId("/test/**");
		dataConfig.setFilter(filter);
		config.setUserDataConfiguration(dataConfig);

		expect(userNodeDao.findNodeIdsForUser(TEST_USER_ID))
				.andReturn(new LinkedHashSet<>(Arrays.asList(TEST_NODE_ID, TEST_NODE_ID_2)));

		DateTime exportDate = ScheduleType.Hourly.exportDate(now);

		Set<String> allSourceIdsNode1 = new LinkedHashSet<>(
				Arrays.asList("/foo/bar", "/test/foo", "/test/bar"));
		Set<String> allSourceIdsNode2 = new LinkedHashSet<>(Arrays.asList("/test/bam"));
		expect(generalNodeDatumDao.getAvailableSources(TEST_NODE_ID, exportDate,
				ScheduleType.Hourly.nextExportDate(exportDate))).andReturn(allSourceIdsNode1);
		expect(generalNodeDatumDao.getAvailableSources(TEST_NODE_ID_2, exportDate,
				ScheduleType.Hourly.nextExportDate(exportDate))).andReturn(allSourceIdsNode2);

		Capture<UserDatumExportTaskInfo> taskCaptor = new Capture<>();

		expect(taskDao.store(capture(taskCaptor)))
				.andReturn(new UserDatumExportTaskPK(TEST_USER_ID, ScheduleType.Hourly, exportDate));

		// when
		replayAll();
		UserDatumExportTaskInfo task = biz.submitDatumExportConfiguration(config, exportDate);

		// then
		assertThat("Task created", task, notNullValue());

		assertThat("Task user ID", task.getUserId(), equalTo(TEST_USER_ID));
		assertThat("Task schedule", task.getScheduleType(), equalTo(ScheduleType.Hourly));
		assertThat("Task date", task.getExportDate(), equalTo(exportDate));
		assertThat("Task config available", task.getConfig(), notNullValue());
		assertThat("Config name", task.getConfig().getName(), equalTo(config.getName()));
		assertThat("Node ID populated",
				task.getConfig().getDataConfiguration().getDatumFilter().getNodeIds(),
				arrayContaining(TEST_NODE_ID, TEST_NODE_ID_2));
		assertThat("Source IDs populated",
				task.getConfig().getDataConfiguration().getDatumFilter().getSourceIds(),
				arrayContaining("/test/foo", "/test/bar", "/test/bam"));
	}

}
