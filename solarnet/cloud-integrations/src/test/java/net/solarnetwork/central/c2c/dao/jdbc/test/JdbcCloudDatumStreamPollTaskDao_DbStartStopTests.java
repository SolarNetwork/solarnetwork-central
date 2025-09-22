/* ==================================================================
 * JdbcCloudDatumStreamPollTaskDao_DbStartStopTests.java - 13/10/2024 10:19:16 am
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

import static java.time.Instant.now;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamMappingConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamPollTaskEntityData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamRakeTaskEntityData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudIntegrationConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudDatumStreamPollTaskEntity;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudDatumStreamRakeTaskEntity;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Completed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Queued;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamRakeTaskDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the datum stream task related status triggers on the datum
 * stream and integration enabled columns.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcCloudDatumStreamPollTaskDao_DbStartStopTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCloudIntegrationConfigurationDao integrationDao;
	private JdbcCloudDatumStreamConfigurationDao datumStreamDao;
	private JdbcCloudDatumStreamMappingConfigurationDao datumStreamMappingDao;
	private JdbcCloudDatumStreamPollTaskDao datumStreamPollTaskDao;
	private JdbcCloudDatumStreamRakeTaskDao datumStreamRakeTaskDao;
	private Long userId;

	private Map<UserLongCompositePK, CloudIntegrationConfiguration> integrations;
	private Map<UserLongCompositePK, CloudDatumStreamConfiguration> datumStreams;
	private Map<UserLongCompositePK, CloudDatumStreamMappingConfiguration> datumStreamMappings;
	private Map<UserLongCompositePK, CloudDatumStreamPollTaskEntity> datumStreamPollTasks;
	private Map<UserLongCompositePK, CloudDatumStreamRakeTaskEntity> datumStreamRakeTasks;

	@BeforeEach
	public void setup() {
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		integrationDao = new JdbcCloudIntegrationConfigurationDao(jdbcTemplate);
		datumStreamDao = new JdbcCloudDatumStreamConfigurationDao(jdbcTemplate);
		datumStreamMappingDao = new JdbcCloudDatumStreamMappingConfigurationDao(jdbcTemplate);
		datumStreamPollTaskDao = new JdbcCloudDatumStreamPollTaskDao(jdbcTemplate);
		datumStreamRakeTaskDao = new JdbcCloudDatumStreamRakeTaskDao(jdbcTemplate);

		integrations = new LinkedHashMap<>();
		datumStreamMappings = new LinkedHashMap<>();
		datumStreams = new LinkedHashMap<>();
		datumStreamPollTasks = new LinkedHashMap<>();
		datumStreamRakeTasks = new LinkedHashMap<>();
	}

	private CloudIntegrationConfiguration createIntegration(Long userId, boolean enabled) {
		CloudIntegrationConfiguration conf = CinJdbcTestUtils.newCloudIntegrationConfiguration(userId,
				randomString(), randomString(), null);
		conf.setEnabled(enabled);
		CloudIntegrationConfiguration entity = integrationDao.get(integrationDao.save(conf));
		integrations.put(entity.getId(), entity);
		return entity;
	}

	private CloudDatumStreamConfiguration createDatumStream(Long userId, Long datumStreamMappingId,
			boolean enabled) {
		CloudDatumStreamConfiguration conf = CinJdbcTestUtils.newCloudDatumStreamConfiguration(userId,
				datumStreamMappingId, randomString(), ObjectDatumKind.Node, randomLong(), randomString(),
				randomString(), randomString(), null);
		conf.setEnabled(enabled);
		CloudDatumStreamConfiguration entity = datumStreamDao.get(datumStreamDao.save(conf));
		datumStreams.put(entity.getId(), entity);
		return entity;
	}

	private CloudDatumStreamMappingConfiguration createDatumStreamMapping(Long userId,
			Long integrationId, Map<String, Object> props) {
		CloudDatumStreamMappingConfiguration conf = CinJdbcTestUtils
				.newCloudDatumStreamMappingConfiguration(userId, integrationId, randomString(), props);
		CloudDatumStreamMappingConfiguration entity = datumStreamMappingDao
				.get(datumStreamMappingDao.save(conf));
		datumStreamMappings.put(entity.getId(), entity);
		return entity;
	}

	private CloudDatumStreamPollTaskEntity createDatumStreamPollTask(Long userId, Long datumStreamId,
			BasicClaimableJobState state) {
		// @formatter:off
		CloudDatumStreamPollTaskEntity conf = newCloudDatumStreamPollTaskEntity(userId,
				datumStreamId,
				state,
				now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
				now().truncatedTo(ChronoUnit.DAYS),
				randomString(),
				null)
				;
		// @formatter:on
		CloudDatumStreamPollTaskEntity entity = datumStreamPollTaskDao
				.get(datumStreamPollTaskDao.save(conf));
		datumStreamPollTasks.put(entity.getId(), entity);
		return entity;
	}

	private CloudDatumStreamRakeTaskEntity createDatumStreamRakeTask(Long userId, Long datumStreamId,
			BasicClaimableJobState state) {
		// @formatter:off
		CloudDatumStreamRakeTaskEntity conf = newCloudDatumStreamRakeTaskEntity(userId,
				datumStreamId,
				state,
				now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
				Period.ofDays(1),
				randomString(),
				null)
				;
		// @formatter:on
		CloudDatumStreamRakeTaskEntity entity = datumStreamRakeTaskDao
				.get(datumStreamRakeTaskDao.save(conf));
		datumStreamRakeTasks.put(entity.getId(), entity);
		return entity;
	}

	@Test
	public void disableIntegrationStopsAllAssociatedTasks() {
		// GIVEN
		final int integrationCount = 3;
		final int datumStreamCount = 5;
		for ( int i = 0; i < integrationCount; i++ ) {
			CloudIntegrationConfiguration integration = createIntegration(userId, true);
			for ( int d = 0; d < datumStreamCount; d++ ) {
				CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
						integration.getConfigId(), null);
				CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
						mapping.getConfigId(), RNG.nextBoolean());
				createDatumStreamPollTask(userId, datumStream.getConfigId(), Queued);
				createDatumStreamRakeTask(userId, datumStream.getConfigId(), Queued);
				createDatumStreamRakeTask(userId, datumStream.getConfigId(), Queued);
			}
		}

		final CloudIntegrationConfiguration randomIntegration = integrations.values().stream().toList()
				.get(RNG.nextInt(integrationCount));

		// WHEN
		BasicFilter filter = new BasicFilter();
		filter.setIntegrationId(randomIntegration.getConfigId());
		integrationDao.updateEnabledStatus(userId, filter, false);

		// THEN
		allCloudIntegrationConfigurationData(jdbcTemplate);
		allCloudDatumStreamMappingConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);
		final List<Map<String, Object>> pollTaskRows = allCloudDatumStreamPollTaskEntityData(
				jdbcTemplate);
		final List<Map<String, Object>> rakeTaskRows = allCloudDatumStreamRakeTaskEntityData(
				jdbcTemplate);

		// @formatter:off
		then(pollTaskRows)
			.as("Rows for poll tasks found")
			.hasSize(integrationCount * datumStreamCount)
			.satisfies(rows -> {
				for (int idx = 0, len = rows.size(); idx < len; idx++ ) {
					then(rows)
						.element(idx)
						.satisfies(row -> {
							CloudDatumStreamConfiguration datumStream = datumStreams.get(new UserLongCompositePK(
									userId, (Long)row.get("ds_id")));
							CloudDatumStreamMappingConfiguration mapping = datumStreamMappings.get(new UserLongCompositePK(
									userId, datumStream.getDatumStreamMappingId()));
							BasicClaimableJobState expectedState = mapping.getIntegrationId()
										.equals(randomIntegration.getConfigId()) && datumStream.isEnabled()
									? Completed : Queued;
							then(row)
								.as("""
									Poll task state only Completed for datum stream %d that was enabled under
									integration %d that was then disabled: %s
									""", datumStream.getConfigId(), datumStream.getDatumStreamMappingId(), datumStream)
								.containsEntry("status", expectedState.keyValue())
								;
						})
						;
				}
			})
			;
		then(rakeTaskRows)
			.as("Rows for rake tasks found")
			.hasSize(integrationCount * datumStreamCount * 2)
			.satisfies(rows -> {
				for (int idx = 0, len = rows.size(); idx < len; idx++ ) {
					then(rows)
						.element(idx)
						.satisfies(row -> {
							CloudDatumStreamConfiguration datumStream = datumStreams.get(new UserLongCompositePK(
									userId, (Long)row.get("ds_id")));
							CloudDatumStreamMappingConfiguration mapping = datumStreamMappings.get(new UserLongCompositePK(
									userId, datumStream.getDatumStreamMappingId()));
							BasicClaimableJobState expectedState = mapping.getIntegrationId()
										.equals(randomIntegration.getConfigId()) && datumStream.isEnabled()
									? Completed : Queued;
							then(row)
								.as("""
									Rake task state only Completed for datum stream %d that was enabled under
									integration %d that was then disabled: %s
									""", datumStream.getConfigId(), datumStream.getDatumStreamMappingId(), datumStream)
								.containsEntry("status", expectedState.keyValue())
								;
						})
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void enableIntegrationStartsAllAssociatedTasks() {
		// GIVEN
		final int integrationCount = 3;
		final int datumStreamCount = 5;
		for ( int i = 0; i < integrationCount; i++ ) {
			CloudIntegrationConfiguration integration = createIntegration(userId, false);
			for ( int d = 0; d < datumStreamCount; d++ ) {
				CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
						integration.getConfigId(), null);
				CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
						mapping.getConfigId(), RNG.nextBoolean());
				createDatumStreamPollTask(userId, datumStream.getConfigId(), Completed);
				createDatumStreamRakeTask(userId, datumStream.getConfigId(), Completed);
				createDatumStreamRakeTask(userId, datumStream.getConfigId(), Completed);
			}
		}

		allCloudIntegrationConfigurationData(jdbcTemplate);
		allCloudDatumStreamMappingConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);
		allCloudDatumStreamPollTaskEntityData(jdbcTemplate);
		allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);

		final CloudIntegrationConfiguration randomIntegration = integrations.values().stream().toList()
				.get(RNG.nextInt(integrationCount));

		log.info("Enabling integration {}", randomIntegration.getConfigId());

		// WHEN
		BasicFilter filter = new BasicFilter();
		filter.setIntegrationId(randomIntegration.getConfigId());
		integrationDao.updateEnabledStatus(userId, filter, true);

		// THEN
		allCloudIntegrationConfigurationData(jdbcTemplate);
		allCloudDatumStreamMappingConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);
		final List<Map<String, Object>> pollTaskRows = allCloudDatumStreamPollTaskEntityData(
				jdbcTemplate);
		final List<Map<String, Object>> rakeTaskRows = allCloudDatumStreamRakeTaskEntityData(
				jdbcTemplate);

		// @formatter:off
		then(pollTaskRows)
			.as("Rows for poll tasks found")
			.hasSize(integrationCount * datumStreamCount)
			.satisfies(rows -> {
				for (int idx = 0, len = rows.size(); idx < len; idx++ ) {
					then(rows)
						.element(idx)
						.satisfies(row -> {
							CloudDatumStreamConfiguration datumStream = datumStreams.get(new UserLongCompositePK(
									userId, (Long)row.get("ds_id")));
							CloudDatumStreamMappingConfiguration mapping = datumStreamMappings.get(new UserLongCompositePK(
									userId, datumStream.getDatumStreamMappingId()));
							BasicClaimableJobState expectedState = mapping.getIntegrationId()
										.equals(randomIntegration.getConfigId()) && datumStream.isEnabled()
									? Queued : Completed;
							then(row)
								.as("""
									Poll task state only Queued for datum stream %d that was enabled under
									integration %d that was then enabled: %s
									""", datumStream.getConfigId(), datumStream.getDatumStreamMappingId(), datumStream)
								.containsEntry("status", expectedState.keyValue())
								;
						})
						;
				}
			})
			;
		then(rakeTaskRows)
			.as("Rows for rake tasks found")
			.hasSize(integrationCount * datumStreamCount * 2)
			.satisfies(rows -> {
				for (int idx = 0, len = rows.size(); idx < len; idx++ ) {
					then(rows)
						.element(idx)
						.satisfies(row -> {
							CloudDatumStreamConfiguration datumStream = datumStreams.get(new UserLongCompositePK(
									userId, (Long)row.get("ds_id")));
							CloudDatumStreamMappingConfiguration mapping = datumStreamMappings.get(new UserLongCompositePK(
									userId, datumStream.getDatumStreamMappingId()));
							BasicClaimableJobState expectedState = mapping.getIntegrationId()
										.equals(randomIntegration.getConfigId()) && datumStream.isEnabled()
									? Queued : Completed;
							then(row)
								.as("""
									Rake task state only Queued for datum stream %d that was enabled under
									integration %d that was then enabled: %s
									""", datumStream.getConfigId(), datumStream.getDatumStreamMappingId(), datumStream)
								.containsEntry("status", expectedState.keyValue())
								;
						})
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void disableDatumStreamStopsAssociatedTask() {
		// GIVEN
		final int integrationCount = 5;
		final int datumStreamCount = 2;
		for ( int i = 0; i < integrationCount; i++ ) {
			CloudIntegrationConfiguration integration = createIntegration(userId,
					i == 0 ? true : RNG.nextBoolean());
			for ( int d = 0; d < datumStreamCount; d++ ) {
				CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
						integration.getConfigId(), null);
				CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
						mapping.getConfigId(), true);
				createDatumStreamPollTask(userId, datumStream.getConfigId(),
						integration.isEnabled() ? Queued : Completed);
				createDatumStreamRakeTask(userId, datumStream.getConfigId(),
						integration.isEnabled() ? Queued : Completed);
				createDatumStreamRakeTask(userId, datumStream.getConfigId(),
						integration.isEnabled() ? Queued : Completed);
			}
		}

		final List<CloudIntegrationConfiguration> enabledIntegrations = integrations.values().stream()
				.filter(CloudIntegrationConfiguration::isEnabled).toList();
		final CloudIntegrationConfiguration randomIntegration = enabledIntegrations
				.get(RNG.nextInt(enabledIntegrations.size()));
		final CloudDatumStreamMappingConfiguration randomMapping = datumStreamMappings.values().stream()
				.filter(map -> map.getIntegrationId().equals(randomIntegration.getConfigId())).toList()
				.get(RNG.nextInt(datumStreamCount));
		final CloudDatumStreamConfiguration randomDatumStream = datumStreams.values().stream()
				.filter(ds -> ds.getDatumStreamMappingId().equals(randomMapping.getConfigId())).findAny()
				.get();

		// WHEN
		BasicFilter filter = new BasicFilter();
		filter.setDatumStreamId(randomDatumStream.getConfigId());
		datumStreamDao.updateEnabledStatus(userId, filter, false);

		// THEN
		allCloudIntegrationConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);
		final List<Map<String, Object>> pollTaskRows = allCloudDatumStreamPollTaskEntityData(
				jdbcTemplate);
		final List<Map<String, Object>> rakeTaskRows = allCloudDatumStreamRakeTaskEntityData(
				jdbcTemplate);

		// @formatter:off
		then(pollTaskRows)
			.as("Rows for poll tasks found")
			.hasSize(integrationCount * datumStreamCount)
			.satisfies(rows -> {
				for (int idx = 0, len = rows.size(); idx < len; idx++ ) {
					then(rows)
						.element(idx)
						.satisfies(row -> {
							CloudDatumStreamConfiguration datumStream = datumStreams.get(new UserLongCompositePK(
									userId, (Long)row.get("ds_id")));
							CloudDatumStreamMappingConfiguration mapping = datumStreamMappings.get(new UserLongCompositePK(
									userId, datumStream.getDatumStreamMappingId()));
							CloudIntegrationConfiguration integration = integrations.get(new UserLongCompositePK(
									userId, mapping.getIntegrationId()));
							BasicClaimableJobState expectedState = datumStream.getConfigId()
										.equals(randomDatumStream.getConfigId()) || !integration.isEnabled()
									? Completed : Queued;
							then(row)
								.as("""
									Poll task state only Completed for datum stream %d that was enabled under
									integration %d that was then disabled: %s
									""", datumStream.getConfigId(), datumStream.getDatumStreamMappingId(), datumStream)
								.containsEntry("status", expectedState.keyValue())
								;
						})
						;
				}
			})
			;
		then(rakeTaskRows)
			.as("Rows for rake tasks found")
			.hasSize(integrationCount * datumStreamCount * 2)
			.satisfies(rows -> {
				for (int idx = 0, len = rows.size(); idx < len; idx++ ) {
					then(rows)
						.element(idx)
						.satisfies(row -> {
							CloudDatumStreamConfiguration datumStream = datumStreams.get(new UserLongCompositePK(
									userId, (Long)row.get("ds_id")));
							CloudDatumStreamMappingConfiguration mapping = datumStreamMappings.get(new UserLongCompositePK(
									userId, datumStream.getDatumStreamMappingId()));
							CloudIntegrationConfiguration integration = integrations.get(new UserLongCompositePK(
									userId, mapping.getIntegrationId()));
							BasicClaimableJobState expectedState = datumStream.getConfigId()
										.equals(randomDatumStream.getConfigId()) || !integration.isEnabled()
									? Completed : Queued;
							then(row)
								.as("""
									Rake task state only Completed for datum stream %d that was enabled under
									integration %d that was then disabled: %s
									""", datumStream.getConfigId(), datumStream.getDatumStreamMappingId(), datumStream)
								.containsEntry("status", expectedState.keyValue())
								;
						})
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void enableDatumStreamStartsAssociatedTask() {
		// GIVEN
		final int integrationCount = 5;
		final int datumStreamCount = 2;
		for ( int i = 0; i < integrationCount; i++ ) {
			CloudIntegrationConfiguration integration = createIntegration(userId,
					i == 0 ? true : RNG.nextBoolean());
			for ( int d = 0; d < datumStreamCount; d++ ) {
				CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
						integration.getConfigId(), null);
				CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
						mapping.getConfigId(), i == 0 && d == 0 ? false : RNG.nextBoolean());
				createDatumStreamPollTask(userId, datumStream.getConfigId(),
						integration.isEnabled() && datumStream.isEnabled() ? Queued : Completed);
				createDatumStreamRakeTask(userId, datumStream.getConfigId(),
						integration.isEnabled() && datumStream.isEnabled() ? Queued : Completed);
				createDatumStreamRakeTask(userId, datumStream.getConfigId(),
						integration.isEnabled() && datumStream.isEnabled() ? Queued : Completed);
			}
		}

		final List<CloudDatumStreamConfiguration> disabledDatumStreams = datumStreams.values().stream()
				.filter(e -> {
					CloudDatumStreamMappingConfiguration mapping = datumStreamMappings
							.get(new UserLongCompositePK(userId, e.getDatumStreamMappingId()));
					return !e.isEnabled() && integrations
							.get(new UserLongCompositePK(userId, mapping.getIntegrationId()))
							.isEnabled();
				}).toList();
		final CloudDatumStreamConfiguration randomDatumStream = disabledDatumStreams
				.get(RNG.nextInt(disabledDatumStreams.size()));

		allCloudIntegrationConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);
		allCloudDatumStreamPollTaskEntityData(jdbcTemplate);
		allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);

		// WHEN
		BasicFilter filter = new BasicFilter();
		filter.setDatumStreamId(randomDatumStream.getConfigId());
		datumStreamDao.updateEnabledStatus(userId, filter, true);

		// THEN
		allCloudIntegrationConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);
		final List<Map<String, Object>> pollTaskRows = allCloudDatumStreamPollTaskEntityData(
				jdbcTemplate);
		final List<Map<String, Object>> rakeTaskRows = allCloudDatumStreamRakeTaskEntityData(
				jdbcTemplate);

		// @formatter:off
		then(pollTaskRows)
			.as("Rows for poll tasks found")
			.hasSize(integrationCount * datumStreamCount)
			.satisfies(rows -> {
				for (int idx = 0, len = rows.size(); idx < len; idx++ ) {
					then(rows)
						.element(idx)
						.satisfies(row -> {
							CloudDatumStreamConfiguration datumStream = datumStreams.get(new UserLongCompositePK(
									userId, (Long)row.get("ds_id")));
							CloudDatumStreamMappingConfiguration mapping = datumStreamMappings.get(new UserLongCompositePK(
									userId, datumStream.getDatumStreamMappingId()));
							CloudIntegrationConfiguration integration = integrations.get(new UserLongCompositePK(
									userId, mapping.getIntegrationId()));
							BasicClaimableJobState expectedState = datumStream.getConfigId()
										.equals(randomDatumStream.getConfigId())
										|| (integration.isEnabled() && datumStream.isEnabled())
									? Queued : Completed;
							then(row)
								.as("""
									Poll task state only Queued for datum stream %d that was disabled under
									integration %d that was then enabled: %s
									""", datumStream.getConfigId(), datumStream.getDatumStreamMappingId(), datumStream)
								.containsEntry("status", expectedState.keyValue())
								;
						})
						;
				}
			})
			;
		then(rakeTaskRows)
			.as("Rows for rake tasks found")
			.hasSize(integrationCount * datumStreamCount * 2)
			.satisfies(rows -> {
				for (int idx = 0, len = rows.size(); idx < len; idx++ ) {
					then(rows)
						.element(idx)
						.satisfies(row -> {
							CloudDatumStreamConfiguration datumStream = datumStreams.get(new UserLongCompositePK(
									userId, (Long)row.get("ds_id")));
							CloudDatumStreamMappingConfiguration mapping = datumStreamMappings.get(new UserLongCompositePK(
									userId, datumStream.getDatumStreamMappingId()));
							CloudIntegrationConfiguration integration = integrations.get(new UserLongCompositePK(
									userId, mapping.getIntegrationId()));
							BasicClaimableJobState expectedState = datumStream.getConfigId()
										.equals(randomDatumStream.getConfigId())
										|| (integration.isEnabled() && datumStream.isEnabled())
									? Queued : Completed;
							then(row)
								.as("""
									Rake task state only Queued for datum stream %d that was disabled under
									integration %d that was then enabled: %s
									""", datumStream.getConfigId(), datumStream.getDatumStreamMappingId(), datumStream)
								.containsEntry("status", expectedState.keyValue())
								;
						})
						;
				}
			})
			;
		// @formatter:on
	}

}
