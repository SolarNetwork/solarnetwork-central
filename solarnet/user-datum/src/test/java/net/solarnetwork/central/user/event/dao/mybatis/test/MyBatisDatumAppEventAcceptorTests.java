/* ==================================================================
 * MyBatisDatumAppEventAcceptorTests.java - 5/06/2020 11:37:24 am
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

package net.solarnetwork.central.user.event.dao.mybatis.test;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.central.datum.domain.AggregateUpdatedEventInfo.AGGREGATE_UPDATED_TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.AggregateUpdatedEventInfo;
import net.solarnetwork.central.datum.domain.BasicDatumAppEvent;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.event.dao.mybatis.MyBatisDatumAppEventAcceptor;
import net.solarnetwork.central.user.event.dao.mybatis.MyBatisUserNodeEventHookConfigurationDao;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.codec.JsonUtils;

/**
 * Test cases for the {@link MyBatisDatumAppEventAcceptor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisDatumAppEventAcceptorTests extends AbstractMyBatisUserEventDaoTestSupport {

	private static final String TEST_SOURCE_ID = "test.source";

	private static final String TEST_SERVICE_ID = "test.service";

	private static final String TASK_TABLE = "solaruser.user_node_event_task";

	private MyBatisUserNodeEventHookConfigurationDao hookConfDao;
	private MyBatisDatumAppEventAcceptor dao;

	private User user;

	@Before
	public void setup() {
		hookConfDao = new MyBatisUserNodeEventHookConfigurationDao();
		hookConfDao.setSqlSessionFactory(getSqlSessionFactory());
		dao = new MyBatisDatumAppEventAcceptor();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		user = createNewUser(UUID.randomUUID().toString() + "@localhost");
		setupTestNode();
		setupUserNode(user.getId(), TEST_NODE_ID);

	}

	private List<Map<String, Object>> rows(String table) {
		return rows(table, "id");
	}

	private List<Map<String, Object>> rows(String table, String sort) {
		StringBuilder buf = new StringBuilder("select * from ");
		buf.append(table);
		if ( sort != null ) {
			buf.append(" order by ").append(sort);
		}
		return jdbcTemplate.queryForList(buf.toString());
	}

	@Test
	public void accept_noConfiguration() {
		// GIVEN
		AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo();
		info.setAggregation(Aggregation.Hour);
		info.setTimeStart(Instant.now().truncatedTo(ChronoUnit.HOURS));
		BasicDatumAppEvent event = new BasicDatumAppEvent(AGGREGATE_UPDATED_TOPIC,
				info.toEventProperties(), TEST_NODE_ID, TEST_SOURCE_ID);

		// WHEN
		dao.offerDatumEvent(event);

		// THEN
		List<Map<String, Object>> taskRows = rows(TASK_TABLE);
		assertThat("No tasks created because no configuration exists", taskRows, hasSize(0));
	}

	private UserNodeEventHookConfiguration createHookConf(Long userId, Long[] nodeIds,
			String[] sourceIds) {
		final UserNodeEventHookConfiguration hookConf = new UserNodeEventHookConfiguration(userId,
				Instant.now());
		hookConf.setName("Test");
		hookConf.setNodeIds(nodeIds);
		hookConf.setSourceIds(sourceIds);
		hookConf.setTopic(AGGREGATE_UPDATED_TOPIC);
		hookConf.setServiceIdentifier(TEST_SERVICE_ID);
		UserNodeEventHookConfiguration entity = hookConfDao.get(hookConfDao.save(hookConf));
		return entity;
	}

	private void assertTaskMap(Map<String, Object> taskRow, Long hookId, Long nodeId, String sourceId,
			AggregateUpdatedEventInfo info) {
		assertThat("Task hook ID matches", taskRow, hasEntry("hook_id", hookId));
		assertThat("Task node ID matches", taskRow, hasEntry("node_id", TEST_NODE_ID));
		assertThat("Task source ID matches", taskRow, hasEntry("source_id", TEST_SOURCE_ID));
		assertThat("Task created available", taskRow, hasEntry(equalTo("created"), notNullValue()));
		assertThat("Task data", taskRow, hasKey("jdata"));

		AggregateUpdatedEventInfo taskInfo = JsonUtils.getObjectFromJSON(taskRow.get("jdata").toString(),
				AggregateUpdatedEventInfo.class);
		assertThat("Task info same as event properties", taskInfo, equalTo(info));
	}

	@Test
	public void accept_oneConfiguration_exactNodeSource() {
		// GIVEN
		createHookConf(user.getId(), new Long[] { -99L }, new String[] { "foo" }); // create another that should not be returned
		final UserNodeEventHookConfiguration hookConf = createHookConf(user.getId(),
				new Long[] { TEST_NODE_ID }, new String[] { TEST_SOURCE_ID });

		// WHEN		
		AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo();
		info.setAggregation(Aggregation.Hour);
		info.setTimeStart(Instant.now().truncatedTo(ChronoUnit.HOURS));
		BasicDatumAppEvent event = new BasicDatumAppEvent(AGGREGATE_UPDATED_TOPIC,
				info.toEventProperties(), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.offerDatumEvent(event);

		// THEN
		List<Map<String, Object>> taskRows = rows(TASK_TABLE);
		assertThat("One task created for matching node/source", taskRows, hasSize(1));
		assertTaskMap(taskRows.get(0), hookConf.getId().getId(), TEST_NODE_ID, TEST_SOURCE_ID, info);
	}

	@Test
	public void accept_oneConfiguration_exactNodeAnySource() {
		// GIVEN
		final UserNodeEventHookConfiguration hookConf = createHookConf(user.getId(),
				new Long[] { TEST_NODE_ID }, null);
		createHookConf(user.getId(), new Long[] { -99L }, new String[] { "foo" }); // create another that should not be returned

		// WHEN		
		AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo();
		info.setAggregation(Aggregation.Hour);
		info.setTimeStart(Instant.now().truncatedTo(ChronoUnit.HOURS));
		BasicDatumAppEvent event = new BasicDatumAppEvent(AGGREGATE_UPDATED_TOPIC,
				info.toEventProperties(), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.offerDatumEvent(event);

		// THEN
		List<Map<String, Object>> taskRows = rows(TASK_TABLE);
		assertThat("One task created for matching node/source", taskRows, hasSize(1));
		assertTaskMap(taskRows.get(0), hookConf.getId().getId(), TEST_NODE_ID, TEST_SOURCE_ID, info);
	}

	@Test
	public void accept_oneConfiguration_anyNodeAnySource() {
		// GIVEN
		final UserNodeEventHookConfiguration hookConf = createHookConf(user.getId(), null, null);
		createHookConf(user.getId(), new Long[] { -99L }, new String[] { "foo" }); // create another that should not be returned

		// WHEN		
		AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo();
		info.setAggregation(Aggregation.Hour);
		info.setTimeStart(Instant.now().truncatedTo(ChronoUnit.HOURS));
		BasicDatumAppEvent event = new BasicDatumAppEvent(AGGREGATE_UPDATED_TOPIC,
				info.toEventProperties(), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.offerDatumEvent(event);

		// THEN
		List<Map<String, Object>> taskRows = rows(TASK_TABLE);
		assertThat("One task created for matching node/source", taskRows, hasSize(1));
		assertTaskMap(taskRows.get(0), hookConf.getId().getId(), TEST_NODE_ID, TEST_SOURCE_ID, info);
	}

	@Test
	public void accept_oneConfiguration_exactNodeSourcePattern() {
		// GIVEN
		final UserNodeEventHookConfiguration hookConf = createHookConf(user.getId(),
				new Long[] { TEST_NODE_ID }, new String[] { "t*" });
		createHookConf(user.getId(), new Long[] { TEST_NODE_ID }, new String[] { "foo" }); // create another that should not be returned

		// WHEN		
		AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo();
		info.setAggregation(Aggregation.Hour);
		info.setTimeStart(Instant.now().truncatedTo(ChronoUnit.HOURS));
		BasicDatumAppEvent event = new BasicDatumAppEvent(AGGREGATE_UPDATED_TOPIC,
				info.toEventProperties(), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.offerDatumEvent(event);

		// THEN
		List<Map<String, Object>> taskRows = rows(TASK_TABLE);
		assertThat("One task created for matching node/source", taskRows, hasSize(1));
		assertTaskMap(taskRows.get(0), hookConf.getId().getId(), TEST_NODE_ID, TEST_SOURCE_ID, info);
	}

	@Test
	public void accept_multiConfiguration_exactNodeSourcePattern() {
		// GIVEN
		final UserNodeEventHookConfiguration hookConf = createHookConf(user.getId(),
				new Long[] { TEST_NODE_ID }, new String[] { "t*" });
		final UserNodeEventHookConfiguration hookConf2 = createHookConf(user.getId(),
				new Long[] { TEST_NODE_ID }, new String[] { TEST_SOURCE_ID });
		createHookConf(user.getId(), new Long[] { TEST_NODE_ID }, new String[] { "foo" }); // create another that should not be returned

		// WHEN		
		AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo();
		info.setAggregation(Aggregation.Hour);
		info.setTimeStart(Instant.now().truncatedTo(ChronoUnit.HOURS));
		BasicDatumAppEvent event = new BasicDatumAppEvent(AGGREGATE_UPDATED_TOPIC,
				info.toEventProperties(), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.offerDatumEvent(event);

		// THEN
		List<Long> hookIds = asList(hookConf.getId().getId(), hookConf2.getId().getId());

		Map<Long, Map<String, Object>> taskRows = rows(TASK_TABLE).stream()
				.collect(toMap(r -> (Long) r.get("hook_id"), r -> r));
		assertThat("Two tasks created for matching node/source", taskRows.keySet(), hasSize(2));
		for ( int i = 0; i < hookIds.size(); i++ ) {
			Long hookId = hookIds.get(i);
			assertTaskMap(taskRows.get(hookId), hookId, TEST_NODE_ID, TEST_SOURCE_ID, info);
		}
	}

}
