/* ==================================================================
 * JdbcUserFluxAggregatePublishConfigurationDaoTests.java - 24/06/2024 9:53:12 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.flux.dao.jdbc.test;

import static net.solarnetwork.central.domain.UserLongCompositePK.UNASSIGNED_ENTITY_ID;
import static net.solarnetwork.central.domain.UserLongCompositePK.UNASSIGNED_USER_ID;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.array;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettings;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.user.flux.dao.BasicFluxConfigurationFilter;
import net.solarnetwork.central.user.flux.dao.jdbc.JdbcUserFluxAggregatePublishConfigurationDao;
import net.solarnetwork.central.user.flux.dao.jdbc.JdbcUserFluxDefaultAggregatePublishConfigurationDao;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfiguration;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfiguration;
import net.solarnetwork.dao.Entity;

/**
 * Test cases for the {@link JdbcUserFluxAggregatePublishConfigurationDao}
 * class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserFluxAggregatePublishConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcUserFluxDefaultAggregatePublishConfigurationDao defaultDao;
	private JdbcUserFluxAggregatePublishConfigurationDao dao;
	private Long userId;

	private UserFluxAggregatePublishConfiguration last;

	@BeforeEach
	public void setup() {
		defaultDao = new JdbcUserFluxDefaultAggregatePublishConfigurationDao(jdbcTemplate);
		dao = new JdbcUserFluxAggregatePublishConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void entityKey() {
		UserLongCompositePK id = new UserLongCompositePK(randomLong(), randomLong());
		UserFluxAggregatePublishConfiguration result = dao.entityKey(id);

		// @formatter:off
		then(result)
			.as("Entity for key returned")
			.isNotNull()
			.as("ID of entity from provided value")
			.returns(id, Entity::getId)
			;
		// @formatter:on
	}

	private List<Map<String, Object>> allUserFluxAggregatePublishConfigurationTable() {
		return CommonDbTestUtils.allTableData(log, jdbcTemplate, "solaruser.user_flux_agg_pub_settings",
				"user_id, id");
	}

	private static UserFluxAggregatePublishConfiguration newUserFluxAggregatePublishConfiguration(
			Long userId, Long[] nodeIds, String[] sourceIds, boolean publish, boolean retain) {
		UserFluxAggregatePublishConfiguration conf = new UserFluxAggregatePublishConfiguration(
				unassignedEntityIdKey(userId), Instant.now());
		conf.setModified(conf.getCreated());
		conf.setNodeIds(nodeIds);
		conf.setSourceIds(sourceIds);
		conf.setPublish(publish);
		conf.setRetain(retain);
		return conf;
	}

	private Function<Array, Object> sqlArrayValue() {
		return (t) -> {
			try {
				return t.getArray();
			} catch ( SQLException e ) {
				return null;
			}
		};
	}

	@Test
	public void insert() {
		// GIVEN
		UserFluxAggregatePublishConfiguration conf = newUserFluxAggregatePublishConfiguration(userId,
				new Long[] { randomLong(), randomLong() },
				new String[] { randomString(), randomString() }, true, true);

		// WHEN
		UserLongCompositePK result = dao.create(userId, conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserLongCompositePK::getUserId)
			.as("ID generated")
			.doesNotReturn(null, UserLongCompositePK::getEntityId)
			;

		List<Map<String, Object>> data = allUserFluxAggregatePublishConfigurationTable();
		then(data).as("Table has 1 row").hasSize(1).asInstanceOf(list(Map.class))
			.element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row ID generated")
			.containsEntry("id", result.getEntityId())
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("No row enabled column")
			.doesNotContainKey("enabled")
			.as("Row node IDs")
			.hasEntrySatisfying("node_ids", (val) -> {
				then(val).as("Array column")
					.asInstanceOf(type(Array.class))
					.extracting(sqlArrayValue())
					.as("node_ids returned as BIGINT[] array")
					.asInstanceOf(array(Long[].class))
					.as("Row node_ids contains input values")
					.containsExactly(conf.getNodeIds())
					;
			})
			.as("Row source IDs")
			.hasEntrySatisfying("source_ids", (val) -> {
				then(val).as("Array column")
					.asInstanceOf(type(Array.class))
					.extracting(sqlArrayValue())
					.as("source_ids returned as TEXT[] array")
					.asInstanceOf(array(String[].class))
					.as("Row sources_ids contains input values")
					.containsExactly(conf.getSourceIds())
					;
			})
			.as("Publish flag")
			.containsEntry("publish", true)
			.as("Retain flag")
			.containsEntry("retain", true)
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		allUserFluxAggregatePublishConfigurationTable();

		// WHEN
		UserFluxAggregatePublishConfiguration result = dao.get(last.getId());

		// THEN
		// @formatter:off
		then(result)
			.as("Retrieved entity matches source")
			.isEqualTo(last)
			.as("Entity values retrieved")
			.matches(c -> c.isSameAs(last))
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
		var data = allUserFluxAggregatePublishConfigurationTable();
		then(data).as("Row deleted").isEmpty();
	}

	@Test
	public void delete_forUser() {
		// GIVEN
		insert();

		UserFluxAggregatePublishConfiguration conf2 = newUserFluxAggregatePublishConfiguration(userId,
				new Long[] { randomLong() }, new String[] { randomString() }, true, true);
		dao.create(userId, conf2);

		Long userId2 = CommonDbTestUtils.insertUser(jdbcTemplate);

		UserFluxAggregatePublishConfiguration conf3 = newUserFluxAggregatePublishConfiguration(userId2,
				new Long[] { randomLong() }, new String[] { randomString() }, true, true);

		UserLongCompositePK id3 = dao.create(userId2, conf3);

		var data = allUserFluxAggregatePublishConfigurationTable();

		// WHEN
		dao.delete(new UserFluxAggregatePublishConfiguration(
				UserLongCompositePK.unassignedEntityIdKey(userId), Instant.now()));

		// THEN
		then(data).as("Initial data before delete has 3 rows").hasSize(3);

		data = allUserFluxAggregatePublishConfigurationTable();
		// @formatter:off
		then(data).as("User 1 rows deleted, leaving just 1 row for user 2").hasSize(1)
				.asInstanceOf(list(Map.class)).element(0, map(String.class, Object.class))
				.as("Row user ID")
				.containsEntry("user_id", userId2)
				.as("Row ID generated")
				.containsEntry("id", id3.getEntityId())
				;
		// @formatter:on
	}

	@Test
	public void settingsForUserNodeSource_matchOne() throws Exception {
		// GIVEN
		final Long nodeId = randomLong();
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);

		UserFluxAggregatePublishConfiguration conf = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf.setNodeIds(new Long[] { nodeId });
		conf.setSourceIds(new String[] { randomString(), randomString(), randomString() });
		conf.setPublish(true);
		conf.setRetain(true);
		dao.create(userId, conf);

		allUserFluxAggregatePublishConfigurationTable();

		// WHEN
		FluxPublishSettings result = dao.nodeSourcePublishConfiguration(userId, nodeId,
				conf.getSourceIds()[0]);

		// THEN
		// @formatter:off
		then(result)
			.as("Result returned")
			.isNotNull()
			.as("Is publish")
			.returns(true, from(FluxPublishSettings::isPublish))
			.as("Is retain")
			.returns(true, from(FluxPublishSettings::isRetain))
			;
		// @formatter:on
	}

	@Test
	public void settingsForUserNodeSource_wildcardNode_matchOne() throws Exception {
		// GIVEN
		setupTestLocation();

		final Long nodeId1 = randomLong();
		setupTestNode(nodeId1);
		setupTestUserNode(userId, nodeId1);

		final Long nodeId2 = randomLong();
		setupTestNode(nodeId2);
		setupTestUserNode(userId, nodeId2);

		final Long userId2 = CommonDbTestUtils.insertUser(jdbcTemplate);

		final Long nodeId3 = randomLong();
		setupTestNode(nodeId3);
		setupTestUserNode(userId2, nodeId3);

		UserFluxAggregatePublishConfiguration conf = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf.setSourceIds(new String[] { randomString(), randomString(), randomString() });
		conf.setPublish(true);
		conf.setRetain(true);
		dao.create(userId, conf);

		allUserFluxAggregatePublishConfigurationTable();

		// WHEN
		FluxPublishSettings result1 = dao.nodeSourcePublishConfiguration(userId, nodeId1,
				conf.getSourceIds()[0]);
		FluxPublishSettings result2 = dao.nodeSourcePublishConfiguration(userId, nodeId2,
				conf.getSourceIds()[0]);

		// test other user (negative match)
		FluxPublishSettings result3 = dao.nodeSourcePublishConfiguration(userId2, nodeId3,
				conf.getSourceIds()[0]);

		// THEN
		// @formatter:off
		then(result1)
			.as("Result 1 returned")
			.isNotNull()
			.as("Is publish because match wildcard node")
			.returns(true, from(FluxPublishSettings::isPublish))
			.as("Is retain because match wildcard node")
			.returns(true, from(FluxPublishSettings::isRetain))
			;
		then(result2)
			.as("Result 2 returned")
			.isNotNull()
			.as("Is publish because match wildcard node")
			.returns(true, from(FluxPublishSettings::isPublish))
			.as("Is retain because match wildcard node")
			.returns(true, from(FluxPublishSettings::isRetain))
			;
		then(result3)
			.as("Result 3 returned")
			.isNotNull()
			.as("Is not publish")
			.returns(false, from(FluxPublishSettings::isPublish))
			.as("Is not retain")
			.returns(false, from(FluxPublishSettings::isRetain))
			;
		// @formatter:on
	}

	@Test
	public void settingsForUserNodeSource_wildcardSource_matchOne() throws Exception {
		// GIVEN
		setupTestLocation();

		final Long nodeId1 = randomLong();
		setupTestNode(nodeId1);
		setupTestUserNode(userId, nodeId1);

		final Long nodeId2 = randomLong();
		setupTestNode(nodeId2);
		setupTestUserNode(userId, nodeId2);

		final Long userId2 = CommonDbTestUtils.insertUser(jdbcTemplate);

		final Long nodeId3 = randomLong();
		setupTestNode(nodeId3);
		setupTestUserNode(userId2, nodeId3);

		UserFluxAggregatePublishConfiguration conf = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf.setNodeIds(new Long[] { nodeId1, nodeId2 });
		conf.setPublish(true);
		conf.setRetain(true);
		dao.create(userId, conf);

		allUserFluxAggregatePublishConfigurationTable();

		// WHEN
		FluxPublishSettings result1 = dao.nodeSourcePublishConfiguration(userId, nodeId1,
				randomString());
		FluxPublishSettings result2 = dao.nodeSourcePublishConfiguration(userId, nodeId2,
				randomString());

		// test other user (negative match)
		FluxPublishSettings result3 = dao.nodeSourcePublishConfiguration(userId2, nodeId3,
				randomString());

		// THEN
		// @formatter:off
		then(result1)
			.as("Result 1 returned")
			.isNotNull()
			.as("Is publish because match wildcard source")
			.returns(true, from(FluxPublishSettings::isPublish))
			.as("Is retain because match wildcard source")
			.returns(true, from(FluxPublishSettings::isRetain))
			;
		then(result2)
			.as("Result 2 returned")
			.isNotNull()
			.as("Is publish")
			.returns(true, from(FluxPublishSettings::isPublish))
			.as("Is retain")
			.returns(true, from(FluxPublishSettings::isRetain))
			;
		then(result3)
			.as("Result 3 returned")
			.isNotNull()
			.as("Is not publish")
			.returns(false, from(FluxPublishSettings::isPublish))
			.as("Is not retain")
			.returns(false, from(FluxPublishSettings::isRetain))
			;
		// @formatter:on
	}

	@Test
	public void settingsForUserNodeSource_matchMulti() throws Exception {
		// GIVEN
		final Long nodeId = randomLong();
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);

		UserFluxAggregatePublishConfiguration conf1 = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf1.setNodeIds(new Long[] { nodeId });
		conf1.setSourceIds(new String[] { randomString(), randomString(), randomString() });
		conf1.setPublish(true);
		conf1.setRetain(true);
		dao.create(userId, conf1);

		// override source ID 2 to not retain
		UserFluxAggregatePublishConfiguration conf2 = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf2.setNodeIds(new Long[] { nodeId, randomLong() });
		conf2.setSourceIds(new String[] { conf1.getSourceIds()[1] });
		conf2.setPublish(true);
		conf2.setRetain(false);
		dao.create(userId, conf2);

		// override source ID 3 to not publish
		UserFluxAggregatePublishConfiguration conf3 = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf3.setNodeIds(new Long[] { nodeId, randomLong() });
		conf3.setSourceIds(new String[] { conf1.getSourceIds()[2] });
		conf3.setPublish(false);
		conf3.setRetain(false);
		dao.create(userId, conf3);

		allUserFluxAggregatePublishConfigurationTable();

		// WHEN
		FluxPublishSettings result1 = dao.nodeSourcePublishConfiguration(userId, nodeId,
				conf1.getSourceIds()[0]);
		FluxPublishSettings result2 = dao.nodeSourcePublishConfiguration(userId, nodeId,
				conf1.getSourceIds()[1]);
		FluxPublishSettings result3 = dao.nodeSourcePublishConfiguration(userId, nodeId,
				conf1.getSourceIds()[2]);

		// also verify node not owned by user is "denied"
		FluxPublishSettings result4 = dao.nodeSourcePublishConfiguration(userId, conf2.getNodeIds()[1],
				conf2.getSourceIds()[0]);

		// THEN
		// @formatter:off
		then(result1)
			.as("Result 1 returned")
			.isNotNull()
			.as("Is publish")
			.returns(true, from(FluxPublishSettings::isPublish))
			.as("Is retain")
			.returns(true, from(FluxPublishSettings::isRetain))
			;
		then(result2)
			.as("Result 2 returned")
			.isNotNull()
			.as("Is publish")
			.returns(true, from(FluxPublishSettings::isPublish))
			.as("Is not retain")
			.returns(false, from(FluxPublishSettings::isRetain))
			;
		then(result3)
			.as("Result 3 returned")
			.isNotNull()
			.as("Is not publish")
			.returns(false, from(FluxPublishSettings::isPublish))
			.as("Is not retain")
			.returns(false, from(FluxPublishSettings::isRetain))
			;
		then(result4)
			.as("Result 4 returned")
			.isNotNull()
			.as("Is not publish")
			.returns(false, from(FluxPublishSettings::isPublish))
			.as("Is not retain")
			.returns(false, from(FluxPublishSettings::isRetain))
			;
		// @formatter:on
	}

	@Test
	public void settingsForUserNodeSource_matchMulti_wildcard() throws Exception {
		// GIVEN
		setupTestLocation();
		final Long nodeId1 = randomLong();
		setupTestNode(nodeId1);
		setupTestUserNode(userId, nodeId1);

		final Long nodeId2 = randomLong();
		setupTestNode(nodeId2);
		setupTestUserNode(userId, nodeId2);

		UserFluxAggregatePublishConfiguration conf1 = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf1.setNodeIds(new Long[] { nodeId1 });
		conf1.setSourceIds(new String[] { randomString(), randomString(), randomString() });
		conf1.setPublish(true);
		conf1.setRetain(true);
		dao.create(userId, conf1);

		// override source ID 2 to not retain
		UserFluxAggregatePublishConfiguration conf2 = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf2.setNodeIds(new Long[] { nodeId1, randomLong() });
		conf2.setSourceIds(new String[] { conf1.getSourceIds()[1] });
		conf2.setPublish(true);
		conf2.setRetain(false);
		dao.create(userId, conf2);

		// override source ID 3 to not publish
		UserFluxAggregatePublishConfiguration conf3 = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf3.setNodeIds(new Long[] { nodeId1, randomLong() });
		conf3.setSourceIds(new String[] { conf1.getSourceIds()[2] });
		conf3.setPublish(false);
		conf3.setRetain(false);
		dao.create(userId, conf3);

		// add wildcard to do nothing... should not override any more specific conf above
		UserFluxAggregatePublishConfiguration conf4 = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf4.setPublish(false);
		conf4.setRetain(true);
		dao.create(userId, conf4);

		allUserFluxAggregatePublishConfigurationTable();

		// WHEN
		FluxPublishSettings result1 = dao.nodeSourcePublishConfiguration(userId, nodeId1,
				conf1.getSourceIds()[0]);
		FluxPublishSettings result2 = dao.nodeSourcePublishConfiguration(userId, nodeId1,
				conf1.getSourceIds()[1]);
		FluxPublishSettings result3 = dao.nodeSourcePublishConfiguration(userId, nodeId1,
				conf1.getSourceIds()[2]);

		// wildcard match
		FluxPublishSettings result4 = dao.nodeSourcePublishConfiguration(userId, nodeId2,
				randomString());

		// also verify node not owned by user is "denied"
		FluxPublishSettings result5 = dao.nodeSourcePublishConfiguration(userId, conf2.getNodeIds()[1],
				conf2.getSourceIds()[0]);

		// THEN
		// @formatter:off
		then(result1)
			.as("Result 1 returned")
			.isNotNull()
			.as("Is publish")
			.returns(true, from(FluxPublishSettings::isPublish))
			.as("Is retain")
			.returns(true, from(FluxPublishSettings::isRetain))
			;
		then(result2)
			.as("Result 2 returned")
			.isNotNull()
			.as("Is publish")
			.returns(true, from(FluxPublishSettings::isPublish))
			.as("Is not retain")
			.returns(false, from(FluxPublishSettings::isRetain))
			;
		then(result3)
			.as("Result 3 returned")
			.isNotNull()
			.as("Is not publish")
			.returns(false, from(FluxPublishSettings::isPublish))
			.as("Is not retain")
			.returns(false, from(FluxPublishSettings::isRetain))
			;
		then(result4)
			.as("Result 3 returned")
			.isNotNull()
			.as("Is not publish because of wildcard match")
			.returns(false, from(FluxPublishSettings::isPublish))
			.as("Is retain because of wildcard match")
			.returns(true, from(FluxPublishSettings::isRetain))
			;
		then(result5)
			.as("Result 4 returned")
			.isNotNull()
			.as("Is not publish because other user")
			.returns(false, from(FluxPublishSettings::isPublish))
			.as("Is not retain because other user")
			.returns(false, from(FluxPublishSettings::isRetain))
			;
		// @formatter:on
	}

	@Test
	public void settingsForUserNodeSource_matchOne_withDefaultsPublish() throws Exception {
		// GIVEN
		final Long nodeId = randomLong();
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);

		// insert default, with true settings
		var defaults = new UserFluxDefaultAggregatePublishConfiguration(userId, Instant.now());
		defaults.setPublish(true);
		defaults.setRetain(false);
		defaultDao.save(defaults);

		// WHEN
		final String sourceId = randomString();
		FluxPublishSettings result = dao.nodeSourcePublishConfiguration(userId, nodeId, sourceId);

		// THEN
		// @formatter:off
		then(result)
			.as("Result returned")
			.isNotNull()
			.as("Is publish")
			.returns(true, from(FluxPublishSettings::isPublish))
			.as("Is retain")
			.returns(false, from(FluxPublishSettings::isRetain))
			;
		// @formatter:on
	}

	@Test
	public void settingsForUserNodeSource_matchOne_withDefaultsPublishAndRetain() throws Exception {
		// GIVEN
		final Long nodeId = randomLong();
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);

		// insert default, with true settings
		var defaults = new UserFluxDefaultAggregatePublishConfiguration(userId, Instant.now());
		defaults.setPublish(true);
		defaults.setRetain(true);
		defaultDao.save(defaults);

		// WHEN
		final String sourceId = randomString();
		FluxPublishSettings result = dao.nodeSourcePublishConfiguration(userId, nodeId, sourceId);

		// THEN
		// @formatter:off
		then(result)
			.as("Result returned")
			.isNotNull()
			.as("Is publish")
			.returns(true, from(FluxPublishSettings::isPublish))
			.as("Is retain")
			.returns(true, from(FluxPublishSettings::isRetain))
			;
		// @formatter:on
	}

	@Test
	public void settingsForUserNodeSource_matchOne_withDefaultsRetain() throws Exception {
		// GIVEN
		final Long nodeId = randomLong();
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);

		// insert default, with true settings
		var defaults = new UserFluxDefaultAggregatePublishConfiguration(userId, Instant.now());
		defaults.setPublish(false);
		defaults.setRetain(true);
		defaultDao.save(defaults);

		// WHEN
		final String sourceId = randomString();
		FluxPublishSettings result = dao.nodeSourcePublishConfiguration(userId, nodeId, sourceId);

		// THEN
		// @formatter:off
		then(result)
			.as("Result returned")
			.isNotNull()
			.as("Is publish")
			.returns(false, from(FluxPublishSettings::isPublish))
			.as("Is retain")
			.returns(true, from(FluxPublishSettings::isRetain))
			;
		// @formatter:on
	}

	@Test
	public void settingsForUserNodeSource_matchOne_withDefaultsOff() throws Exception {
		// GIVEN
		final Long nodeId = randomLong();
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);

		// insert default, with true settings
		var defaults = new UserFluxDefaultAggregatePublishConfiguration(userId, Instant.now());
		defaults.setPublish(false);
		defaults.setRetain(false);
		defaultDao.save(defaults);

		// WHEN
		final String sourceId = randomString();
		FluxPublishSettings result = dao.nodeSourcePublishConfiguration(userId, nodeId, sourceId);

		// THEN
		// @formatter:off
		then(result)
			.as("Result returned")
			.isNotNull()
			.as("Is publish")
			.returns(false, from(FluxPublishSettings::isPublish))
			.as("Is retain")
			.returns(false, from(FluxPublishSettings::isRetain))
			;
		// @formatter:on
	}

	@Test
	public void findFiltered() {
		// GIVEN
		final Long nodeId = randomLong();
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);

		UserFluxAggregatePublishConfiguration conf1 = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf1.setNodeIds(new Long[] { randomLong() });
		conf1.setSourceIds(new String[] { randomString() });
		conf1.setPublish(true);
		conf1.setRetain(true);
		var id1 = dao.create(userId, conf1);

		UserFluxAggregatePublishConfiguration conf2 = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf2.setNodeIds(new Long[] { randomLong() });
		conf2.setSourceIds(new String[] { randomString() });
		conf2.setPublish(true);
		conf2.setRetain(false);
		var id2 = dao.create(userId, conf2);

		final Long userId2 = CommonDbTestUtils.insertUser(jdbcTemplate);

		// override source ID 3 to not publish
		UserFluxAggregatePublishConfiguration conf3 = new UserFluxAggregatePublishConfiguration(
				UNASSIGNED_USER_ID, UNASSIGNED_ENTITY_ID, Instant.now());
		conf3.setNodeIds(new Long[] { randomLong() });
		conf3.setSourceIds(new String[] { randomString() });
		conf3.setPublish(false);
		conf3.setRetain(false);
		dao.create(userId2, conf3);

		final var data = allUserFluxAggregatePublishConfigurationTable();

		// WHEN
		var filter = new BasicFluxConfigurationFilter();
		filter.setUserId(userId);
		var result = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		then(data).as("Table has 3 rows").hasSize(3);

		then(result)
			.as("Result returned")
			.isNotNull()
			.as("Only user 1 results returned, in ID order")
			.containsExactly(conf1.copyWithId(id1), conf2.copyWithId(id2))
			;
		// @formatter:on
	}

}
