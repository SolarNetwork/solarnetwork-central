/* ==================================================================
 * MyBatisUserNodeEventTaskDaoTests.java - 5/11/2021 8:07:55 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.central.datum.domain.AggregateUpdatedEventInfo.AGGREGATE_UPDATED_TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.event.dao.mybatis.MyBatisDatumAppEventAcceptor;
import net.solarnetwork.central.user.event.dao.mybatis.MyBatisUserNodeEventHookConfigurationDao;
import net.solarnetwork.central.user.event.dao.mybatis.MyBatisUserNodeEventTaskDao;
import net.solarnetwork.central.user.event.domain.UserNodeEvent;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.user.event.domain.UserNodeEventTask;
import net.solarnetwork.central.user.event.domain.UserNodeEventTaskState;

/**
 * Test cases for the {@link MyBatisUserNodeEventTaskDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserNodeEventTaskDaoTests extends AbstractMyBatisUserEventDaoTestSupport {

	private static final String TEST_SOURCE_ID = "test.source";

	private static final String TEST_SERVICE_ID = "test.service";

	private static final String TASK_TABLE = "solaruser.user_node_event_task";

	private static final String TASK_RESULT_TABLE = "solaruser.user_node_event_task_result";

	private MyBatisUserNodeEventHookConfigurationDao hookConfDao;
	private MyBatisDatumAppEventAcceptor acceptor;
	private MyBatisUserNodeEventTaskDao dao;

	private User user;
	private UserNodeEventHookConfiguration lastHook;
	private AggregateUpdatedEventInfo lastEventInfo;

	@Before
	public void setup() {
		hookConfDao = new MyBatisUserNodeEventHookConfigurationDao();
		hookConfDao.setSqlSessionFactory(getSqlSessionFactory());
		acceptor = new MyBatisDatumAppEventAcceptor();
		acceptor.setSqlSessionFactory(getSqlSessionFactory());
		dao = new MyBatisUserNodeEventTaskDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		lastHook = null;
		lastEventInfo = null;

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

	private void accept_oneConfiguration_exactNodeSource() {
		createHookConf(user.getId(), new Long[] { -99L }, new String[] { "foo" }); // create another that should not be returned
		createHookConf(user.getId(), new Long[] { TEST_NODE_ID }, new String[] { TEST_SOURCE_ID });

		// WHEN		
		AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo();
		info.setAggregation(Aggregation.Hour);
		info.setTimeStart(Instant.now().truncatedTo(ChronoUnit.HOURS));
		BasicDatumAppEvent event = new BasicDatumAppEvent(AGGREGATE_UPDATED_TOPIC,
				info.toEventProperties(), TEST_NODE_ID, TEST_SOURCE_ID);
		acceptor.offerDatumEvent(event);
		lastEventInfo = info;
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
		claimedTask.setCompleted(Instant.now().truncatedTo(ChronoUnit.MILLIS));
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

	@Test
	public void purge_noTasks() {
		// GIVEN

		// WHEN
		long count = dao.purgeCompletedTasks(Instant.now());

		// THEN
		assertThat("Nothing purged because nothing exists", count, equalTo(0L));
	}

	@Test
	public void purge_nothingOlder() throws Exception {
		// GIVEN
		complete_task();

		// WHEN
		long count = dao.purgeCompletedTasks(Instant.now().minus(1, ChronoUnit.DAYS));

		// THEN
		assertThat("Nothing purged because nothing older", count, equalTo(0L));
	}

	@Test
	public void purge_task() {
		// GIVEN
		accept_oneConfiguration_exactNodeSource();

		// WHEN
		long count = dao.purgeCompletedTasks(Instant.now().plus(1, ChronoUnit.DAYS));

		// THEN
		assertThat("One task purged because both older", count, equalTo(1L));
	}

	@Test
	public void purge_tasks() throws Exception {
		// GIVEN
		createHookConf(user.getId(), new Long[] { TEST_NODE_ID }, new String[] { TEST_SOURCE_ID });

		// create 3 tasks @ 3 dates
		Instant ts = null;
		for ( int i = 0; i < 3; i++ ) {
			ts = Instant.now();
			AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo();
			info.setAggregation(Aggregation.Hour);
			info.setTimeStart(Instant.now().truncatedTo(ChronoUnit.HOURS));
			lastEventInfo = info;
			BasicDatumAppEvent event = new BasicDatumAppEvent(AGGREGATE_UPDATED_TOPIC, ts,
					info.toEventProperties(), TEST_NODE_ID, TEST_SOURCE_ID);
			acceptor.offerDatumEvent(event);
			Thread.sleep(300);
		}

		// WHEN
		long count = dao.purgeCompletedTasks(ts.truncatedTo(ChronoUnit.MILLIS));

		// THEN
		assertThat("Deleted 2 tasks older than last task", count, equalTo(2L));
		List<Map<String, Object>> taskRows = rows(TASK_TABLE);
		assertThat("One task remains", taskRows, hasSize(1));
		Timestamp timestamp = (Timestamp) taskRows.get(0).get("created");
		assertThat("Remaining row is task with most recent timestamp", timestamp.getTime(),
				equalTo(ts.toEpochMilli()));
	}

	@Test
	public void purge_taskButNotResult() throws Exception {
		// GIVEN
		createHookConf(user.getId(), new Long[] { TEST_NODE_ID }, new String[] { TEST_SOURCE_ID });

		// create 3 tasks @ 3 dates
		Instant ts = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(1, ChronoUnit.HOURS);
		for ( int i = 0; i < 3; i++ ) {
			ts = ts.plusSeconds(60);
			AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo();
			info.setAggregation(Aggregation.Hour);
			info.setTimeStart(Instant.now().truncatedTo(ChronoUnit.HOURS));
			lastEventInfo = info;
			BasicDatumAppEvent event = new BasicDatumAppEvent(AGGREGATE_UPDATED_TOPIC, ts,
					info.toEventProperties(), TEST_NODE_ID, TEST_SOURCE_ID);
			acceptor.offerDatumEvent(event);
			if ( i > 1 ) {
				UserNodeEvent claimed = dao.claimQueuedTask(AGGREGATE_UPDATED_TOPIC);
				UserNodeEventTask claimedTask = claimed.getTask();
				Thread.sleep(400L);
				claimedTask.setCompleted(ts);
				claimedTask.setSuccess(true);
				claimedTask.setStatus(UserNodeEventTaskState.Completed);
				claimedTask.setMessage("Good one.");
				dao.taskCompleted(claimedTask);
			}
		}

		// WHEN
		long count = dao.purgeCompletedTasks(ts);

		// THEN
		List<Map<String, Object>> taskRows = rows(TASK_TABLE);
		List<Map<String, Object>> taskResultRows = rows(TASK_RESULT_TABLE);
		assertThat("Deleted 1 tasks older than last task", count, equalTo(1L));
		assertThat("One task remains because _created_ older than delete date", taskRows, hasSize(1));
		assertThat("One task result remains because _completed_ not older than delete date",
				taskResultRows, hasSize(1));
	}

	@Test
	public void purge_tasksAndResults() throws Exception {
		// GIVEN
		createHookConf(user.getId(), new Long[] { TEST_NODE_ID }, new String[] { TEST_SOURCE_ID });

		// create 4 tasks @ 4 dates
		Instant ts = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(1, ChronoUnit.HOURS);
		for ( int i = 0; i < 4; i++ ) {
			ts = ts.plusSeconds(60);
			AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo();
			info.setAggregation(Aggregation.Hour);
			info.setTimeStart(Instant.now().truncatedTo(ChronoUnit.HOURS));
			lastEventInfo = info;
			BasicDatumAppEvent event = new BasicDatumAppEvent(AGGREGATE_UPDATED_TOPIC, ts,
					info.toEventProperties(), TEST_NODE_ID, TEST_SOURCE_ID);
			acceptor.offerDatumEvent(event);

			// last 2 tasks will be turned into results
			if ( i > 1 ) {
				UserNodeEvent claimed = dao.claimQueuedTask(AGGREGATE_UPDATED_TOPIC);
				UserNodeEventTask claimedTask = claimed.getTask();
				Thread.sleep(400L);
				claimedTask.setCompleted(ts.plusSeconds(30));
				claimedTask.setSuccess(true);
				claimedTask.setStatus(UserNodeEventTaskState.Completed);
				claimedTask.setMessage("Good one.");
				dao.taskCompleted(claimedTask);
			}
		}

		// WHEN
		long count = dao.purgeCompletedTasks(ts.plusSeconds(31));

		// THEN
		List<Map<String, Object>> taskRows = rows(TASK_TABLE);
		List<Map<String, Object>> taskResultRows = rows(TASK_RESULT_TABLE);
		assertThat("No tasks remain", taskRows, hasSize(0));
		assertThat("No task results remain", taskResultRows, hasSize(0));
		assertThat("Deleted 2 tasks + 2 task results older than delete date", count, equalTo(4L));
	}

}
