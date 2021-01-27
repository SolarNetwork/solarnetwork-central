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

import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static net.solarnetwork.util.JodaDateUtils.fromJodaToInstant;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.export.biz.dao.DaoUserExportBiz;
import net.solarnetwork.central.user.export.dao.UserAdhocDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDataConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDestinationConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserOutputConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserAdhocDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskPK;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link UserExportBiz} class.
 * 
 * @author matt
 * @version 1.2
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
	private UserAdhocDatumExportTaskInfoDao adhocTaskDao;
	private UserNodeDao userNodeDao;
	private DatumStreamMetadataDao metaDao;

	private DaoUserExportBiz biz;

	@Before
	public void setup() {
		configurationDao = EasyMock.createMock(UserDatumExportConfigurationDao.class);
		dataConfigurationDao = EasyMock.createMock(UserDataConfigurationDao.class);
		destConfigurationDao = EasyMock.createMock(UserDestinationConfigurationDao.class);
		outputConfigurationDao = EasyMock.createMock(UserOutputConfigurationDao.class);
		taskDao = EasyMock.createMock(UserDatumExportTaskInfoDao.class);
		adhocTaskDao = EasyMock.createMock(UserAdhocDatumExportTaskInfoDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		metaDao = EasyMock.createMock(DatumStreamMetadataDao.class);

		biz = new DaoUserExportBiz(configurationDao, dataConfigurationDao, destConfigurationDao,
				outputConfigurationDao, taskDao, adhocTaskDao, userNodeDao, metaDao);
	}

	private void replayAll() {
		EasyMock.replay(configurationDao, dataConfigurationDao, destConfigurationDao,
				outputConfigurationDao, taskDao, adhocTaskDao, userNodeDao, metaDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(configurationDao, dataConfigurationDao, destConfigurationDao,
				outputConfigurationDao, taskDao, adhocTaskDao, userNodeDao, metaDao);
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
		List<ObjectDatumStreamMetadata> allMetas = allSourceIds.stream().map(e -> {
			return BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(), "UTC",
					ObjectDatumKind.Node, UUID.randomUUID().getMostSignificantBits(), e);
		}).collect(Collectors.toList());
		Capture<ObjectStreamCriteria> sourceFilterCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(sourceFilterCaptor))).andReturn(allMetas);

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

		ObjectStreamCriteria sourceFilter = sourceFilterCaptor.getValue();
		assertThat("Source filter node", sourceFilter.getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source filter start date", sourceFilter.getStartDate(),
				equalTo(fromJodaToInstant(exportDate)));
		assertThat("Source filter end date", sourceFilter.getEndDate(),
				equalTo(fromJodaToInstant(ScheduleType.Hourly.nextExportDate(exportDate))));
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
		List<ObjectDatumStreamMetadata> allMetas1 = allSourceIdsNode1.stream().map(e -> {
			return BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(), "UTC",
					ObjectDatumKind.Node, UUID.randomUUID().getMostSignificantBits(), e);
		}).collect(Collectors.toList());
		Set<String> allSourceIdsNode2 = new LinkedHashSet<>(Arrays.asList("/test/bam"));
		List<ObjectDatumStreamMetadata> allMetas2 = allSourceIdsNode2.stream().map(e -> {
			return BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(), "UTC",
					ObjectDatumKind.Node, UUID.randomUUID().getMostSignificantBits(), e);
		}).collect(Collectors.toList());
		expect(metaDao.findDatumStreamMetadata(assertWith(new Assertion<ObjectStreamCriteria>() {

			private int call = 0;

			@Override
			public void check(ObjectStreamCriteria sourceFilter) throws Throwable {
				call++;
				if ( call < 3 ) {
					assertThat("Source filter node " + call, sourceFilter.getNodeId(),
							equalTo(call == 1 ? TEST_NODE_ID : TEST_NODE_ID_2));
					assertThat("Source filter start date " + call, sourceFilter.getStartDate(),
							equalTo(fromJodaToInstant(exportDate)));
					assertThat("Source filter end date " + call, sourceFilter.getEndDate(),
							equalTo(fromJodaToInstant(ScheduleType.Hourly.nextExportDate(exportDate))));
				} else {
					fail("Expected only 2 calls to getAvailableSources(filter)");
				}
			}
		}))).andReturn(allMetas1).andReturn(allMetas2);

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

	@Test
	public void submitAdhocTask() {
		// given
		UserDatumExportConfiguration config = createConfiguration();

		// make ad hoc with no ID
		config.setId(null);

		expect(userNodeDao.findNodeIdsForUser(TEST_USER_ID))
				.andReturn(Collections.singleton(TEST_NODE_ID));

		Capture<UserAdhocDatumExportTaskInfo> taskCaptor = new Capture<>();

		UUID pk = UUID.randomUUID();
		expect(adhocTaskDao.store(capture(taskCaptor))).andReturn(pk);

		// when
		replayAll();
		UserAdhocDatumExportTaskInfo task = biz.submitAdhocDatumExportConfiguration(config);

		// then
		assertThat("Task created", task, notNullValue());

		assertThat("Task user ID", task.getUserId(), equalTo(TEST_USER_ID));
		assertThat("Task schedule", task.getScheduleType(), equalTo(ScheduleType.Adhoc));
		assertThat("Task config available", task.getConfig(), notNullValue());
		assertThat("Config name", task.getConfig().getName(), equalTo(config.getName()));
		assertThat("Node ID populated",
				task.getConfig().getDataConfiguration().getDatumFilter().getNodeIds(),
				arrayContaining(TEST_NODE_ID));
	}

}
