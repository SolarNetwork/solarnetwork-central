/* ==================================================================
 * JdbcUserNodeInstructionTaskDaoTests.java - 11/11/2025 9:49:09 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.jdbc.test;

import static java.time.Instant.now;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.dao.jdbc.test.UserJdbcTestUtils.allUserNodeInstructionTaskEntityData;
import static net.solarnetwork.central.user.dao.jdbc.test.UserJdbcTestUtils.newUserNodeInstructionTaskEntity;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.user.dao.BasicUserNodeInstructionTaskFilter;
import net.solarnetwork.central.user.dao.jdbc.JdbcUserNodeInstructionTaskDao;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.dao.Entity;

/**
 * Test cases for the {@link JdbcUserNodeInstructionTaskDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcUserNodeInstructionTaskDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static final String TEST_SCHEDULE = "60";

	private JdbcUserNodeInstructionTaskDao dao;
	private Long userId;
	private Long nodeId;

	private UserNodeInstructionTaskEntity last;

	@BeforeEach
	public void setup() {
		dao = new JdbcUserNodeInstructionTaskDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);

		Long locId = randomLong();
		nodeId = randomLong();
		setupTestLocation(locId);

		setupTestNode(nodeId, locId);
		setupTestUserNode(userId, nodeId);
	}

	@Test
	public void entityKey() {
		UserLongCompositePK id = new UserLongCompositePK(randomLong(), randomLong());
		UserNodeInstructionTaskEntity result = dao.entityKey(id);

		// @formatter:off
		then(result)
			.as("Entity for key returned")
			.isNotNull()
			.as("ID of entity from provided value")
			.returns(id, Entity::getId)
			;
		// @formatter:on
	}

	@Test
	public void insert() {
		// GIVEN
		Map<String, Object> props = singletonMap("foo", "bar");
		Map<String, Object> rprops = singletonMap("bim", "bam");
		// @formatter:off
		UserNodeInstructionTaskEntity conf = newUserNodeInstructionTaskEntity(
				userId,
				randomString(),
				nodeId,
				randomString(),
				TEST_SCHEDULE,
				BasicClaimableJobState.Queued,
				now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
				props,
				now().truncatedTo(ChronoUnit.SECONDS),
				randomString(),
				rprops
				);
		// @formatter:on

		// WHEN
		UserLongCompositePK result = dao.create(userId, conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserLongCompositePK::getUserId)
			.extracting(UserLongCompositePK::getEntityId)
			.as("Config ID generated")
			.isNotNull()
			;

		List<Map<String, Object>> data = allUserNodeInstructionTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asInstanceOf(list(Map.class))
			.element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row ID generated")
			.containsKey("id")
			.as("No creation date")
			.doesNotContainKey("created")
			.as("No modification date")
			.doesNotContainKey("modified")
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			.as("Row name")
			.containsEntry("cname", conf.getName())
			.as("Row node ID")
			.containsEntry("node_id", conf.getNodeId())
			.as("Row topic")
			.containsEntry("topic", conf.getTopic())
			.as("Row schedule")
			.containsEntry("schedule", conf.getSchedule())
			.as("Row service properties")
			.hasEntrySatisfying("sprops", o -> {
				then(JsonUtils.getStringMap(o.toString()))
					.as("Row service props")
					.isEqualTo(props)
					;
			})
			.as("Row status")
			.containsEntry("status", conf.getState().keyValue())
			.as("Row execution date")
			.containsEntry("exec_at", Timestamp.from(conf.getExecuteAt()))
			.as("Row last execution date NOT inserted")
			.containsEntry("last_exec_at", null)
			.as("Row message NOT inserted")
			.containsEntry("message", null)
			.as("Row result properties NOT inserted")
			.containsEntry("rprops", null)
			;
		// @formatter:on
		last = dao.get(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		UserNodeInstructionTaskEntity result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		final int userCount = 2;
		final int taskCount = 2;
		final List<UserNodeInstructionTaskEntity> confs = new ArrayList<>(userCount * taskCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			final Long locId = CommonDbTestUtils.insertLocation(jdbcTemplate, "GB", "UTC");
			final Long nodeId = CommonDbTestUtils.insertNode(jdbcTemplate, locId);
			CommonDbTestUtils.insertUserNode(jdbcTemplate, userId, nodeId);
			for ( int t = 0; t < taskCount; t++ ) {
				// @formatter:off
				UserNodeInstructionTaskEntity entity = newUserNodeInstructionTaskEntity(
						userId,
						randomString(),
						nodeId,
						randomString(),
						TEST_SCHEDULE,
						BasicClaimableJobState.Queued,
						now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
						null,
						null,
						null,
						null
						);
				// @formatter:on
				var pk = dao.save(entity);
				confs.add(dao.get(pk));
			}
		}

		// WHEN
		final UserNodeInstructionTaskEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		final var filter = new BasicUserNodeInstructionTaskFilter();
		filter.setUserId(randomConf.getUserId());
		var results = dao.findFiltered(filter);

		// THEN
		UserNodeInstructionTaskEntity[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId()))
				.toArray(UserNodeInstructionTaskEntity[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

	@Test
	public void findFiltered_forUserAndState() throws Exception {
		final int userCount = 2;
		final int taskCount = 10;
		final List<UserNodeInstructionTaskEntity> confs = new ArrayList<>(userCount * taskCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			final Long locId = CommonDbTestUtils.insertLocation(jdbcTemplate, "GB", "UTC");
			final Long nodeId = CommonDbTestUtils.insertNode(jdbcTemplate, locId);
			CommonDbTestUtils.insertUserNode(jdbcTemplate, userId, nodeId);
			for ( int t = 0; t < taskCount; t++ ) {
				// @formatter:off
				UserNodeInstructionTaskEntity entity = newUserNodeInstructionTaskEntity(
						userId,
						randomString(),
						nodeId,
						randomString(),
						TEST_SCHEDULE,
						BasicClaimableJobState.Queued,
						now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
						null,
						null,
						null,
						null
						);
				// @formatter:on
				var pk = dao.save(entity);
				confs.add(dao.get(pk));
			}
		}

		// WHEN
		final UserNodeInstructionTaskEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		final var filter = new BasicUserNodeInstructionTaskFilter();
		filter.setUserId(randomConf.getUserId());
		filter.setClaimableJobStates(new BasicClaimableJobState[] { BasicClaimableJobState.Claimed,
				BasicClaimableJobState.Executing });
		var results = dao.findFiltered(filter);

		// THEN
		UserNodeInstructionTaskEntity[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId())
						&& EnumSet.of(BasicClaimableJobState.Claimed, BasicClaimableJobState.Executing)
								.contains(e.getState()))
				.toArray(UserNodeInstructionTaskEntity[]::new);
		then(results).as("Results for single user and specified states returned")
				.containsExactly(expected);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// populate some result data
		UserNodeInstructionTaskEntity orig = last.copyWithId(last.getId());
		orig.setState(BasicClaimableJobState.Queued);
		orig.setExecuteAt(now().plusMillis(818));
		orig.setLastExecuteAt(now().plusMillis(-181));
		orig.setMessage(randomString());
		orig.setResultProps(Map.of("pop", "bang"));
		dao.updateTask(orig, last.getState());

		// WHEN
		UserNodeInstructionTaskEntity conf = orig.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setName(randomString());
		conf.setTopic(randomString());
		conf.setSchedule("123");
		conf.setServiceProps(Map.of("bar", "foo"));
		conf.setState(BasicClaimableJobState.Completed);

		// the following should NOT be updated because they are execution result properties
		conf.setExecuteAt(now().plusMillis(2474));
		conf.setLastExecuteAt(now().plusMillis(1747));
		conf.setMessage("not saved");
		conf.setResultProps(Map.of("not", "saved"));

		UserLongCompositePK result = dao.save(conf);
		UserNodeInstructionTaskEntity updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allUserNodeInstructionTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);

		// expected is updated conf props + orig execution props
		UserNodeInstructionTaskEntity expected = conf.copyWithId(conf.getId());
		expected.setLastExecuteAt(orig.getLastExecuteAt());
		expected.setMessage(orig.getMessage());
		expected.setResultProps(orig.getResultProps());

		// @formatter:off
		then(updated)
			.as("Retrieved entity matches updated source")
			.isEqualTo(expected)
			.as("Entity saved updated configuration values, preserving execution properties")
			.matches(c -> c.isSameAs(expected), "execution updated");
		// @formatter:on
	}

	@Test
	public void update_forState() {
		// GIVEN
		insert();

		// WHEN
		UserNodeInstructionTaskEntity conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setName(randomString());
		conf.setTopic(randomString());
		conf.setSchedule("123");
		conf.setServiceProps(Map.of("bar", "foo"));

		conf.setState(BasicClaimableJobState.Completed);
		conf.setExecuteAt(now().plusMillis(474));
		conf.setLastExecuteAt(now().plusMillis(747));
		conf.setMessage(randomString());
		conf.setResultProps(Map.of("pop", "bang"));

		boolean result = dao.updateTask(conf, BasicClaimableJobState.Queued);
		UserNodeInstructionTaskEntity updated = dao.get(conf.getId());

		// THEN
		List<Map<String, Object>> data = allUserNodeInstructionTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);

		// @formatter:off
		then(result)
			.as("True returned because row updated")
			.isTrue()
			;

		// expected is orig conf props + updated execution props
		UserNodeInstructionTaskEntity expected = conf.copyWithId(conf.getId());
		expected.setEnabled(last.isEnabled());
		expected.setName(last.getName());
		expected.setTopic(last.getTopic());
		expected.setSchedule(last.getSchedule());
		expected.setServiceProps(last.getServiceProps());

		then(updated)
			.as("Retrieved entity matches updated source")
			.isEqualTo(expected)
			.as("Entity saved updated execution values, preserving configuration properties")
			.matches(c -> c.isSameAs(expected))
			;
		// @formatter:on
	}

	@Test
	public void update_forState_noMatch() {
		// GIVEN
		insert();

		// WHEN
		UserNodeInstructionTaskEntity conf = last.copyWithId(last.getId());
		conf.setServiceProps(Map.of("bar", "foo"));
		conf.setState(BasicClaimableJobState.Completed);
		conf.setExecuteAt(now().plusMillis(474));
		conf.setLastExecuteAt(now().plusMillis(747));
		conf.setMessage(randomString());
		conf.setResultProps(Map.of("pop", "bang"));

		boolean result = dao.updateTask(conf, BasicClaimableJobState.Claimed);
		UserNodeInstructionTaskEntity updated = dao.get(conf.getId());

		// THEN
		List<Map<String, Object>> data = allUserNodeInstructionTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);

		// @formatter:off
		then(result)
			.as("False returned because row not updated because state did not match")
			.isFalse()
			;

		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(conf)
			.matches(c -> c.isSameAs(last), "Entity keeps original values")
			;
		// @formatter:on
	}

	@Test
	public void updateState() {
		// GIVEN
		insert();

		// WHEN
		final BasicClaimableJobState newState = BasicClaimableJobState.Claimed;
		boolean result = dao.updateTaskState(last.getId(), newState, BasicClaimableJobState.Queued);
		UserNodeInstructionTaskEntity updated = dao.get(last.getId());

		// THEN
		List<Map<String, Object>> data = allUserNodeInstructionTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);

		// @formatter:off
		then(result)
			.as("True returned because row updated")
			.isTrue()
			;

		UserNodeInstructionTaskEntity expected = last.clone();
		expected.setState(newState);

		then(updated)
			.as("Retrieved entity matches updated source")
			.isEqualTo(last)
			.as("Entity saved updated state")
			.matches(c -> c.isSameAs(expected), "state updated")
			;
		// @formatter:on
	}

	@Test
	public void updateState_noMatch() {
		// GIVEN
		insert();

		// WHEN
		final BasicClaimableJobState newState = BasicClaimableJobState.Completed;
		boolean result = dao.updateTaskState(last.getId(), newState, BasicClaimableJobState.Claimed);
		UserNodeInstructionTaskEntity updated = dao.get(last.getId());

		// THEN
		List<Map<String, Object>> data = allUserNodeInstructionTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);

		// @formatter:off
		then(result)
			.as("False returned because row not updated because state did not match")
			.isFalse()
			;

		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(last)
			.matches(c -> c.isSameAs(last), "Entity keeps original values")
			;
		// @formatter:on
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allUserNodeInstructionTaskEntityData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void delete_filter_topic() {
		final int userCount = 2;
		final int taskCount = 2;
		final List<UserNodeInstructionTaskEntity> confs = new ArrayList<>(userCount * taskCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			final Long locId = CommonDbTestUtils.insertLocation(jdbcTemplate, "GB", "UTC");
			final Long nodeId = CommonDbTestUtils.insertNode(jdbcTemplate, locId);
			CommonDbTestUtils.insertUserNode(jdbcTemplate, userId, nodeId);
			for ( int t = 0; t < taskCount; t++ ) {
				// @formatter:off
				UserNodeInstructionTaskEntity entity = newUserNodeInstructionTaskEntity(
						userId,
						randomString(),
						nodeId,
						randomString(),
						TEST_SCHEDULE,
						BasicClaimableJobState.Queued,
						now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
						null,
						null,
						null,
						null
						);
				// @formatter:on
				var pk = dao.save(entity);
				confs.add(dao.get(pk));
			}
		}

		// WHEN
		final UserNodeInstructionTaskEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		final var filter = new BasicUserNodeInstructionTaskFilter();
		filter.setUserId(randomConf.getUserId());
		filter.setTopic(randomConf.getTopic());
		int result = dao.delete(filter);

		// THEN
		UserNodeInstructionTaskEntity[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId())
						&& randomConf.getTopic().equals(e.getTopic()))
				.toArray(UserNodeInstructionTaskEntity[]::new);

		// @formatter:off
		then(result)
			.as("Count of tasks for given topic deleted")
			.isEqualTo(expected.length)
			;

		then(dao.getAll(null))
			.as("Expected tasks deleted")
			.doesNotContain(expected)
			;
		// @formatter:on
	}

	@Test
	public void delete_filter_user() {
		final int userCount = 2;
		final int taskCount = 2;
		final List<UserNodeInstructionTaskEntity> confs = new ArrayList<>(userCount * taskCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			final Long locId = CommonDbTestUtils.insertLocation(jdbcTemplate, "GB", "UTC");
			final Long nodeId = CommonDbTestUtils.insertNode(jdbcTemplate, locId);
			CommonDbTestUtils.insertUserNode(jdbcTemplate, userId, nodeId);
			for ( int t = 0; t < taskCount; t++ ) {
				// @formatter:off
				UserNodeInstructionTaskEntity entity = newUserNodeInstructionTaskEntity(
						userId,
						randomString(),
						nodeId,
						randomString(),
						TEST_SCHEDULE,
						BasicClaimableJobState.Queued,
						now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
						null,
						null,
						null,
						null
						);
				// @formatter:on
				var pk = dao.save(entity);
				confs.add(dao.get(pk));
			}
		}

		// WHEN
		final UserNodeInstructionTaskEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		final var filter = new BasicUserNodeInstructionTaskFilter();
		filter.setUserId(randomConf.getUserId());
		int result = dao.delete(filter);

		// THEN
		// @formatter:off
		then(result)
			.as("Count of tasks for given user deleted")
			.isEqualTo(taskCount)
			;
		// @formatter:on
		UserNodeInstructionTaskEntity[] expected = confs.stream()
				.filter(e -> !randomConf.getUserId().equals(e.getUserId()))
				.toArray(UserNodeInstructionTaskEntity[]::new);
		then(dao.getAll(null)).as("Expected tasks deleted").containsOnlyOnce(expected);
	}

	@Test
	public void claimTask_noRows() {
		// GIVEN

		// WHEN
		UserNodeInstructionTaskEntity result = dao.claimQueuedTask();

		// THEN
		// @formatter:off
		then(result)
			.as("Null returned when no rows exist")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void claimTask() {
		// GIVEN
		insert();

		// @formatter:off
		UserNodeInstructionTaskEntity conf2 = newUserNodeInstructionTaskEntity(
				userId,
				randomString(),
				nodeId,
				randomString(),
				TEST_SCHEDULE,
				BasicClaimableJobState.Completed,
				now().truncatedTo(ChronoUnit.SECONDS),
				null,
				null,
				null,
				null
				);
		// @formatter:on
		dao.save(conf2);

		allUserNodeInstructionTaskEntityData(jdbcTemplate);

		// WHEN
		UserNodeInstructionTaskEntity result = dao.claimQueuedTask();

		//TestTransaction.flagForCommit();

		// THEN
		UserNodeInstructionTaskEntity expected = last.clone();
		expected.setState(BasicClaimableJobState.Claimed);

		// @formatter:off
		then(result)
			.as("Retrieved entity matches with with Queued state")
			.isEqualTo(expected)
			.matches(c -> c.isSameAs(expected), "Claimed entity has Claimed state")
			;
		// @formatter:on
	}

	@Test
	public void claimTask_execAtInPast() {
		// GIVEN
		// @formatter:off
		// 1st task executeAt in future: cannot be claimed
		UserNodeInstructionTaskEntity conf1 = newUserNodeInstructionTaskEntity(
				userId,
				randomString(),
				nodeId,
				randomString(),
				TEST_SCHEDULE,
				BasicClaimableJobState.Queued,
				now().truncatedTo(ChronoUnit.SECONDS).plus(1L, ChronoUnit.DAYS),
				null,
				null,
				null,
				null
				);

		// 2nd task executeAt in past: can be claimed
		UserNodeInstructionTaskEntity conf2 = newUserNodeInstructionTaskEntity(
				userId,
				randomString(),
				nodeId,
				randomString(),
				TEST_SCHEDULE,
				BasicClaimableJobState.Queued,
				now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
				null,
				null,
				null,
				null
				);
		// @formatter:on

		conf1 = dao.get(dao.save(conf1));
		conf2 = dao.get(dao.save(conf2));

		allUserNodeInstructionTaskEntityData(jdbcTemplate);

		// WHEN
		UserNodeInstructionTaskEntity result = dao.claimQueuedTask();

		// THEN
		UserNodeInstructionTaskEntity expected = conf2.clone();
		expected.setState(BasicClaimableJobState.Claimed);

		// @formatter:off
		then(result)
			.as("Retrieved entity matches row with executeAt in the past")
			.isEqualTo(expected)
			.matches(c -> c.isSameAs(expected), "Claimed entity has Claimed state")
			;
		// @formatter:on
	}

	@Test
	public void resetAbandoned() {
		// GIVEN
		final int taskCount = 10;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);

		final List<UserNodeInstructionTaskEntity> rakeTasks = new ArrayList<>(taskCount);

		for ( int t = 0; t < taskCount; t++ ) {
			// @formatter:off
			UserNodeInstructionTaskEntity conf = newUserNodeInstructionTaskEntity(
					userId,
					randomString(),
					nodeId,
					randomString(),
					TEST_SCHEDULE,
					t == 0 || RNG.nextBoolean() ? BasicClaimableJobState.Executing
							: RNG.nextBoolean() ? BasicClaimableJobState.Claimed
									: BasicClaimableJobState.Queued,
					start.plusSeconds(60 * t),
					null,
					null,
					null,
					null
					);
			// @formatter:on
			rakeTasks.add(dao.get(dao.save(conf)));
		}

		final List<UserNodeInstructionTaskEntity> tasksToReset = rakeTasks.stream()
				.filter(e -> e.getState() == BasicClaimableJobState.Executing
						|| e.getState() == BasicClaimableJobState.Claimed)
				.toList();
		final UserNodeInstructionTaskEntity randomExecutingTask = tasksToReset
				.get(RNG.nextInt(tasksToReset.size()));

		allUserNodeInstructionTaskEntityData(jdbcTemplate);

		// WHEN
		final Instant minimumDate = randomExecutingTask.getExecuteAt().plusSeconds(1);
		int count = dao.resetAbandondedExecutingTasks(minimumDate);

		// THEN
		final int expectedCount = (int) tasksToReset.stream()
				.filter(e -> e.getExecuteAt().isBefore(minimumDate)).count();
		// @formatter:off
		then(count)
			.as("Should reset all tasks in executing state older than given minimum date")
			.isEqualTo(expectedCount)
			;
		// @formatter:on
	}

	@Test
	public void updateEnabledStatus_forUser() {
		// GIVEN
		final int userCount = 2;
		final int taskCount = 2;
		final List<UserNodeInstructionTaskEntity> confs = new ArrayList<>(userCount * taskCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			final Long locId = CommonDbTestUtils.insertLocation(jdbcTemplate, "GB", "UTC");
			final Long nodeId = CommonDbTestUtils.insertNode(jdbcTemplate, locId);
			CommonDbTestUtils.insertUserNode(jdbcTemplate, userId, nodeId);
			for ( int t = 0; t < taskCount; t++ ) {
				// @formatter:off
				UserNodeInstructionTaskEntity entity = newUserNodeInstructionTaskEntity(
						userId,
						randomString(),
						nodeId,
						randomString(),
						TEST_SCHEDULE,
						BasicClaimableJobState.Queued,
						now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
						null,
						null,
						null,
						null
						);
				// @formatter:on
				var pk = dao.save(entity);
				confs.add(dao.get(pk));
			}
		}

		// WHEN
		final UserNodeInstructionTaskEntity randomConf = confs.get(RNG.nextInt(confs.size()));

		int result = dao.updateEnabledStatus(randomConf.getUserId(), null, false);

		// THEN
		// @formatter:off
		UserNodeInstructionTaskEntity[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId()))
				.toArray(UserNodeInstructionTaskEntity[]::new);
		then(result).as("Updated state rows").isEqualTo(expected.length);

		List<Boolean> data = allUserNodeInstructionTaskEntityData(jdbcTemplate)
			.stream()
			.filter(m -> userId.equals(m.get("user_id")))
			.map(m -> (Boolean)m.get("enabled"))
			.toList()
			;
		then(data)
			.as("All tasks for user are disabled")
			.allMatch(b -> b == false)
			;
		// @formatter:on
	}

	@Test
	public void updateEnabledStatus_forEntity() {
		// GIVEN
		final int userCount = 2;
		final int taskCount = 2;
		final List<UserNodeInstructionTaskEntity> confs = new ArrayList<>(userCount * taskCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			final Long locId = CommonDbTestUtils.insertLocation(jdbcTemplate, "GB", "UTC");
			final Long nodeId = CommonDbTestUtils.insertNode(jdbcTemplate, locId);
			CommonDbTestUtils.insertUserNode(jdbcTemplate, userId, nodeId);
			for ( int t = 0; t < taskCount; t++ ) {
				// @formatter:off
				UserNodeInstructionTaskEntity entity = newUserNodeInstructionTaskEntity(
						userId,
						randomString(),
						nodeId,
						randomString(),
						TEST_SCHEDULE,
						BasicClaimableJobState.Queued,
						now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
						null,
						null,
						null,
						null
						);
				// @formatter:on
				var pk = dao.save(entity);
				confs.add(dao.get(pk));
			}
		}

		// WHEN
		final UserNodeInstructionTaskEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		final var filter = new BasicUserNodeInstructionTaskFilter();
		filter.setUserId(randomConf.getUserId());
		filter.setTaskId(randomConf.getConfigId());

		int result = dao.updateEnabledStatus(randomConf.getUserId(), filter, false);

		// THEN
		// @formatter:off
		then(result).as("Updated state row").isEqualTo(1);
		
		List<Boolean> data = allUserNodeInstructionTaskEntityData(jdbcTemplate)
			.stream()
			.filter(m -> userId.equals(m.get("user_id")) && randomConf.getConfigId().equals(m.get("id")))
			.map(m -> (Boolean)m.get("enabled"))
			.toList()
			;
		then(data)
			.as("All tasks for user are disabled")
			.allMatch(b -> b == false)
			;
		// @formatter:on
	}

}
