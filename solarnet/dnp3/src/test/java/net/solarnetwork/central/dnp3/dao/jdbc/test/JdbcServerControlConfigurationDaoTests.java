/* ==================================================================
 * JdbcServerControlConfigurationDaoTests.java - 7/08/2023 6:56:43 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.dao.jdbc.test;

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.dnp3.dao.jdbc.test.Dnp3JdbcTestUtils.allServerControlConfigurationData;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertLocation;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertNode;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.dao.jdbc.JdbcServerConfigurationDao;
import net.solarnetwork.central.dnp3.dao.jdbc.JdbcServerControlConfigurationDao;
import net.solarnetwork.central.dnp3.domain.ControlType;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.user.domain.UserNodePK;

/**
 * Test cases for the {@link JdbcServerControlConfigurationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcServerControlConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcServerConfigurationDao serverDao;

	private JdbcServerControlConfigurationDao dao;

	private Long userId;
	private ServerConfiguration lastServer;
	private ServerControlConfiguration last;

	@BeforeEach
	public void setup() {
		serverDao = new JdbcServerConfigurationDao(jdbcTemplate);
		dao = new JdbcServerControlConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void insert() {
		// GIVEN
		lastServer = serverDao.get(serverDao.create(userId,
				Dnp3JdbcTestUtils.newServerConfiguration(userId, UUID.randomUUID().toString())));

		// WHEN
		ServerControlConfiguration conf = new ServerControlConfiguration(userId,
				lastServer.getServerId(), 0, Instant.now());
		conf.setModified(Instant.now().plusMillis(234L));
		conf.setNodeId(UUID.randomUUID().getMostSignificantBits());
		conf.setControlId(UUID.randomUUID().toString());
		conf.setType(ControlType.Analog);
		UserLongIntegerCompositePK result = dao.create(userId, lastServer.getServerId(), conf);

		// THEN
		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(conf.getUserId(), UserLongIntegerCompositePK::getUserId)
			.as("Server ID as provided")
			.returns(conf.getServerId(), UserLongIntegerCompositePK::getGroupId)
			.as("Index as provided")
			.returns(conf.getIndex(), UserLongIntegerCompositePK::getEntityId)
			;

		List<Map<String, Object>> data = allServerControlConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", conf.getUserId())
			.as("Row server ID")
			.containsEntry("server_id", conf.getServerId())
			.as("Row index")
			.containsEntry("idx", conf.getIndex())
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			.as("Row node ID")
			.containsEntry("node_id", conf.getNodeId())
			.as("Row control ID")
			.containsEntry("control_id", conf.getControlId())
			.as("Row control type")
			.containsEntry("ctype", String.valueOf((char)conf.getType().getCode()))
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void insert_withProperty() {
		// GIVEN
		lastServer = serverDao.get(serverDao.create(userId,
				Dnp3JdbcTestUtils.newServerConfiguration(userId, UUID.randomUUID().toString())));

		// WHEN
		ServerControlConfiguration conf = new ServerControlConfiguration(userId,
				lastServer.getServerId(), 0, Instant.now());
		conf.setModified(Instant.now().plusMillis(234L));
		conf.setNodeId(UUID.randomUUID().getMostSignificantBits());
		conf.setControlId(UUID.randomUUID().toString());
		conf.setProperty(UUID.randomUUID().toString());
		conf.setType(ControlType.Analog);
		UserLongIntegerCompositePK result = dao.create(userId, lastServer.getServerId(), conf);

		// THEN
		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(conf.getUserId(), UserLongIntegerCompositePK::getUserId)
			.as("Server ID as provided")
			.returns(conf.getServerId(), UserLongIntegerCompositePK::getGroupId)
			.as("Index as provided")
			.returns(conf.getIndex(), UserLongIntegerCompositePK::getEntityId)
			;

		List<Map<String, Object>> data = allServerControlConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", conf.getUserId())
			.as("Row server ID")
			.containsEntry("server_id", conf.getServerId())
			.as("Row index")
			.containsEntry("idx", conf.getIndex())
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			.as("Row node ID")
			.containsEntry("node_id", conf.getNodeId())
			.as("Row control ID")
			.containsEntry("control_id", conf.getControlId())
			.as("Row property")
			.containsEntry("pname", conf.getProperty())
			.as("Row control type")
			.containsEntry("ctype", String.valueOf((char)conf.getType().getCode()))
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		ServerControlConfiguration result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void get_withProperty() {
		// GIVEN
		insert_withProperty();

		// WHEN
		ServerControlConfiguration result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		ServerControlConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setNodeId(UUID.randomUUID().getMostSignificantBits());
		conf.setControlId(UUID.randomUUID().toString());
		conf.setType(ControlType.Binary);

		UserLongIntegerCompositePK result = dao.save(conf);
		ServerControlConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allServerControlConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);
		// @formatter:off
		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(conf)
			.as("Entity saved updated values")
			.matches(c -> c.isSameAs(updated));
		// @formatter:on
	}

	@Test
	public void update_withProperty() {
		// GIVEN
		insert_withProperty();

		// WHEN
		ServerControlConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setNodeId(UUID.randomUUID().getMostSignificantBits());
		conf.setControlId(UUID.randomUUID().toString());
		conf.setProperty(UUID.randomUUID().toString());
		conf.setType(ControlType.Binary);

		UserLongIntegerCompositePK result = dao.save(conf);
		ServerControlConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allServerControlConfigurationData(jdbcTemplate);
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
		List<Map<String, Object>> data = allServerControlConfigurationData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int userCount = 3;
		final int serverCount = 3;
		final int count = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<ServerControlConfiguration> confs = new ArrayList<>(count);

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			userIds.add(userId);

			for ( int s = 0; s < serverCount; s++ ) {
				ServerConfiguration server = Dnp3JdbcTestUtils.newServerConfiguration(userId,
						UUID.randomUUID().toString());
				UserLongCompositePK serverId = serverDao.create(userId, server);
				server = server.copyWithId(serverId);

				for ( int i = 0; i < count; i++ ) {
					ServerControlConfiguration conf = new ServerControlConfiguration(userId,
							server.getServerId(), i, Instant.now());
					conf.setModified(conf.getCreated());
					conf.setNodeId(UUID.randomUUID().getMostSignificantBits());
					conf.setControlId(UUID.randomUUID().toString());
					conf.setType(ControlType.Binary);
					UserLongIntegerCompositePK id = dao.create(userId, server.getServerId(), conf);
					confs.add(conf.copyWithId(id));
				}
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<ServerControlConfiguration> results = dao.findAll(userId, null, null);

		// THEN
		ServerControlConfiguration[] expected = confs.stream().filter(e -> userId.equals(e.getUserId()))
				.toArray(ServerControlConfiguration[]::new);
		then(results).as("Results for single user returned").contains(expected);
	}

	@Test
	public void findForGroup() throws Exception {
		// GIVEN
		final int userCount = 3;
		final int serverCount = 3;
		final int count = 3;
		final Map<Long, List<Long>> userGroups = new HashMap<>(userCount);
		final List<ServerControlConfiguration> confs = new ArrayList<>(count);

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			userGroups.put(userId, new ArrayList<>(serverCount));

			for ( int s = 0; s < serverCount; s++ ) {
				ServerConfiguration server = Dnp3JdbcTestUtils.newServerConfiguration(userId,
						UUID.randomUUID().toString());
				UserLongCompositePK serverId = serverDao.create(userId, server);
				server = server.copyWithId(serverId);
				userGroups.get(userId).add(server.getServerId());

				for ( int i = 0; i < count; i++ ) {
					ServerControlConfiguration conf = new ServerControlConfiguration(userId,
							server.getServerId(), i, Instant.now());
					conf.setModified(conf.getCreated());
					conf.setNodeId(UUID.randomUUID().getMostSignificantBits());
					conf.setControlId(UUID.randomUUID().toString());
					conf.setType(ControlType.Binary);
					UserLongIntegerCompositePK id = dao.create(userId, server.getServerId(), conf);
					confs.add(conf.copyWithId(id));
				}
			}
		}

		// WHEN
		final Entry<Long, List<Long>> groups = userGroups.entrySet().iterator().next();
		final Long groupId = groups.getValue().get(1);
		Collection<ServerControlConfiguration> results = dao.findAll(groups.getKey(), groupId, null);

		// THEN
		ServerControlConfiguration[] expected = confs.stream()
				.filter(e -> groups.getKey().equals(e.getUserId()) && groupId.equals(e.getServerId()))
				.toArray(ServerControlConfiguration[]::new);
		then(results).as("Results for single group returned").contains(expected);
	}

	@Test
	public void findForGroup_validOwnership() throws Exception {
		// GIVEN
		final int userCount = 3;
		final int serverCount = 3;
		final int count = 6;
		final Map<Long, List<Long>> userGroups = new HashMap<>(userCount);
		final List<ServerControlConfiguration> confs = new ArrayList<>(count);
		final Set<UserNodePK> userNodeOwnership = new HashSet<>();
		final SecureRandom rng = new SecureRandom();

		final Long locationId = insertLocation(jdbcTemplate, TEST_LOC_COUNTRY, TEST_TZ);

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			userGroups.put(userId, new ArrayList<>(serverCount));

			for ( int s = 0; s < serverCount; s++ ) {
				ServerConfiguration server = Dnp3JdbcTestUtils.newServerConfiguration(userId,
						UUID.randomUUID().toString());
				UserLongCompositePK serverId = serverDao.create(userId, server);
				server = server.copyWithId(serverId);
				userGroups.get(userId).add(server.getServerId());

				for ( int i = 0; i < count; i++ ) {
					ServerControlConfiguration conf = new ServerControlConfiguration(userId,
							server.getServerId(), i, Instant.now());
					conf.setModified(conf.getCreated());
					conf.setNodeId(UUID.randomUUID().getMostSignificantBits());

					// always insert a node record, but only sometimes a user_node record
					// so some records should not be returned
					insertNode(jdbcTemplate, conf.getNodeId(), locationId);
					if ( rng.nextBoolean() ) {
						insertUserNode(jdbcTemplate, userId, conf.getNodeId());
						userNodeOwnership.add(new UserNodePK(userId, conf.getNodeId()));
					}

					conf.setControlId(UUID.randomUUID().toString());
					conf.setType(ControlType.Binary);
					UserLongIntegerCompositePK id = dao.create(userId, server.getServerId(), conf);
					confs.add(conf.copyWithId(id));
				}
			}
		}

		// WHEN
		final Entry<Long, List<Long>> groups = userGroups.entrySet().iterator().next();
		final Long groupId = groups.getValue().get(1);
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(groups.getKey());
		filter.setServerId(groupId);
		filter.setValidNodeOwnership(true);
		List<ServerControlConfiguration> results = stream(dao.findFiltered(filter).spliterator(), false)
				.toList();

		log.debug("Valid configurations found: [{}]", results.stream().map(Object::toString)
				.collect(Collectors.joining(",\n\t", "\n\t", "\n")));

		// THEN
		ServerControlConfiguration[] expected = confs.stream()
				.filter(e -> groups.getKey().equals(e.getUserId()) && groupId.equals(e.getServerId())
						&& userNodeOwnership.contains(new UserNodePK(e.getUserId(), e.getNodeId())))
				.toArray(ServerControlConfiguration[]::new);
		then(results).as("Results for single group with valid node ownership returned")
				.contains(expected);
	}

}
