/* ==================================================================
 * JdbcCapacityOptimizerConfigurationDaoTests.java - 12/08/2022 6:33:46 pm
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
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.allCapacityGroupMeasurementData;
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.allConfigurationData;
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.allHeartbeatData;
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.allTokenData;
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.newCapacityOptimizerConf;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcAssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcFlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.MeasurementStyle;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcCapacityOptimizerConfigurationDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCapacityOptimizerConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	@Autowired
	private PlatformTransactionManager txManager;

	private JdbcFlexibilityProviderDao flexibilityProviderDao;
	private JdbcCapacityProviderConfigurationDao capacityProviderDao;
	private JdbcCapacityGroupConfigurationDao capacityGroupDao;
	private JdbcAssetConfigurationDao assetDao;
	private JdbcCapacityOptimizerConfigurationDao dao;
	private Long userId;
	private Long flexibilityProviderId;

	private CapacityOptimizerConfiguration last;
	private SystemSettings lastSettings;
	private Instant lastOfflineDate;
	private Instant lastHeartbeatDate;

	@BeforeEach
	public void setup() {
		flexibilityProviderDao = new JdbcFlexibilityProviderDao(jdbcTemplate);
		capacityProviderDao = new JdbcCapacityProviderConfigurationDao(jdbcTemplate);
		capacityGroupDao = new JdbcCapacityGroupConfigurationDao(jdbcTemplate);
		assetDao = new JdbcAssetConfigurationDao(jdbcTemplate);
		dao = new JdbcCapacityOptimizerConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		flexibilityProviderId = flexibilityProviderDao
				.idForToken(flexibilityProviderDao.createAuthToken(unassignedEntityIdKey(userId)), false)
				.getEntityId();
	}

	@Test
	public void insert() {
		// GIVEN
		CapacityOptimizerConfiguration conf = newCapacityOptimizerConf(userId, flexibilityProviderId,
				Instant.now());

		// WHEN
		UserLongCompositePK result = dao.create(userId, conf);

		// THEN
		List<Map<String, Object>> data = allConfigurationData(jdbcTemplate, OscpRole.CapacityOptimizer);
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
		assertThat("Row reg status matches", row,
				hasEntry("reg_status", conf.getRegistrationStatus().getCode()));
		assertThat("Row name matches", row, hasEntry("cname", conf.getName()));
		assertThat("Row baseUrl matches", row, hasEntry("url", conf.getBaseUrl()));
		assertThat("Row serviceProps matches", getStringMap(row.get("sprops").toString()),
				is(equalTo(conf.getServiceProps())));
		assertThat("Row matches returned primary key result", result,
				is(equalTo(new UserLongCompositePK(userId, (Long) row.get("id")))));

		data = allHeartbeatData(jdbcTemplate, OscpRole.CapacityOptimizer);
		assertThat("Heartbeat table has 1 row", data, hasSize(1));
		row = data.get(0);
		assertThat("Heartbeat row user ID matches", row, hasEntry("user_id", conf.getUserId()));
		assertThat("Heartbeat row entity ID matches", row, hasEntry("id", entityId));
		assertThat("Heartbeat row heartbeat", row, not(hasEntry("heartbeat_at", entityId)));

		last = conf.copyWithId(result);
	}

	@Test
	public void insert_authToken() {
		// GIVEN
		insert();

		// WHEN
		String token = randomUUID().toString();
		dao.saveExternalSystemAuthToken(last.getId(), token);

		// THEN
		List<Map<String, Object>> data = allTokenData(jdbcTemplate, OscpRole.CapacityOptimizer);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID has been assigned", row, hasEntry("user_id", last.getUserId()));
		assertThat("Row ID has been assigned", row, hasEntry("id", last.getEntityId()));
		assertThat("Row creation date assigned", row, hasEntry(equalTo("created"), notNullValue()));
		assertThat("Row modification date is creation date", row,
				hasEntry("modified", row.get("created")));
		assertThat("Row token matches token value", row, hasEntry("token", token));
	}

	@Test
	public void get_authToken() {
		// GIVEN
		insert();

		final String token = randomUUID().toString();
		dao.saveExternalSystemAuthToken(last.getId(), token);

		// WHEN
		String result = dao.getExternalSystemAuthToken(last.getId());

		assertThat("Result token matches saved value", result, is(equalTo(token)));
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		CapacityOptimizerConfiguration result = dao.get(last.getId());

		// THEN
		assertThat("Retrieved entity ID matches source", result, is(equalTo(last)));
		assertThat("Retrieved enabled matches", result.isEnabled(), is(equalTo(last.isEnabled())));
		assertThat("Retrieved name matches", result.getName(), is(equalTo(last.getName())));
		assertThat("Retrieved oscpVersion matches", result.getOscpVersion(),
				is(equalTo(last.getOscpVersion())));
		assertThat("Retrieved baseUrl matches", result.getBaseUrl(), is(equalTo(last.getBaseUrl())));
		assertThat("Retrieved reg status matches", result.getRegistrationStatus(),
				is(equalTo(last.getRegistrationStatus())));
		assertThat("Retrieved flexibilityProviderId matches", result.getFlexibilityProviderId(),
				is(equalTo(last.getFlexibilityProviderId())));
		assertThat("Retrieved serviceProps matches", result.getServiceProps(),
				is(equalTo(last.getServiceProps())));
	}

	@Test
	public void getForUpdate() throws InterruptedException {
		// GIVEN
		insert();

		// WHEN
		AtomicBoolean updateFailed = new AtomicBoolean();

		try {
			TestTransaction.flagForCommit();
			TestTransaction.end();

			TransactionTemplate tt = new TransactionTemplate(txManager);

			tt.executeWithoutResult((ts) -> {
				CapacityOptimizerConfiguration result = dao.getForUpdate(last.getId());
				Thread t = new Thread(() -> {
					tt.executeWithoutResult((ts2) -> {
						try {
							jdbcTemplate.queryForList(
									"SELECT * FROM solaroscp.oscp_co_conf WHERE user_id = ? AND id = ? FOR UPDATE NOWAIT",
									result.getUserId(), result.getEntityId());
						} catch ( ConcurrencyFailureException e ) {
							updateFailed.set(true);
						}
						log.info("Update 2 signaling");
					});
				}, "Update 2");
				t.start();
				try {
					t.join(5000L);
				} catch ( InterruptedException e ) {
					// ignore
				}
			});
		} finally {
			JdbcTestUtils.deleteFromTables(jdbcTemplate, "solaruser.user_user", "solaroscp.oscp_co_conf",
					"solaroscp.oscp_fp_token");
		}

		// THEN
		assertThat("Update 2 failed", updateFailed.get(), is(equalTo(true)));
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		CapacityOptimizerConfiguration conf = last.copyWithId(last.getId());
		conf.setBaseUrl(randomUUID().toString());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setName(randomUUID().toString());
		conf.setRegistrationStatus(RegistrationStatus.Failed);
		conf.setServiceProps(Collections.singletonMap("bim", "bam"));

		UserLongCompositePK result = dao.save(conf);

		// THEN
		List<Map<String, Object>> data = allConfigurationData(jdbcTemplate, OscpRole.CapacityOptimizer);
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
		assertThat("Row reg status matches", row,
				hasEntry("reg_status", conf.getRegistrationStatus().getCode()));
		assertThat("Row name matches", row, hasEntry("cname", conf.getName()));
		assertThat("Row baseUrl matches", row, hasEntry("url", conf.getBaseUrl()));
		assertThat("Row serviceProps matches", getStringMap(row.get("sprops").toString()),
				is(equalTo(conf.getServiceProps())));
	}

	@Test
	public void saveSettings() {
		// GIVEN
		insert();

		// WHEN
		SystemSettings settings = new SystemSettings(123,
				EnumSet.of(MeasurementStyle.Continuous, MeasurementStyle.Intermittent));
		dao.saveSettings(last.getId(), settings);

		// THEN
		List<Map<String, Object>> data = allConfigurationData(jdbcTemplate, OscpRole.CapacityOptimizer);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row heartbeat matches return value", row,
				hasEntry("heartbeat_secs", settings.heartbeatSeconds()));
		lastSettings = settings;
	}

	@Test
	public void get_settings() {
		// GIVEN
		saveSettings();

		// WHEN
		CapacityOptimizerConfiguration result = dao.get(last.getId());

		// THEN
		assertThat("Retrieved entity ID matches source", result, is(equalTo(last)));
		assertThat("Retrieved enabled matches", result.isEnabled(), is(equalTo(last.isEnabled())));
		assertThat("Retrieved name matches", result.getName(), is(equalTo(last.getName())));
		assertThat("Retrieved oscpVersion matches", result.getOscpVersion(),
				is(equalTo(last.getOscpVersion())));
		assertThat("Retrieved baseUrl matches", result.getBaseUrl(), is(equalTo(last.getBaseUrl())));
		assertThat("Retrieved reg status matches", result.getRegistrationStatus(),
				is(equalTo(last.getRegistrationStatus())));
		assertThat("Retrieved flexibilityProviderId matches", result.getFlexibilityProviderId(),
				is(equalTo(last.getFlexibilityProviderId())));
		assertThat("Retrieved settings matches", result.getSettings(), is(equalTo(lastSettings)));
		assertThat("Retrieved settings matches", result.getHeartbeatDate(),
				is(equalTo(last.getHeartbeatDate())));
		assertThat("Retrieved serviceProps matches", result.getServiceProps(),
				is(equalTo(last.getServiceProps())));
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allConfigurationData(jdbcTemplate, OscpRole.CapacityOptimizer);
		assertThat("Row deleted from db", data, hasSize(0));
	}

	@Test
	public void findForUser() {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<Long> flexibilityProviderIds = new ArrayList<>(userCount);
		final List<CapacityOptimizerConfiguration> confs = new ArrayList<>(count);
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < count; i++ ) {
			Instant t = start.plusSeconds(i);
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				Long flexibilityProviderId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
					flexibilityProviderId = flexibilityProviderDao
							.idForToken(flexibilityProviderDao
									.createAuthToken(unassignedEntityIdKey(userId)), false)
							.getEntityId();
					flexibilityProviderIds.add(flexibilityProviderId);
				} else {
					userId = userIds.get(u);
					flexibilityProviderId = flexibilityProviderIds.get(u);
				}
				CapacityOptimizerConfiguration conf = OscpJdbcTestUtils.newCapacityOptimizerConf(userId,
						flexibilityProviderId, t);
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<CapacityOptimizerConfiguration> results = dao.findAll(userId, null);

		// THEN
		CapacityOptimizerConfiguration[] expected = confs.stream()
				.filter(e -> userId.equals(e.getUserId()))
				.toArray(CapacityOptimizerConfiguration[]::new);
		assertThat("Results for single user returned", results, contains(expected));
	}

	@Test
	public void updateOfflineDate() {
		// GIVEN
		insert();

		// WHEN
		Instant offline = Instant.now();
		dao.updateOfflineDate(last.getId(), offline);

		// THEN
		List<Map<String, Object>> data = allConfigurationData(jdbcTemplate, OscpRole.CapacityOptimizer);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row offline date matches set value", row,
				hasEntry("offline_at", Timestamp.from(offline)));
		lastOfflineDate = offline;
	}

	@Test
	public void get_offline() {
		// GIVEN
		updateOfflineDate();

		// WHEN
		CapacityOptimizerConfiguration result = dao.get(last.getId());

		// THEN
		assertThat("Offline date returned", result.getOfflineDate(), is(equalTo(lastOfflineDate)));
	}

	@Test
	public void updateHeartbeatDate_fromNull() {
		// GIVEN
		insert();

		// WHEN
		Instant ts = Instant.now();
		boolean result = dao.compareAndSetHeartbeat(last.getId(), null, ts);

		// THEN
		assertThat("Heartbeat is set", result, is(equalTo(true)));
		List<Map<String, Object>> data = allHeartbeatData(jdbcTemplate, OscpRole.CapacityOptimizer);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row heartbeat date matches set value", row,
				hasEntry("heartbeat_at", Timestamp.from(ts)));
		lastHeartbeatDate = ts;
	}

	@Test
	public void updateHeartbeatDate() {
		// GIVEN
		updateHeartbeatDate_fromNull();

		// WHEN
		Instant ts = Instant.now().plusSeconds(10);
		boolean result = dao.compareAndSetHeartbeat(last.getId(), lastHeartbeatDate, ts);

		// THEN
		assertThat("Heartbeat is set", result, is(equalTo(true)));
		List<Map<String, Object>> data = allHeartbeatData(jdbcTemplate, OscpRole.CapacityOptimizer);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row heartbeat date matches set value", row,
				hasEntry("heartbeat_at", Timestamp.from(ts)));
		lastHeartbeatDate = ts;
	}

	@Test
	public void updateHeartbeatDate_noMatch() {
		// GIVEN
		updateHeartbeatDate_fromNull();

		// WHEN
		Instant expected = Instant.now().plusSeconds(5);
		Instant ts = Instant.now().plusSeconds(10);
		boolean result = dao.compareAndSetHeartbeat(last.getId(), expected, ts);

		// THEN
		assertThat("Heartbeat is not set", result, is(equalTo(false)));
		List<Map<String, Object>> data = allHeartbeatData(jdbcTemplate, OscpRole.CapacityOptimizer);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row heartbeat date matches previous value", row,
				hasEntry("heartbeat_at", Timestamp.from(lastHeartbeatDate)));
	}

	@Test
	public void get_heartbeat() {
		// GIVEN
		updateHeartbeatDate_fromNull();

		// WHEN
		CapacityOptimizerConfiguration result = dao.get(last.getId());

		// THEN
		assertThat("Heartbeat date returned", result.getHeartbeatDate(), is(equalTo(lastHeartbeatDate)));
	}

	@Test
	public void processExpiredMeasurement() {
		// GIVEN
		CapacityOptimizerConfiguration conf = OscpJdbcTestUtils.newCapacityOptimizerConf(userId,
				flexibilityProviderId, Instant.now());
		UserLongCompositePK id = dao.create(userId, conf);
		jdbcTemplate.update("UPDATE solaroscp.oscp_co_conf SET reg_status = ?",
				RegistrationStatus.Registered.getCode());
		last = dao.get(id);

		CapacityProviderConfiguration provConf = capacityProviderDao
				.get(capacityProviderDao.create(userId, OscpJdbcTestUtils.newCapacityProviderConf(userId,
						flexibilityProviderId, Instant.now())));

		CapacityGroupConfiguration group = capacityGroupDao.get(
				capacityGroupDao.create(userId, OscpJdbcTestUtils.newCapacityGroupConfiguration(userId,
						provConf.getEntityId(), id.getEntityId(), Instant.now())));

		assetDao.create(userId, OscpJdbcTestUtils.newAssetConfiguration(userId, group.getEntityId(),
				OscpRole.CapacityOptimizer, Instant.now()));

		// WHEN
		Instant expectedTaskDate = group.getCapacityOptimizerMeasurementPeriod()
				.previousPeriodStart(Instant.now());
		Instant newTs = group.getCapacityOptimizerMeasurementPeriod().nextPeriodStart(expectedTaskDate);
		boolean result = dao.processExternalSystemWithExpiredMeasurement((ctx) -> {
			assertThat("Role is provider", ctx.role(), is(equalTo(OscpRole.CapacityOptimizer)));
			assertThat("Found optimizer row", ctx.config().getId(), is(equalTo(last.getId())));
			assertThat("Found group", ctx.groupIdentifier(), is(equalTo(group.getIdentifier())));
			assertThat("Task date is previous period start", ctx.taskDate(),
					is(equalTo(expectedTaskDate)));
			return newTs;
		});

		// THEN
		assertThat("Result 'true' when Instant returned from callback", result, is(equalTo(result)));
		List<Map<String, Object>> data = allCapacityGroupMeasurementData(jdbcTemplate,
				OscpRole.CapacityOptimizer);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID matches", row, hasEntry("user_id", last.getUserId()));
		assertThat("Row ID matches", row, hasEntry("cg_id", group.getEntityId()));
		assertThat("Row measurement date updated", row, hasEntry("meas_at", Timestamp.from(newTs)));
	}

	@Test
	public void processExpiredMeasurement_skipLocked() {
		// GIVEN
		CapacityOptimizerConfiguration conf = OscpJdbcTestUtils.newCapacityOptimizerConf(userId,
				flexibilityProviderId, Instant.now());
		UserLongCompositePK id = dao.create(userId, conf);
		jdbcTemplate.update("UPDATE solaroscp.oscp_co_conf SET reg_status = ?",
				RegistrationStatus.Registered.getCode());
		last = dao.get(id);

		CapacityProviderConfiguration provConf = capacityProviderDao
				.get(capacityProviderDao.create(userId, OscpJdbcTestUtils.newCapacityProviderConf(userId,
						flexibilityProviderId, Instant.now())));

		CapacityGroupConfiguration group = capacityGroupDao.get(
				capacityGroupDao.create(userId, OscpJdbcTestUtils.newCapacityGroupConfiguration(userId,
						provConf.getEntityId(), id.getEntityId(), Instant.now())));

		assetDao.create(userId, OscpJdbcTestUtils.newAssetConfiguration(userId, group.getEntityId(),
				OscpRole.CapacityOptimizer, Instant.now()));

		// WHEN
		Instant expectedTaskDate = group.getCapacityOptimizerMeasurementPeriod()
				.previousPeriodStart(Instant.now());
		Instant newTs = group.getCapacityOptimizerMeasurementPeriod().nextPeriodStart(expectedTaskDate);

		AtomicBoolean updateFailed = new AtomicBoolean();

		AtomicBoolean result = new AtomicBoolean(false);

		try {
			TestTransaction.flagForCommit();
			TestTransaction.end();

			TransactionTemplate tt = new TransactionTemplate(txManager);
			CountDownLatch latch = new CountDownLatch(1);

			tt.executeWithoutResult((ts) -> {
				boolean b = dao.processExternalSystemWithExpiredMeasurement((ctx) -> {
					log.info("Locked ID: {}", ctx.config().getId());

					Thread t = new Thread(() -> {
						tt.executeWithoutResult((ts2) -> {
							try {
								jdbcTemplate.queryForList(
										"SELECT * FROM solaroscp.oscp_cg_co_meas LIMIT 1 FOR UPDATE NOWAIT");
							} catch ( ConcurrencyFailureException e ) {
								updateFailed.set(true);
							} finally {
								log.info("Tx 2 signaling");
								latch.countDown();
							}
						});
					}, "Update 2");
					t.start();

					log.info("Tx 1 waiting within lock");
					try {
						latch.await(1L, TimeUnit.MINUTES);
					} catch ( InterruptedException e ) {
						// ignore
					}
					assertThat("Role is optimizer", ctx.role(), is(equalTo(OscpRole.CapacityOptimizer)));
					assertThat("Found optimizer row", ctx.config().getId(), is(equalTo(last.getId())));
					assertThat("Found group", ctx.groupIdentifier(), is(equalTo(group.getIdentifier())));
					assertThat("Task date is previous period start", ctx.taskDate(),
							is(equalTo(expectedTaskDate)));
					return newTs;
				});
				result.set(b);
			});
		} finally {
			JdbcTestUtils.deleteFromTables(jdbcTemplate, "solaruser.user_user");
		}

		assertThat("Update 1 succeeded", result.get(), is(equalTo(true)));
		assertThat("Update 2 failed", updateFailed.get(), is(equalTo(true)));
	}

}
