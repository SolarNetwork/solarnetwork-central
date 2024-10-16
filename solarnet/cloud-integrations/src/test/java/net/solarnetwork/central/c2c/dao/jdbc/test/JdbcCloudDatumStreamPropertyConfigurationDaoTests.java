/* ==================================================================
 * JdbcCloudDatumStreamPropertyConfigurationDaoTests.java - 4/10/2024 9:21:10 am
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

package net.solarnetwork.central.c2c.dao.jdbc.test;

import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamPropertyConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudDatumStreamPropertyConfiguration;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.datum.DatumSamplesType;

/**
 * Test cases for the {@link JdbcCloudDatumStreamPropertyConfigurationDao}
 * class.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcCloudDatumStreamPropertyConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCloudIntegrationConfigurationDao integrationDao;
	private JdbcCloudDatumStreamMappingConfigurationDao datumStreamMappingDao;
	private JdbcCloudDatumStreamPropertyConfigurationDao dao;
	private Long userId;

	private CloudDatumStreamPropertyConfiguration last;

	@BeforeEach
	public void setup() {
		dao = new JdbcCloudDatumStreamPropertyConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		integrationDao = new JdbcCloudIntegrationConfigurationDao(jdbcTemplate);
		datumStreamMappingDao = new JdbcCloudDatumStreamMappingConfigurationDao(jdbcTemplate);
	}

	private CloudIntegrationConfiguration createIntegration(Long userId, Map<String, Object> props) {
		CloudIntegrationConfiguration conf = CinJdbcTestUtils.newCloudIntegrationConfiguration(userId,
				randomString(), randomString(), props);
		CloudIntegrationConfiguration entity = integrationDao.get(integrationDao.save(conf));
		return entity;
	}

	private CloudDatumStreamMappingConfiguration createDatumStreamMapping(Long userId,
			Long integrationId, Map<String, Object> props) {
		CloudDatumStreamMappingConfiguration conf = CinJdbcTestUtils
				.newCloudDatumStreamMappingConfiguration(userId, integrationId, randomString(), props);
		CloudDatumStreamMappingConfiguration entity = datumStreamMappingDao
				.get(datumStreamMappingDao.save(conf));
		return entity;
	}

	@Test
	public void entityKey() {
		UserLongIntegerCompositePK id = new UserLongIntegerCompositePK(randomLong(), randomLong(),
				randomInt());
		CloudDatumStreamPropertyConfiguration result = dao.entityKey(id);

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
		final CloudIntegrationConfiguration integration = createIntegration(userId,
				Map.of("bim", "bam"));
		final CloudDatumStreamMappingConfiguration stream = createDatumStreamMapping(userId,
				integration.getConfigId(), Map.of("bif", "bop"));

		// @formatter:off
		final CloudDatumStreamPropertyConfiguration conf = newCloudDatumStreamPropertyConfiguration(
				userId,
				stream.getConfigId(),
				0,
				DatumSamplesType.Instantaneous,
				randomString(),
				CloudDatumStreamValueType.Reference,
				randomString(),
				new BigDecimal("1.234"),
				2
				);
		// @formatter:on

		// WHEN
		UserLongIntegerCompositePK result = dao.create(userId, stream.getConfigId(), conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserLongIntegerCompositePK::getUserId)
			.as("Datum stream ID as provided")
			.returns(stream.getConfigId(), UserLongIntegerCompositePK::getGroupId)
			.as("Index as provided")
			.returns(conf.getIndex(), UserLongIntegerCompositePK::getEntityId)
			;

		List<Map<String, Object>> data = allCloudDatumStreamPropertyConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asInstanceOf(list(Map.class))
			.element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row datum stream mapping ID")
			.containsEntry("map_id", stream.getConfigId())
			.as("Row index")
			.containsEntry("idx", conf.getIndex())
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row property type")
			.containsEntry("ptype", conf.getPropertyType().keyValue())
			.as("Row property name")
			.containsEntry("pname", conf.getPropertyName())
			.as("Row value reference")
			.containsEntry("vref", conf.getValueReference())
			.as("Row multiplier")
			.containsEntry("mult", conf.getMultiplier())
			.as("Row scale")
			.containsEntry("scale", conf.getScale())
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		CloudDatumStreamPropertyConfiguration result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		CloudDatumStreamPropertyConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setPropertyType(DatumSamplesType.Accumulating);
		conf.setPropertyName(randomString());
		conf.setValueType(CloudDatumStreamValueType.SpelExpression);
		conf.setValueReference(randomString());
		conf.setMultiplier(new BigDecimal("4.321"));
		conf.setScale(3);

		UserLongIntegerCompositePK result = dao.save(conf);
		CloudDatumStreamPropertyConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamPropertyConfigurationData(jdbcTemplate);
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
		List<Map<String, Object>> data = allCloudDatumStreamPropertyConfigurationData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUserStreamMapping() throws Exception {
		// GIVEN
		final int userCount = 2;
		final int integrationCount = 2;
		final int mappingCount = 2;
		final int count = 3;
		final List<CloudDatumStreamPropertyConfiguration> confs = new ArrayList<>(
				userCount * integrationCount * mappingCount * count);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int g = 0; g < integrationCount; g++ ) {
				final Long integrationId = createIntegration(userId, Map.of("foo", "bar")).getConfigId();
				for ( int s = 0; s < mappingCount; s++ ) {
					final Long mappingId = createDatumStreamMapping(userId, integrationId,
							Map.of("bim", "bam")).getConfigId();
					for ( int i = 0; i < count; i++ ) {
						// @formatter:off
						CloudDatumStreamPropertyConfiguration conf = newCloudDatumStreamPropertyConfiguration(
								userId,
								mappingId,
								i,
								DatumSamplesType.Instantaneous,
								randomString(),
								CloudDatumStreamValueType.Reference,
								randomString(),
								new BigDecimal("1.234"),
								2
								);
						// @formatter:on
						UserLongIntegerCompositePK id = dao.create(userId, mappingId, conf);
						conf = conf.copyWithId(id);
						confs.add(conf);
					}
				}
			}
		}

		// WHEN
		final CloudDatumStreamPropertyConfiguration randomConf = confs.get(RNG.nextInt(confs.size()));
		Collection<CloudDatumStreamPropertyConfiguration> results = dao.findAll(randomConf.getUserId(),
				randomConf.getDatumStreamMappingId(), null);

		// THEN
		CloudDatumStreamPropertyConfiguration[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId())
						&& randomConf.getDatumStreamMappingId().equals(e.getDatumStreamMappingId()))
				.toArray(CloudDatumStreamPropertyConfiguration[]::new);
		then(results).as("Results for single user stream mapping returned").contains(expected);
	}

	@Test
	public void deleteForUserStreamMapping() throws Exception {
		// GIVEN
		final int userCount = 2;
		final int integrationCount = 2;
		final int mappingCount = 2;
		final int count = 3;
		final List<CloudDatumStreamPropertyConfiguration> confs = new ArrayList<>(
				userCount * integrationCount * mappingCount * count);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int g = 0; g < integrationCount; g++ ) {
				final Long integrationId = createIntegration(userId, Map.of("foo", "bar")).getConfigId();
				for ( int s = 0; s < mappingCount; s++ ) {
					final Long mappingId = createDatumStreamMapping(userId, integrationId,
							Map.of("bim", "bam")).getConfigId();
					for ( int i = 0; i < count; i++ ) {
						// @formatter:off
						CloudDatumStreamPropertyConfiguration conf = newCloudDatumStreamPropertyConfiguration(
								userId,
								mappingId,
								i,
								DatumSamplesType.Instantaneous,
								randomString(),
								CloudDatumStreamValueType.Reference,
								randomString(),
								new BigDecimal("1.234"),
								2
								);
						// @formatter:on
						UserLongIntegerCompositePK id = dao.create(userId, mappingId, conf);
						conf = conf.copyWithId(id);
						confs.add(conf);
					}
				}
			}
		}

		// WHEN
		final CloudDatumStreamPropertyConfiguration randomConf = confs.get(RNG.nextInt(confs.size()));
		dao.delete(dao.entityKey(UserLongIntegerCompositePK.unassignedEntityIdKey(randomConf.getUserId(),
				randomConf.getDatumStreamMappingId())));

		// THEN
		List<Map<String, Object>> allRows = allCloudDatumStreamPropertyConfigurationData(jdbcTemplate);
		// @formatter:off
		then(allRows)
			.as("Should have deleted 3 rows")
			.hasSize(confs.size() - 3)
			.as("Should have deleted all rows for given (user,datumStreamMappingId) group")
			.noneMatch(row -> {
				return row.get("user_id").equals(randomConf.getUserId())
						&& row.get("map_id").equals(randomConf.getDatumStreamMappingId());
			})
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		final int userCount = 2;
		final int integrationCount = 2;
		final int mappingCount = 2;
		final int count = 3;
		final List<CloudDatumStreamPropertyConfiguration> confs = new ArrayList<>(
				userCount * integrationCount * mappingCount * count);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int g = 0; g < integrationCount; g++ ) {
				final Long integrationId = createIntegration(userId, Map.of("foo", "bar")).getConfigId();
				for ( int s = 0; s < mappingCount; s++ ) {
					final Long mappingId = createDatumStreamMapping(userId, integrationId,
							Map.of("bim", "bam")).getConfigId();
					for ( int i = 0; i < count; i++ ) {
						// @formatter:off
						CloudDatumStreamPropertyConfiguration conf = newCloudDatumStreamPropertyConfiguration(
								userId,
								mappingId,
								i,
								DatumSamplesType.Instantaneous,
								randomString(),
								CloudDatumStreamValueType.Reference,
								randomString(),
								new BigDecimal("1.234"),
								2
								);
						// @formatter:on
						UserLongIntegerCompositePK id = dao.create(userId, mappingId, conf);
						conf = conf.copyWithId(id);
						confs.add(conf);
					}
				}
			}
		}

		// WHEN
		final CloudDatumStreamPropertyConfiguration randomConf = confs.get(RNG.nextInt(confs.size()));
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomConf.getUserId());
		var results = dao.findFiltered(filter);

		// THEN
		CloudDatumStreamPropertyConfiguration[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId()))
				.toArray(CloudDatumStreamPropertyConfiguration[]::new);
		then(results).as("Results for single user returned").contains(expected);
	}

}
