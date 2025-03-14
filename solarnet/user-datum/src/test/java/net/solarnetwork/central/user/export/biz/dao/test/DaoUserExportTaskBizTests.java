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

import static net.solarnetwork.central.security.SecurityTokenType.User;
import static net.solarnetwork.central.security.SecurityUtils.becomeToken;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.assertj.core.api.BDDAssertions.then;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import java.time.Instant;
import java.time.ZonedDateTime;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.export.biz.dao.DaoUserExportTaskBiz;
import net.solarnetwork.central.user.export.dao.UserAdhocDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.domain.UserAdhocDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskPK;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link UserExportBiz} class.
 * 
 * @author matt
 * @version 2.1
 */
public class DaoUserExportTaskBizTests {

	private static final Long TEST_USER_ID = -1L;
	private static final Long TEST_NODE_ID = -2L;
	private static final Long TEST_NODE_ID_2 = -3L;
	private static final AtomicLong ID_GENERATOR = new AtomicLong(-999L);

	private UserDatumExportTaskInfoDao taskDao;
	private UserAdhocDatumExportTaskInfoDao adhocTaskDao;
	private UserNodeDao userNodeDao;
	private DatumStreamMetadataDao metaDao;

	private DaoUserExportTaskBiz biz;

	@BeforeEach
	public void setup() {
		taskDao = EasyMock.createMock(UserDatumExportTaskInfoDao.class);
		adhocTaskDao = EasyMock.createMock(UserAdhocDatumExportTaskInfoDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		metaDao = EasyMock.createMock(DatumStreamMetadataDao.class);

		biz = new DaoUserExportTaskBiz(taskDao, adhocTaskDao, userNodeDao, metaDao);
	}

	private void replayAll() {
		EasyMock.replay(taskDao, adhocTaskDao, userNodeDao, metaDao);
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(taskDao, adhocTaskDao, userNodeDao, metaDao);
		SecurityUtils.removeAuthentication();
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
		UserDatumExportConfiguration config = createConfiguration();
		ZonedDateTime now = ZonedDateTime.now(config.zone());
		expect(userNodeDao.findNodeIdsForUser(TEST_USER_ID))
				.andReturn(Collections.singleton(TEST_NODE_ID));

		Capture<UserDatumExportTaskInfo> taskCaptor = new Capture<>();

		ZonedDateTime exportDate = ScheduleType.Hourly.exportDate(now);
		expect(taskDao.save(capture(taskCaptor))).andReturn(
				new UserDatumExportTaskPK(TEST_USER_ID, ScheduleType.Hourly, exportDate.toInstant()));

		// when
		replayAll();
		UserDatumExportTaskInfo task = biz.submitDatumExportConfiguration(config,
				exportDate.toInstant());

		// then
		assertThat("Task created", task, notNullValue());

		assertThat("Task user ID", task.getUserId(), equalTo(TEST_USER_ID));
		assertThat("Task schedule", task.getScheduleType(), equalTo(ScheduleType.Hourly));
		assertThat("Task date", task.getExportDate(), equalTo(exportDate.toInstant()));
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

		UserDatumExportConfiguration config = createConfiguration();
		ZonedDateTime now = ZonedDateTime.now(config.zone());
		UserDataConfiguration dataConfig = new UserDataConfiguration();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setSourceId("/test/**");
		dataConfig.setFilter(filter);
		config.setUserDataConfiguration(dataConfig);

		expect(userNodeDao.findNodeIdsForUser(TEST_USER_ID))
				.andReturn(Collections.singleton(TEST_NODE_ID));

		ZonedDateTime exportDate = ScheduleType.Hourly.exportDate(now);

		Set<String> allSourceIds = new LinkedHashSet<>(
				Arrays.asList("/foo/bar", "/test/foo", "/test/bar"));
		List<ObjectDatumStreamMetadata> allMetas = allSourceIds.stream().map(e -> {
			return BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(), "UTC",
					ObjectDatumKind.Node, UUID.randomUUID().getMostSignificantBits(), e);
		}).collect(Collectors.toList());
		Capture<ObjectStreamCriteria> sourceFilterCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(sourceFilterCaptor))).andReturn(allMetas);

		Capture<UserDatumExportTaskInfo> taskCaptor = new Capture<>();

		expect(taskDao.save(capture(taskCaptor))).andReturn(
				new UserDatumExportTaskPK(TEST_USER_ID, ScheduleType.Hourly, exportDate.toInstant()));

		// when
		replayAll();
		UserDatumExportTaskInfo task = biz.submitDatumExportConfiguration(config,
				exportDate.toInstant());

		// then
		assertThat("Task created", task, notNullValue());

		assertThat("Task user ID", task.getUserId(), equalTo(TEST_USER_ID));
		assertThat("Task schedule", task.getScheduleType(), equalTo(ScheduleType.Hourly));
		assertThat("Task date", task.getExportDate(), equalTo(exportDate.toInstant()));
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
				equalTo(exportDate.toInstant()));
		assertThat("Source filter end date", sourceFilter.getEndDate(),
				equalTo(ScheduleType.Hourly.nextExportDate(exportDate).toInstant()));
	}

	@Test
	public void submitTaskResolveSourceIdPatternMultipleNodes() {
		// given
		biz.setPathMatcher(new AntPathMatcher());

		UserDatumExportConfiguration config = createConfiguration();
		ZonedDateTime now = ZonedDateTime.now(config.zone());
		UserDataConfiguration dataConfig = new UserDataConfiguration();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setSourceId("/test/**");
		dataConfig.setFilter(filter);
		config.setUserDataConfiguration(dataConfig);

		expect(userNodeDao.findNodeIdsForUser(TEST_USER_ID))
				.andReturn(new LinkedHashSet<>(Arrays.asList(TEST_NODE_ID, TEST_NODE_ID_2)));

		ZonedDateTime exportDate = ScheduleType.Hourly.exportDate(now);

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
							equalTo(exportDate.toInstant()));
					assertThat("Source filter end date " + call, sourceFilter.getEndDate(),
							equalTo(ScheduleType.Hourly.nextExportDate(exportDate).toInstant()));
				} else {
					fail("Expected only 2 calls to getAvailableSources(filter)");
				}
			}
		}))).andReturn(allMetas1).andReturn(allMetas2);

		Capture<UserDatumExportTaskInfo> taskCaptor = new Capture<>();

		expect(taskDao.save(capture(taskCaptor))).andReturn(
				new UserDatumExportTaskPK(TEST_USER_ID, ScheduleType.Hourly, exportDate.toInstant()));

		// when
		replayAll();
		UserDatumExportTaskInfo task = biz.submitDatumExportConfiguration(config,
				exportDate.toInstant());

		// then
		assertThat("Task created", task, notNullValue());

		assertThat("Task user ID", task.getUserId(), equalTo(TEST_USER_ID));
		assertThat("Task schedule", task.getScheduleType(), equalTo(ScheduleType.Hourly));
		assertThat("Task date", task.getExportDate(), equalTo(exportDate.toInstant()));
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
		// GIVEN
		UserDatumExportConfiguration config = createConfiguration();

		// make ad hoc with no ID
		config.setId(null);

		expect(userNodeDao.findNodeIdsForUser(TEST_USER_ID))
				.andReturn(Collections.singleton(TEST_NODE_ID));

		Capture<UserAdhocDatumExportTaskInfo> taskCaptor = new Capture<>();

		UUID pk = UUID.randomUUID();
		expect(adhocTaskDao.save(capture(taskCaptor))).andReturn(pk);

		// WHEN
		replayAll();
		UserAdhocDatumExportTaskInfo task = biz.submitAdhocDatumExportConfiguration(config);

		// THEN
		// @formatter:off
		then(task)
			.as("Task created")
			.isNotNull()
			.as("Task user ID")
			.returns(TEST_USER_ID, UserAdhocDatumExportTaskInfo::getUserId)
			.as("Schedule is Adhoc")
			.returns(ScheduleType.Adhoc, UserAdhocDatumExportTaskInfo::getScheduleType)
			.as("No token")
			.returns(null, UserAdhocDatumExportTaskInfo::getTokenId)
			.as("Config name")
			;
		// @formatter:on
		then(task.getConfig().getName()).isEqualTo(config.getName());
		then(task.getConfig().getDataConfiguration().getDatumFilter().getNodeIds())
				.contains(TEST_NODE_ID);
	}

	@Test
	public void submitAdhocTask_withToken() {
		// GIVEN
		UserDatumExportConfiguration config = createConfiguration();

		// make ad hoc with no ID
		config.setId(null);

		expect(userNodeDao.findNodeIdsForUser(TEST_USER_ID))
				.andReturn(Collections.singleton(TEST_NODE_ID));

		Capture<UserAdhocDatumExportTaskInfo> taskCaptor = new Capture<>();

		UUID pk = UUID.randomUUID();
		expect(adhocTaskDao.save(capture(taskCaptor))).andReturn(pk);

		// WHEN
		replayAll();
		SecurityToken auth = becomeToken(randomString(), User, TEST_USER_ID, null);
		UserAdhocDatumExportTaskInfo task = biz.submitAdhocDatumExportConfiguration(config);

		// THEN
		// @formatter:off
		then(task)
			.as("Task created")
			.isNotNull()
			.as("Task user ID")
			.returns(TEST_USER_ID, UserAdhocDatumExportTaskInfo::getUserId)
			.as("Schedule is Adhoc")
			.returns(ScheduleType.Adhoc, UserAdhocDatumExportTaskInfo::getScheduleType)
			.as("No token")
			.returns(auth.getToken(), UserAdhocDatumExportTaskInfo::getTokenId)
			.as("Config name")
			;
		// @formatter:on
		then(task.getConfig().getName()).isEqualTo(config.getName());
		then(task.getConfig().getDataConfiguration().getDatumFilter().getNodeIds())
				.contains(TEST_NODE_ID);
	}

	@Test
	public void submitAdhocTask_withMultipleNodes_withSourceIdPatterns() {
		// GIVEN
		biz.setPathMatcher(new AntPathMatcher());

		UserDatumExportConfiguration config = createConfiguration();

		// make ad hoc with no ID
		config.setId(null);

		UserDataConfiguration dataConfig = new UserDataConfiguration();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(Instant.parse("2025-03-01T00:00:00Z"));
		filter.setEndDate(Instant.parse("2025-03-02T00:00:00Z"));
		filter.setNodeIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_2 });
		filter.setSourceIds(new String[] { "/test/**", "/foo/**" });
		dataConfig.setFilter(filter);
		config.setUserDataConfiguration(dataConfig);

		Set<String> allSourceIdsNode1 = new LinkedHashSet<>(
				Arrays.asList("/foo/bar", "/test/foo", "/test/bar", "/some/other"));
		List<ObjectDatumStreamMetadata> allMetas1 = allSourceIdsNode1.stream().map(e -> {
			return BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(), "UTC",
					ObjectDatumKind.Node, UUID.randomUUID().getMostSignificantBits(), e);
		}).collect(Collectors.toList());
		Set<String> allSourceIdsNode2 = new LinkedHashSet<>(Arrays.asList("/test/bam", "/bam/bam"));
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
							equalTo(filter.getStartDate()));
					assertThat("Source filter end date " + call, sourceFilter.getEndDate(),
							equalTo(filter.getEndDate()));
				} else {
					fail("Expected only 2 calls to getAvailableSources(filter)");
				}
			}
		}))).andReturn(allMetas1).andReturn(allMetas2);

		Capture<UserAdhocDatumExportTaskInfo> taskCaptor = new Capture<>();

		UUID pk = UUID.randomUUID();
		expect(adhocTaskDao.save(capture(taskCaptor))).andReturn(pk);

		// WHEN
		replayAll();
		SecurityToken auth = becomeToken(randomString(), User, TEST_USER_ID, null);
		UserAdhocDatumExportTaskInfo task = biz.submitAdhocDatumExportConfiguration(config);

		// THEN
		// @formatter:off
		then(task)
			.as("Task created")
			.isNotNull()
			.as("Task user ID")
			.returns(TEST_USER_ID, UserAdhocDatumExportTaskInfo::getUserId)
			.as("Schedule is Adhoc")
			.returns(ScheduleType.Adhoc, UserAdhocDatumExportTaskInfo::getScheduleType)
			.as("No token")
			.returns(auth.getToken(), UserAdhocDatumExportTaskInfo::getTokenId)
			.as("Config name")
			;
		// @formatter:on
		then(task.getConfig().getName()).isEqualTo(config.getName());
		then(task.getConfig().getDataConfiguration().getDatumFilter().getNodeIds())
				.as("Node IDs given in filter patterns are resolved")
				.containsExactlyInAnyOrder(TEST_NODE_ID, TEST_NODE_ID_2);
		then(task.getConfig().getDataConfiguration().getDatumFilter().getSourceIds())
				.as("Source IDs matching filter patterns are resolved")
				.containsExactlyInAnyOrder("/foo/bar", "/test/foo", "/test/bar", "/test/bam");
	}

}
