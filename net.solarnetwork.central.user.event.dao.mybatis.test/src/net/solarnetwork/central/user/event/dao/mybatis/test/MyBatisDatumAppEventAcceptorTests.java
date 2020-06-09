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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.sql.Timestamp;
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
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeEventHookConfigurationDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNodeEvent;
import net.solarnetwork.central.user.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.user.domain.UserNodeEventTask;
import net.solarnetwork.central.user.domain.UserNodeEventTaskState;
import net.solarnetwork.central.user.event.dao.mybatis.MyBatisDatumAppEventAcceptor;
import net.solarnetwork.util.JsonUtils;

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

	private static final String TASK_RESULT_TABLE = "solaruser.user_node_event_task_result";

	private MyBatisUserNodeEventHookConfigurationDao hookConfDao;
	private MyBatisDatumAppEventAcceptor dao;

	private UserNodeEventHookConfiguration lastHook;
	private AggregateUpdatedEventInfo lastEventInfo;

	@Before
	public void setup() {
		hookConfDao = new MyBatisUserNodeEventHookConfigurationDao();
		hookConfDao.setSqlSessionFactory(getSqlSessionFactory());
		dao = new MyBatisDatumAppEventAcceptor();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		lastHook = null;
		lastEventInfo = null;
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
		lastHook = entity;
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
		final User user = createNewUser("foo@localhost");
		setupTestNode();
		setupUserNode(user.getId(), TEST_NODE_ID);
		createHookConf(user.getId(), new Long[] { -99L }, new String[] { "foo" }); // create another that should not be returned
		final UserNodeEventHookConfiguration hookConf = createHookConf(user.getId(),
				new Long[] { TEST_NODE_ID }, new String[] { TEST_SOURCE_ID });

		// WHEN		
		AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo();
		info.setAggregation(Aggregation.Hour);
		info.setTimeStart(Instant.now().truncatedTo(ChronoUnit.HOURS));
		lastEventInfo = info;
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
		final User user = createNewUser("foo@localhost");
		setupTestNode();
		setupUserNode(user.getId(), TEST_NODE_ID);
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
		final User user = createNewUser("foo@localhost");
		setupTestNode();
		setupUserNode(user.getId(), TEST_NODE_ID);
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
		final User user = createNewUser("foo@localhost");
		setupTestNode();
		setupUserNode(user.getId(), TEST_NODE_ID);
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
		final User user = createNewUser("foo@localhost");
		setupTestNode();
		setupUserNode(user.getId(), TEST_NODE_ID);
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

	@Test
	public void claim_noTasks() {
		// GIVEN

		// WHEN
		UserNodeEvent claimed = dao.claimQueuedTask(AGGREGATE_UPDATED_TOPIC);

		// THEN
		assertThat("No task claimed because none exist", claimed, nullValue());

	}

	@Test
	public void claim_task() {
		// GIVEN
		accept_oneConfiguration_exactNodeSource();

		// WHEN
		UserNodeEvent claimed = dao.claimQueuedTask(AGGREGATE_UPDATED_TOPIC);

		// THEN
		assertThat("Task claimed", claimed, notNullValue());

		UserNodeEventTask claimedTask = claimed.getTask();
		assertThat("Claimed task available", claimedTask, notNullValue());
		assertThat("Claimed task ID populated", claimedTask.getId(), notNullValue());
		assertThat("Claimed task user ID matches", claimedTask.getUserId(),
				equalTo(lastHook.getUserId()));
		assertThat("Claimed task node ID matches", claimedTask.getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Claimed task source ID matches", claimedTask.getSourceId(), equalTo(TEST_SOURCE_ID));

		assertThat("Claimed task has aggregation property", claimedTask.getTaskProperties(),
				hasEntry("aggregationKey", lastEventInfo.getAggregationKey()));
		assertThat("Claimed task has timestamp property", claimedTask.getTaskProperties(),
				hasEntry("timestamp", lastEventInfo.getTimestamp()));

		UserNodeEventHookConfiguration claimedHook = claimed.getConfig();
		assertThat("Claimed task's hook available", claimedHook, notNullValue());
		assertThat("Claimed hook matches", claimedHook, equalTo(lastHook));
		assertThat("Claimed hook user matches", claimedHook.getUserId(), equalTo(lastHook.getUserId()));
	}

	@Test
	public void complete_task() throws Exception {
		// GIVEN
		accept_oneConfiguration_exactNodeSource();

		// WHEN
		UserNodeEvent claimed = dao.claimQueuedTask(AGGREGATE_UPDATED_TOPIC);
		UserNodeEventTask claimedTask = claimed.getTask();
		Thread.sleep(400L);
		claimedTask.setCompleted(Instant.now());
		claimedTask.setSuccess(true);
		claimedTask.setStatus(UserNodeEventTaskState.Completed);
		claimedTask.setMessage("Good one.");
		dao.taskCompleted(claimedTask);

		// THEN
		Map<UUID, Map<String, Object>> taskRows = rows(TASK_TABLE).stream()
				.collect(toMap(r -> (UUID) r.get("id"), r -> r));
		assertThat("Task removed from task table", taskRows.keySet(), hasSize(0));

		taskRows = rows(TASK_RESULT_TABLE).stream().collect(toMap(r -> (UUID) r.get("id"), r -> r));
		assertThat("Task added to task result table", taskRows.keySet(), hasSize(1));

		Map<String, Object> row = taskRows.get(claimedTask.getId());
		assertThat("Task result row ID preserved", row, notNullValue());
		assertThat("Task result hook ID preserved", row.get("hook_id"),
				equalTo(claimedTask.getHookId()));
		assertThat("Task result node ID preserved", row.get("node_id"),
				equalTo(claimedTask.getNodeId()));
		assertThat("Task result source ID preserved", row.get("source_id"),
				equalTo(claimedTask.getSourceId()));
		assertThat("Task result status persisted", row.get("status"),
				equalTo(String.valueOf(claimedTask.getStatusKey())));
		assertThat("Task result success persisted", row.get("success"),
				equalTo(claimedTask.getSuccess()));
		assertThat("Task result message persisted", row.get("message"),
				equalTo(claimedTask.getMessage()));
		assertThat("Task result completed date persisted", row.get("completed"),
				equalTo(new Timestamp(claimedTask.getCompleted().toEpochMilli())));
	}
}
