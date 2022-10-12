/* ==================================================================
 * JdbcCapacityGroupConfigurationDaoTests.java - 12/08/2022 6:33:46 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc.test;

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.allCapacityGroupConfigurationData;
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.allCapacityGroupMeasurementData;
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.newCapacityGroupConfiguration;
import static net.solarnetwork.codec.JsonUtils.getStringMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcFlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.MeasurementPeriod;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcCapacityGroupConfigurationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcCapacityGroupConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcFlexibilityProviderDao flexibilityProviderDao;
	private JdbcCapacityProviderConfigurationDao capacityProviderDao;
	private JdbcCapacityOptimizerConfigurationDao capacityOptimizerDao;

	private JdbcCapacityGroupConfigurationDao dao;
	private Long userId;
	private Long flexibilityProviderId;

	private CapacityProviderConfiguration lastProvider;
	private CapacityOptimizerConfiguration lastOptimzer;
	private CapacityGroupConfiguration last;
	private Instant lastCapacityProviderMeasurementDate;
	private Instant lastCapacityOptimizerMeasurementDate;

	@BeforeEach
	public void setup() {
		flexibilityProviderDao = new JdbcFlexibilityProviderDao(jdbcTemplate);
		capacityProviderDao = new JdbcCapacityProviderConfigurationDao(jdbcTemplate);
		capacityOptimizerDao = new JdbcCapacityOptimizerConfigurationDao(jdbcTemplate);
		dao = new JdbcCapacityGroupConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		flexibilityProviderId = flexibilityProviderDao
				.idForToken(flexibilityProviderDao.createAuthToken(unassignedEntityIdKey(userId)), false)
				.getEntityId();
	}

	@Test
	public void insert() {
		// GIVEN
		lastProvider = capacityProviderDao.get(capacityProviderDao.create(userId, OscpJdbcTestUtils
				.newCapacityProviderConf(userId, flexibilityProviderId, Instant.now())));
		lastOptimzer = capacityOptimizerDao.get(capacityOptimizerDao.create(userId, OscpJdbcTestUtils
				.newCapacityOptimizerConf(userId, flexibilityProviderId, Instant.now())));
		CapacityGroupConfiguration conf = new CapacityGroupConfiguration(
				UserLongCompositePK.unassignedEntityIdKey(userId), Instant.now());
		conf.setEnabled(true);
		conf.setName(randomUUID().toString());
		conf.setIdentifier(randomUUID().toString());
		conf.setCapacityProviderMeasurementPeriod(MeasurementPeriod.TwentyMinute);
		conf.setCapacityOptimizerMeasurementPeriod(MeasurementPeriod.FiveMinute);
		conf.setCapacityProviderId(lastProvider.getEntityId());
		conf.setCapacityOptimizerId(lastOptimzer.getEntityId());
		conf.setServiceProps(Collections.singletonMap("foo", randomUUID().toString()));

		// WHEN
		UserLongCompositePK result = dao.create(userId, conf);

		// THEN
		List<Map<String, Object>> data = allCapacityGroupConfigurationData(jdbcTemplate);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row ID has been generated by DB", row,
				hasEntry(equalTo("id"), allOf(notNullValue(), not(equalTo(conf.getEntityId())))));
		Long entityId = (Long) row.get("id");
		assertThat("Primary key returned", result, is(notNullValue()));
		assertThat("Row creation date", row, hasEntry("created", Timestamp.from(conf.getCreated())));
		assertThat("Row modification date is creation date", row,
				hasEntry("modified", Timestamp.from(conf.getCreated())));
		assertThat("Row user ID matches", row, hasEntry("user_id", conf.getUserId()));
		assertThat("Row enabled matches", row, hasEntry("enabled", conf.isEnabled()));
		assertThat("Row name matches", row, hasEntry("cname", conf.getName()));
		assertThat("Row ident matches", row, hasEntry("ident", conf.getIdentifier()));
		assertThat("Row CP measurement period matches", row,
				hasEntry("cp_meas_secs", conf.getCapacityProviderMeasurementPeriod().getCode()));
		assertThat("Row CO measurement period matches", row,
				hasEntry("co_meas_secs", conf.getCapacityOptimizerMeasurementPeriod().getCode()));
		assertThat("Row capacity provider ID", row, hasEntry("cp_id", conf.getCapacityProviderId()));
		assertThat("Row capacity optimizer ID", row, hasEntry("co_id", conf.getCapacityOptimizerId()));
		assertThat("Row serviceProps matches", getStringMap(row.get("sprops").toString()),
				is(equalTo(conf.getServiceProps())));
		assertThat("Row matches returned primary key result", result,
				is(equalTo(new UserLongCompositePK(userId, (Long) row.get("id")))));

		data = allCapacityGroupMeasurementData(jdbcTemplate, OscpRole.CapacityProvider);
		assertThat("Capacity Provider measurement table has 1 row", data, hasSize(1));
		row = data.get(0);
		assertThat("Capacity Provider measurement row user ID matches", row,
				hasEntry("user_id", conf.getUserId()));
		assertThat("Capacity Provider measurement row entity ID matches", row,
				hasEntry("cg_id", entityId));
		assertThat("Capacity Provider measurement row measurement not set yet", row,
				not(hasEntry("meas_at", entityId)));

		data = allCapacityGroupMeasurementData(jdbcTemplate, OscpRole.CapacityOptimizer);
		assertThat("Capacity Optimizer measurement table has 1 row", data, hasSize(1));
		row = data.get(0);
		assertThat("Capacity Optimizer measurement row user ID matches", row,
				hasEntry("user_id", conf.getUserId()));
		assertThat("Capacity Optimizer measurement row entity ID matches", row,
				hasEntry("cg_id", entityId));
		assertThat("Capacity Optimizer measurement row measurement not set yet", row,
				not(hasEntry("meas_at", entityId)));

		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		CapacityGroupConfiguration result = dao.get(last.getId());

		// THEN
		assertThat("Retrieved entity matches source", result, is(equalTo(last)));
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		lastProvider = capacityProviderDao.get(capacityProviderDao.create(userId, OscpJdbcTestUtils
				.newCapacityProviderConf(userId, flexibilityProviderId, Instant.now())));
		lastOptimzer = capacityOptimizerDao.get(capacityOptimizerDao.create(userId, OscpJdbcTestUtils
				.newCapacityOptimizerConf(userId, flexibilityProviderId, Instant.now())));

		// WHEN
		CapacityGroupConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setName(randomUUID().toString());
		conf.setIdentifier(randomUUID().toString());
		conf.setCapacityProviderMeasurementPeriod(MeasurementPeriod.ThirtyMinute);
		conf.setCapacityOptimizerMeasurementPeriod(MeasurementPeriod.TwentyMinute);
		conf.setCapacityProviderId(lastProvider.getEntityId());
		conf.setCapacityOptimizerId(lastOptimzer.getEntityId());
		conf.setServiceProps(Collections.singletonMap("bim", "bam"));

		UserLongCompositePK result = dao.save(conf);

		// THEN
		List<Map<String, Object>> data = allCapacityGroupConfigurationData(jdbcTemplate);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row ID has been generated by DB", row, hasEntry("id", conf.getEntityId()));
		assertThat("Primary key returned unchanged", result, is(equalTo(conf.getId())));
		assertThat("Row creation date unchanged", row,
				hasEntry("created", Timestamp.from(conf.getCreated())));
		assertThat("Row modification date is updated", row,
				hasEntry("modified", Timestamp.from(conf.getModified())));
		assertThat("Row user ID matches", row, hasEntry("user_id", conf.getUserId()));
		assertThat("Row enabled matches", row, hasEntry("enabled", conf.isEnabled()));
		assertThat("Row name matches", row, hasEntry("cname", conf.getName()));
		assertThat("Row ident matches", row, hasEntry("ident", conf.getIdentifier()));
		assertThat("Row cp_meas_secs matches", row,
				hasEntry("cp_meas_secs", conf.getCapacityProviderMeasurementPeriod().getCode()));
		assertThat("Row co_meas_secs matches", row,
				hasEntry("co_meas_secs", conf.getCapacityOptimizerMeasurementPeriod().getCode()));
		assertThat("Row provider ID matches", row, hasEntry("cp_id", conf.getCapacityProviderId()));
		assertThat("Row optimizer ID matches", row, hasEntry("co_id", conf.getCapacityOptimizerId()));
		assertThat("Row serviceProps matches", getStringMap(row.get("sprops").toString()),
				is(equalTo(conf.getServiceProps())));
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allCapacityGroupConfigurationData(jdbcTemplate);
		assertThat("Row deleted from db", data, hasSize(0));
	}

	@Test
	public void findForUser() {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		Map<Long, CapacityProviderConfiguration> userProviders = new LinkedHashMap<>(userCount);
		Map<Long, CapacityOptimizerConfiguration> userOptimizers = new LinkedHashMap<>(userCount);
		final List<CapacityGroupConfiguration> confs = new ArrayList<>(count);
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < count; i++ ) {
			Instant t = start.plusSeconds(i);
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
					Long flexibilityProviderId = flexibilityProviderDao
							.idForToken(flexibilityProviderDao
									.createAuthToken(unassignedEntityIdKey(userId)), false)
							.getEntityId();
					userProviders.put(userId,
							capacityProviderDao.get(capacityProviderDao.create(userId,
									OscpJdbcTestUtils.newCapacityProviderConf(userId,
											flexibilityProviderId, Instant.now()))));
					userOptimizers.put(userId,
							capacityOptimizerDao.get(capacityOptimizerDao.create(userId,
									OscpJdbcTestUtils.newCapacityOptimizerConf(userId,
											flexibilityProviderId, Instant.now()))));

				} else {
					userId = userIds.get(u);
				}
				CapacityGroupConfiguration conf = newCapacityGroupConfiguration(userId,
						userProviders.get(userId).getEntityId(),
						userOptimizers.get(userId).getEntityId(), t);
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<CapacityGroupConfiguration> results = dao.findAll(userId, null);

		// THEN
		CapacityGroupConfiguration[] expected = confs.stream().filter(e -> userId.equals(e.getUserId()))
				.toArray(CapacityGroupConfiguration[]::new);
		assertThat("Results for single user returned", results, contains(expected));
	}

	@Test
	public void findForCapacityGroupAndIdentifier() {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		Map<Long, CapacityProviderConfiguration> userProviders = new LinkedHashMap<>(userCount);
		Map<Long, CapacityOptimizerConfiguration> userOptimizers = new LinkedHashMap<>(userCount);
		final List<CapacityGroupConfiguration> confs = new ArrayList<>(count);
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < count; i++ ) {
			Instant t = start.plusSeconds(i);
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
					Long flexibilityProviderId = flexibilityProviderDao
							.idForToken(flexibilityProviderDao
									.createAuthToken(unassignedEntityIdKey(userId)), false)
							.getEntityId();
					userProviders.put(userId,
							capacityProviderDao.get(capacityProviderDao.create(userId,
									OscpJdbcTestUtils.newCapacityProviderConf(userId,
											flexibilityProviderId, Instant.now()))));
					userOptimizers.put(userId,
							capacityOptimizerDao.get(capacityOptimizerDao.create(userId,
									OscpJdbcTestUtils.newCapacityOptimizerConf(userId,
											flexibilityProviderId, Instant.now()))));

				} else {
					userId = userIds.get(u);
				}
				CapacityGroupConfiguration conf = newCapacityGroupConfiguration(userId,
						userProviders.get(userId).getEntityId(),
						userOptimizers.get(userId).getEntityId(), t);
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		final Long providerId = userProviders.get(userId).getEntityId();
		final CapacityGroupConfiguration expected = confs.stream()
				.filter(e -> userId.equals(e.getUserId())).findFirst().orElse(null);
		CapacityGroupConfiguration result = dao.findForCapacityProvider(userId, providerId,
				expected.getIdentifier());

		// THEN
		assertThat("Results returned", result, is(equalTo(expected)));
	}

	@Test
	public void updateCapacityProviderMeasurementDate_fromNull() {
		// GIVEN
		insert();

		// WHEN
		Instant ts = Instant.now();
		boolean result = dao.compareAndSetMeasurement(last.getId(), OscpRole.CapacityProvider, null, ts);

		// THEN
		assertThat("CapacityProvider measurement is set", result, is(equalTo(true)));
		List<Map<String, Object>> data = allCapacityGroupMeasurementData(jdbcTemplate,
				OscpRole.CapacityProvider);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row measurement date matches set value", row,
				hasEntry("meas_at", Timestamp.from(ts)));
		lastCapacityProviderMeasurementDate = ts;
	}

	@Test
	public void updateCapacityProviderMeasurementDate() {
		// GIVEN
		updateCapacityProviderMeasurementDate_fromNull();

		// WHEN
		Instant ts = Instant.now().plusSeconds(10);
		boolean result = dao.compareAndSetMeasurement(last.getId(), OscpRole.CapacityProvider,
				lastCapacityProviderMeasurementDate, ts);

		// THEN
		assertThat("CapacityProvider measurement is set", result, is(equalTo(true)));
		List<Map<String, Object>> data = allCapacityGroupMeasurementData(jdbcTemplate,
				OscpRole.CapacityProvider);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row measurement date matches set value", row,
				hasEntry("meas_at", Timestamp.from(ts)));
		lastCapacityProviderMeasurementDate = ts;
	}

	@Test
	public void updateCapacityProviderMeasurementDate_noMatch() {
		// GIVEN
		updateCapacityProviderMeasurementDate_fromNull();

		// WHEN
		Instant expected = Instant.now().plusSeconds(5);
		Instant ts = Instant.now().plusSeconds(10);
		boolean result = dao.compareAndSetMeasurement(last.getId(), OscpRole.CapacityProvider, expected,
				ts);

		// THEN
		assertThat("CapacityProvider measurement is not set", result, is(equalTo(false)));
		List<Map<String, Object>> data = allCapacityGroupMeasurementData(jdbcTemplate,
				OscpRole.CapacityProvider);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row measurement date matches previous value", row,
				hasEntry("meas_at", Timestamp.from(lastCapacityProviderMeasurementDate)));
	}

	@Test
	public void updateCapacityOptimizerMeasurementDate_fromNull() {
		// GIVEN
		insert();

		// WHEN
		Instant ts = Instant.now();
		boolean result = dao.compareAndSetMeasurement(last.getId(), OscpRole.CapacityOptimizer, null,
				ts);

		// THEN
		assertThat("CapacityOptimizer measurement is set", result, is(equalTo(true)));
		List<Map<String, Object>> data = allCapacityGroupMeasurementData(jdbcTemplate,
				OscpRole.CapacityOptimizer);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row measurement date matches set value", row,
				hasEntry("meas_at", Timestamp.from(ts)));
		lastCapacityOptimizerMeasurementDate = ts;
	}

	@Test
	public void updateCapacityOptimizerMeasurementDate() {
		// GIVEN
		updateCapacityOptimizerMeasurementDate_fromNull();

		// WHEN
		Instant ts = Instant.now().plusSeconds(10);
		boolean result = dao.compareAndSetMeasurement(last.getId(), OscpRole.CapacityOptimizer,
				lastCapacityOptimizerMeasurementDate, ts);

		// THEN
		assertThat("CapacityOptimizer measurement is set", result, is(equalTo(true)));
		List<Map<String, Object>> data = allCapacityGroupMeasurementData(jdbcTemplate,
				OscpRole.CapacityOptimizer);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row measurement date matches set value", row,
				hasEntry("meas_at", Timestamp.from(ts)));
		lastCapacityOptimizerMeasurementDate = ts;
	}

	@Test
	public void updateCapacityOptimizerMeasurementDate_noMatch() {
		// GIVEN
		updateCapacityOptimizerMeasurementDate_fromNull();

		// WHEN
		Instant expected = Instant.now().plusSeconds(5);
		Instant ts = Instant.now().plusSeconds(10);
		boolean result = dao.compareAndSetMeasurement(last.getId(), OscpRole.CapacityOptimizer, expected,
				ts);

		// THEN
		assertThat("CapacityOptimizer measurement is not set", result, is(equalTo(false)));
		List<Map<String, Object>> data = allCapacityGroupMeasurementData(jdbcTemplate,
				OscpRole.CapacityOptimizer);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row measurement date matches previous value", row,
				hasEntry("meas_at", Timestamp.from(lastCapacityOptimizerMeasurementDate)));
	}

	@Test
	public void get_meas() {
		// GIVEN
		updateCapacityProviderMeasurementDate_fromNull();

		Instant ts = Instant.now().plusSeconds(123);
		dao.compareAndSetMeasurement(last.getId(), OscpRole.CapacityOptimizer, null, ts);

		// WHEN
		CapacityGroupConfiguration result = dao.get(last.getId());

		// THEN
		assertThat("CapacityProvider measurement date returned",
				result.getCapacityProviderMeasurementDate(),
				is(equalTo(lastCapacityProviderMeasurementDate)));
		assertThat("CapacityOptimizer measurement date returned",
				result.getCapacityOptimizerMeasurementDate(), is(equalTo(ts)));
	}
}
