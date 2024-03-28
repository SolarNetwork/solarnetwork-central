/* ==================================================================
 * JdbcEndpointConfigurationDaoTests.java - 21/02/2024 3:38:11 pm
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

package net.solarnetwork.central.inin.dao.jdbc.test;

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.inin.dao.jdbc.test.InstructionInputJdbcTestUtils.allEndpointConfigurationData;
import static net.solarnetwork.central.inin.dao.jdbc.test.InstructionInputJdbcTestUtils.newEndpointConfiguration;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.dao.BasicFilter;
import net.solarnetwork.central.inin.dao.jdbc.JdbcEndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.jdbc.JdbcTransformConfigurationDao;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformPhase;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link JdbcEndpointConfigurationDao} class.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcEndpointConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcEndpointConfigurationDao dao;
	private Long userId;
	private JdbcTransformConfigurationDao reqTransformDao;
	private JdbcTransformConfigurationDao resTransformDao;

	private EndpointConfiguration last;

	@BeforeEach
	public void setup() {
		dao = new JdbcEndpointConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		reqTransformDao = new JdbcTransformConfigurationDao(jdbcTemplate, TransformPhase.Request);
		resTransformDao = new JdbcTransformConfigurationDao(jdbcTemplate, TransformPhase.Response);
	}

	@Test
	public void entityKey() {
		UserUuidPK id = new UserUuidPK(randomLong(), randomUUID());
		EndpointConfiguration result = dao.entityKey(id);

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
		EndpointConfiguration conf = newEndpointConfiguration(userId, randomUUID(), randomString(),
				new Long[] { randomLong() }, null, null);

		// WHEN
		UserUuidPK result = dao.create(userId, conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserUuidPK::getUserId)
			.as("ID generated")
			.doesNotReturn(null, UserUuidPK::getUuid)
			;

		List<Map<String, Object>> data = allEndpointConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row ID generated")
			.containsKey("id")
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			.as("Row name")
			.containsEntry("cname", conf.getName())
			.as("Row node ID")
			.hasEntrySatisfying("node_ids", o -> {
				Long[] ids = CommonJdbcUtils.arrayValue(o);
				then(ids)
					.as("Row node_ids")
					.contains(conf.getNodeIds().toArray(Long[]::new))
					;
			})
			.as("No request transform ID")
			.containsEntry("req_xform_id", null)
			.as("No response transform ID")
			.containsEntry("res_xform_id", null)
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void insert_generateUuid() {
		// GIVEN
		EndpointConfiguration conf = newEndpointConfiguration(userId, UserUuidPK.UNASSIGNED_UUID_ID,
				randomString(), new Long[] { randomLong() }, null, null);

		// WHEN
		UserUuidPK result = dao.create(userId, conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserUuidPK::getUserId)
			.as("ID generated")
			.doesNotReturn(null, UserUuidPK::getUuid)
			.as("UUID not unassigned value")
			.doesNotReturn(UserUuidPK.UNASSIGNED_UUID_ID, UserUuidPK::getUuid)
			;

		List<Map<String, Object>> data = allEndpointConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row ID generated")
			.containsKey("id")
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			.as("Row name")
			.containsEntry("cname", conf.getName())
			.as("Row node ID")
			.hasEntrySatisfying("node_ids", o -> {
				Long[] ids = CommonJdbcUtils.arrayValue(o);
				then(ids)
					.as("Row node_ids")
					.contains(conf.getNodeIds().toArray(Long[]::new))
					;
			})
			.as("No request transform ID")
			.containsEntry("req_xform_id", null)
			.as("No response transform ID")
			.containsEntry("res_xform_id", null)
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void insert_withTransforms() {
		// GIVEN
		TransformConfiguration reqXform = InstructionInputJdbcTestUtils
				.newRequestTransformConfiguration(userId, randomString(), randomString(), null);
		reqXform = reqTransformDao.get(reqTransformDao.create(userId, reqXform));

		TransformConfiguration resXform = InstructionInputJdbcTestUtils
				.newResponseTransformConfiguration(userId, randomString(), randomString(), null);
		resXform = resTransformDao.get(resTransformDao.create(userId, resXform));

		EndpointConfiguration conf = newEndpointConfiguration(userId, randomUUID(), randomString(),
				new Long[] { randomLong() }, reqXform.getTransformId(), resXform.getTransformId());

		// WHEN
		UserUuidPK result = dao.create(userId, conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserUuidPK::getUserId)
			.as("ID generated")
			.doesNotReturn(null, UserUuidPK::getUuid)
			;

		List<Map<String, Object>> data = allEndpointConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row ID generated")
			.containsKey("id")
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			.as("Row name")
			.containsEntry("cname", conf.getName())
			.as("Row node ID")
			.hasEntrySatisfying("node_ids", o -> {
				Long[] ids = CommonJdbcUtils.arrayValue(o);
				then(ids)
					.as("Row node_ids")
					.contains(conf.getNodeIds().toArray(Long[]::new))
					;
			})
			.as("Row request transform ID")
			.containsEntry("req_xform_id", conf.getRequestTransformId())
			.as("Row response transform ID")
			.containsEntry("res_xform_id", conf.getResponseTransformId())
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void save_insert() {
		// GIVEN
		EndpointConfiguration conf = newEndpointConfiguration(userId, randomUUID(), randomString(),
				new Long[] { randomLong() }, null, null);

		// WHEN
		UserUuidPK result = dao.save(conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserUuidPK::getUserId)
			.as("ID generated")
			.doesNotReturn(null, UserUuidPK::getUuid)
			;

		List<Map<String, Object>> data = allEndpointConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row ID generated")
			.containsKey("id")
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			.as("Row name")
			.containsEntry("cname", conf.getName())
			.as("Row node ID")
			.hasEntrySatisfying("node_ids", o -> {
				Long[] ids = CommonJdbcUtils.arrayValue(o);
				then(ids)
					.as("Row node_ids")
					.contains(conf.getNodeIds().toArray(Long[]::new))
					;
			})
			.as("No request transform ID")
			.containsEntry("req_xform_id", null)
			.as("No response transform ID")
			.containsEntry("res_xform_id", null)
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void save_generateUuid() {
		// GIVEN
		EndpointConfiguration conf = newEndpointConfiguration(userId, UserUuidPK.UNASSIGNED_UUID_ID,
				randomString(), new Long[] { randomLong() }, null, null);

		// WHEN
		UserUuidPK result = dao.save(conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserUuidPK::getUserId)
			.as("ID generated")
			.doesNotReturn(null, UserUuidPK::getUuid)
			.as("UUID not unassigned value")
			.doesNotReturn(UserUuidPK.UNASSIGNED_UUID_ID, UserUuidPK::getUuid)
			;

		List<Map<String, Object>> data = allEndpointConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row ID generated")
			.containsKey("id")
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			.as("Row name")
			.containsEntry("cname", conf.getName())
			.as("Row node ID")
			.hasEntrySatisfying("node_ids", o -> {
				Long[] ids = CommonJdbcUtils.arrayValue(o);
				then(ids)
					.as("Row node_ids")
					.contains(conf.getNodeIds().toArray(Long[]::new))
					;
			})
			.as("No request transform ID")
			.containsEntry("req_xform_id", null)
			.as("No response transform ID")
			.containsEntry("res_xform_id", null)
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		EndpointConfiguration result = dao.get(last.getId());

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
	public void get_withTransforms() {
		// GIVEN
		insert_withTransforms();

		// WHEN
		EndpointConfiguration result = dao.get(last.getId());

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
	public void update() {
		// GIVEN
		insert();

		// WHEN
		EndpointConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setName(randomString());
		conf.setNodeIds(Collections.singleton(randomLong()));

		UserUuidPK result = dao.save(conf);
		EndpointConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allEndpointConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);
		// @formatter:off
		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(conf)
			.as("Entity saved updated values")
			.matches(c -> c.isSameAs(updated));
		// @formatter:on
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allEndpointConfigurationData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<EndpointConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				EndpointConfiguration conf = newEndpointConfiguration(userId, randomUUID(),
						randomString(), new Long[] { randomLong() }, null, null);
				UserUuidPK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<EndpointConfiguration> results = dao.findAll(userId, null);

		// THEN
		EndpointConfiguration[] expected = confs.stream().filter(e -> userId.equals(e.getUserId()))
				.toArray(EndpointConfiguration[]::new);
		then(results).as("Results for single user returned").contains(expected);
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<EndpointConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				EndpointConfiguration conf = newEndpointConfiguration(userId, randomUUID(),
						randomString(), new Long[] { randomLong() }, null, null);
				UserUuidPK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(userId);
		FilterResults<EndpointConfiguration, UserUuidPK> results = dao.findFiltered(filter);

		// THEN
		EndpointConfiguration[] expected = confs.stream().filter(e -> userId.equals(e.getUserId()))
				.toArray(EndpointConfiguration[]::new);
		then(results).as("Results for single user returned").contains(expected);
	}

	@Test
	public void updateEnabledStatus() throws Exception {
		// GIVEN
		final int count = 2;
		final int userCount = 2;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<EndpointConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				EndpointConfiguration conf = newEndpointConfiguration(userId, randomUUID(),
						randomString(), new Long[] { randomLong() }, null, null);
				UserUuidPK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final EndpointConfiguration conf = confs.get(1);
		final BasicFilter filter = new BasicFilter();
		filter.setEndpointId(conf.getEndpointId());
		int result = dao.updateEnabledStatus(conf.getUserId(), filter, false);

		// THEN
		then(result).as("One row updated").isEqualTo(1);

		EndpointConfiguration updated = dao.get(conf.getId());
		then(updated).as("Enabled status updated").returns(false, EndpointConfiguration::isEnabled);
	}

	@Test
	public void updateEnabledStatus_forUser() throws Exception {
		// GIVEN
		final int count = 2;
		final int userCount = 2;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<EndpointConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				EndpointConfiguration conf = newEndpointConfiguration(userId, randomUUID(),
						randomString(), new Long[] { randomLong() }, null, null);
				UserUuidPK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		int result = dao.updateEnabledStatus(userId, null, false);

		// THEN
		then(result).as("All rows for user updated").isEqualTo(count);

		List<Map<String, Object>> data = allEndpointConfigurationData(jdbcTemplate);
		for ( var row : data ) {
			Long rowUserId = (Long) row.get("user_id");
			if ( rowUserId.equals(userId) ) {
				then(row).as("Updated enabled for user").containsEntry("enabled", false);
			} else {
				then(row).as("Did not update enabled for other user").containsEntry("enabled", true);

			}
		}
	}

}
