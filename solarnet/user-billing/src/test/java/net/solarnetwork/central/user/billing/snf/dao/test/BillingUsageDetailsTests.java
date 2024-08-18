/* ==================================================================
 * BillingUsageTierDetailsTests.java - 18/08/2024 12:35:03 pm
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

package net.solarnetwork.central.user.billing.snf.dao.test;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.dao.AuditNodeServiceEntity.dailyAuditNodeService;
import static net.solarnetwork.central.dao.AuditUserServiceEntity.dailyAuditUserService;
import static net.solarnetwork.central.test.CommonDbTestUtils.allTableData;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.AuditNodeServiceValue;
import net.solarnetwork.central.domain.AuditUserServiceValue;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.central.user.billing.snf.test.OAuthTestUtils;
import net.solarnetwork.central.user.billing.snf.test.OcppTestUtils;
import net.solarnetwork.central.user.billing.snf.test.OscpTestUtils;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the
 * {@code solarbill.billing_usage_details(userid BIGINT, ts_min TIMESTAMP, ts_max TIMESTAMP, effective_date date)}
 * stored procedure.
 *
 * @author matt
 * @version 1.0
 */
public class BillingUsageDetailsTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static final ZoneId TEST_ZONE = ZoneId.of("Pacific/Auckland");

	@Test
	public void usage_basic() {
		// GIVEN

		// create test user
		final Long locId = CommonTestUtils.randomLong();
		setupTestLocation(locId, TEST_ZONE.getId());

		final Long userId = CommonTestUtils.randomLong();
		setupTestUser(userId, locId);

		// create datum streams
		final int datumStreamCount = 4;
		final var datumStreamMetas = new ArrayList<ObjectDatumStreamMetadata>(datumStreamCount);
		for ( int i = 0; i < datumStreamCount; i++ ) {
			datumStreamMetas.add(BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
					TEST_ZONE.getId(), ObjectDatumKind.Node, (long) i, "source/%d".formatted(i)));
			setupTestNode((long) i, locId);
			setupTestUserNode(userId, (long) i);
		}
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, datumStreamMetas);

		// populate daily audit data for all streams
		final int dayCount = 10;
		final ZonedDateTime month = ZonedDateTime.of(2024, 11, 1, 0, 0, 0, 0, TEST_ZONE);

		final var auditDatumDaily = new ArrayList<AuditDatum>(dayCount * datumStreamCount);
		final var auditAccDatumDaily = new ArrayList<AuditDatum>(dayCount * datumStreamCount);
		final var auditNodeServiceDaily = new ArrayList<AuditNodeServiceValue>(
				dayCount * datumStreamCount);

		for ( ObjectDatumStreamMetadata streamMeta : datumStreamMetas ) {
			for ( int i = 0; i < dayCount; i++ ) {
				final Instant day = month.plusDays(i).toInstant();

				// UUID streamId, Instant timestamp, Long datumCount,
				// Long datumHourlyCount, Integer datumDailyCount, Long datumPropertyCount,
				// Long datumQueryCount, Long datumPropertyUpdateCount, Long fluxDataInCount
				auditDatumDaily.add(AuditDatumEntity.dailyAuditDatum(streamMeta.getStreamId(), day,
						100_000L, 2_000L, 400_000, 200_000L, 5_000_000_000L, 50_000L, 8_000_000L));

				// UUID streamId, Instant timestamp, Long datumCount, Long datumHourlyCount,
				// Integer datumDailyCount, Integer datumMonthlyCount
				auditAccDatumDaily.add(AuditDatumEntity.accumulativeAuditDatum(streamMeta.getStreamId(),
						day, 100_000_000L, 600_000L, 400_000, 200_000));

				// populate node instruction audit records (each stream is from different node)
				auditNodeServiceDaily
						.add(dailyAuditNodeService(streamMeta.getObjectId(), "inst", day, 125_250L));
			}
		}
		DatumDbUtils.insertAuditDatum(log, jdbcTemplate, auditDatumDaily);
		DatumDbUtils.insertAuditDatum(log, jdbcTemplate, auditAccDatumDaily);
		DatumDbUtils.insertAuditNodeServiceValueDaily(log, jdbcTemplate, auditNodeServiceDaily);

		// populate user-level audit data
		final var auditUserServiceDaily = new ArrayList<AuditUserServiceValue>(dayCount);

		for ( int i = 0; i < dayCount; i++ ) {
			final Instant day = month.plusDays(i).toInstant();

			// populate flux data out audit records (each stream is from different node)
			auditUserServiceDaily.add(dailyAuditUserService(userId, "flxo", day, 655_250L));
		}
		DatumDbUtils.insertAuditUserServiceValueDaily(log, jdbcTemplate, auditUserServiceDaily);

		allTableData(log, jdbcTemplate, "solardatm.da_datm_meta", "node_id");
		allTableData(log, jdbcTemplate, "solardatm.aud_datm_daily", "stream_id,ts_start");
		allTableData(log, jdbcTemplate, "solardatm.aud_acc_datm_daily", "stream_id,ts_start");
		allTableData(log, jdbcTemplate, "solardatm.aud_node_daily", "node_id,service,ts_start");
		allTableData(log, jdbcTemplate, "solardatm.aud_user_daily", "user_id,service,ts_start");

		// WHEN
		List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
				select * from solarbill.billing_usage_details(?, ?, ?, ?)
				""", userId, Timestamp.valueOf(month.toLocalDateTime()),
				Timestamp.valueOf(month.plusMonths(1).toLocalDateTime()),
				Timestamp.valueOf(month.plusMonths(1).toLocalDateTime()));

		// THEN
		log.debug("Got billing_usage_details: [{}]",
				rows.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		then(rows).as("Single row returned for billing usage").hasSize(1);
		// @formatter:off
		then(rows.get(0))
			.as("Properties in summed from audit daily data datumPropertyCount")
			.containsEntry("prop_in", auditDatumDaily.stream().mapToLong(AuditDatum::getDatumPropertyCount).sum())

			.as("Datum out summed from audit daily data datumQueryCount")
			.containsEntry("datum_out", auditDatumDaily.stream().mapToLong(AuditDatum::getDatumQueryCount).sum())

			.as("Flux data in summed from audit daily data fluxDataInCount")
			.containsEntry("flux_data_in", auditDatumDaily.stream().mapToLong(AuditDatum::getFluxDataInCount).sum())

			.as("Datum stored summed from audit acc daily data datumTotalCount")
			.containsEntry("datum_stored", auditAccDatumDaily.stream().mapToLong(AuditDatum::getDatumTotalCount).sum())

			.as("Instructions summed from audit node service 'inst' daily data")
			.containsEntry("instr_issued", auditNodeServiceDaily.stream().filter(a -> "inst".equals(a.getService()))
					.mapToLong(AuditNodeServiceValue::getCount).sum())

			.as("Flux data out summed from audit user service 'flxo' daily data")
			.containsEntry("flux_data_out", auditUserServiceDaily.stream().filter(a -> "flxo".equals(a.getService()))
					.mapToLong(AuditUserServiceValue::getCount).sum())

			.as("No OCPP chargers")
			.containsEntry("ocpp_chargers", null)

			.as("No OSCP capacity groups")
			.containsEntry("oscp_cap_groups", null)

			.as("No OSCP capacity")
			.containsEntry("oscp_cap", null)

			.as("No DNP3 data points")
			.containsEntry("dnp3_data_points", null)

			.as("No OAuth client credentials")
			.containsEntry("oauth_client_creds", null)

			;
		// @formatter:on
	}

	@Test
	public void usage_oauth() {
		// GIVEN
		final ZonedDateTime month = ZonedDateTime.of(2024, 11, 1, 0, 0, 0, 0, TEST_ZONE);

		// create test user
		final Long locId = CommonTestUtils.randomLong();
		setupTestLocation(locId, TEST_ZONE.getId());

		final Long userId = CommonTestUtils.randomLong();
		setupTestUser(userId, locId);

		// create OSCP OAuth Flexibility Provider
		final int fpOauthCount = 5;
		for ( int i = 0; i < fpOauthCount * 2; i++ ) {
			// save 50% with OAuth == true
			OscpTestUtils.saveFlexibilityProviderAuthId(jdbcTemplate, userId, randomString(),
					i % 2 == 0);
		}

		// create ININ credentials
		final int ininCredOauthCount = 3;
		for ( int i = 0; i < ininCredOauthCount * 2; i++ ) {
			OAuthTestUtils.saveInstructionInputCredential(jdbcTemplate, userId, randomString(),
					i % 2 == 0);
		}

		allTableData(log, jdbcTemplate, "solaroscp.oscp_fp_token", "id");
		allTableData(log, jdbcTemplate, "solardin.inin_credential", "id");

		// WHEN
		List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
				select * from solarbill.billing_usage_details(?, ?, ?, ?)
				""", userId, Timestamp.valueOf(month.toLocalDateTime()),
				Timestamp.valueOf(month.plusMonths(1).toLocalDateTime()),
				Timestamp.valueOf(month.plusMonths(1).toLocalDateTime()));

		// THEN
		log.debug("Got billing_usage_details: [{}]",
				rows.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		then(rows).as("Single row returned for billing usage").hasSize(1);

		// @formatter:off
		then(rows.get(0))
			.as("No properties in")
			.containsEntry("prop_in", null)

			.as("No datum out")
			.containsEntry("datum_out", null)

			.as("No flux data in")
			.containsEntry("flux_data_in", null)

			.as("No datum stored")
			.containsEntry("datum_stored", null)

			.as("No instructions")
			.containsEntry("instr_issued", null)

			.as("No flex data out")
			.containsEntry("flux_data_out",null)

			.as("No OCPP chargers")
			.containsEntry("ocpp_chargers", null)

			.as("No OSCP capacity groups")
			.containsEntry("oscp_cap_groups", null)

			.as("No OSCP capacity")
			.containsEntry("oscp_cap", null)

			.as("No DNP3 data points")
			.containsEntry("dnp3_data_points", null)

			.as("OAuth client credentials is sum of OSCP and ININ credentials where oauth == true")
			.containsEntry("oauth_client_creds", (long)fpOauthCount + ininCredOauthCount)

			;
		// @formatter:on
	}

	@Test
	public void usage_ocpp() {
		// GIVEN
		final ZonedDateTime month = ZonedDateTime.of(2024, 11, 1, 0, 0, 0, 0, TEST_ZONE);

		// create test user
		final Long locId = randomLong();
		setupTestLocation(locId, TEST_ZONE.getId());

		final Long userId = randomLong();
		setupTestUser(userId, locId);

		final Long nodeId = randomLong();
		setupTestNode(nodeId, locId);
		setupTestUserNode(userId, nodeId);

		// create OCPP chargers
		final int ocppChargerCount = 5;
		for ( int i = 0; i < ocppChargerCount * 2; i++ ) {
			// save 50% with enabled == true
			OcppTestUtils.saveOcppCharger(jdbcTemplate, userId, nodeId, randomString(), "SolarNetwork",
					"Test", i % 2 == 0);
		}

		allTableData(log, jdbcTemplate, "solarev.ocpp_charge_point", "id");

		// WHEN
		List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
				select * from solarbill.billing_usage_details(?, ?, ?, ?)
				""", userId, Timestamp.valueOf(month.toLocalDateTime()),
				Timestamp.valueOf(month.plusMonths(1).toLocalDateTime()),
				Timestamp.valueOf(month.plusMonths(1).toLocalDateTime()));

		// THEN
		log.debug("Got billing_usage_details: [{}]",
				rows.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		then(rows).as("Single row returned for billing usage").hasSize(1);

		// @formatter:off
		then(rows.get(0))
			.as("No properties in")
			.containsEntry("prop_in", null)

			.as("No datum out")
			.containsEntry("datum_out", null)

			.as("No flux data in")
			.containsEntry("flux_data_in", null)

			.as("No datum stored")
			.containsEntry("datum_stored", null)

			.as("No instructions")
			.containsEntry("instr_issued", null)

			.as("No flex data out")
			.containsEntry("flux_data_out",null)

			.as("OCPP chargers is sum of enabled OCPP chargers")
			.containsEntry("ocpp_chargers", (long)ocppChargerCount)

			.as("No OSCP capacity groups")
			.containsEntry("oscp_cap_groups", null)

			.as("No OSCP capacity")
			.containsEntry("oscp_cap", null)

			.as("No DNP3 data points")
			.containsEntry("dnp3_data_points", null)

			.as("No OAuth client credentials")
			.containsEntry("oauth_client_creds", null)

			;
		// @formatter:on
	}

	@Test
	public void usage_oscp() {
		// GIVEN
		final ZonedDateTime month = ZonedDateTime.of(2024, 11, 1, 0, 0, 0, 0, TEST_ZONE);

		// create test user
		final Long locId = randomLong();
		setupTestLocation(locId, TEST_ZONE.getId());

		final Long userId = randomLong();
		setupTestUser(userId, locId);

		final Long nodeId = randomLong();
		setupTestNode(nodeId, locId);
		setupTestUserNode(userId, nodeId);

		// GIVEN
		final Long fpId = OscpTestUtils.saveFlexibilityProviderAuthId(jdbcTemplate, userId,
				randomUUID().toString(), false);
		final Long cpId = OscpTestUtils.saveCapacityProvider(jdbcTemplate, userId, fpId, "CP");
		final Long coId = OscpTestUtils.saveCapacityOptimizer(jdbcTemplate, userId, fpId, "CO");
		final int numOscpCapacityGroups = 150;
		final int numOscpFlexibilityAssets = 1;
		final String[] iProps = new String[] { "watts" };
		final String[] eProps = new String[] { "wattHours" };
		final Instant startDay = month.toInstant();
		final int numDays = 10;
		final var streamMetas = new ArrayList<ObjectDatumStreamMetadata>(
				numOscpCapacityGroups * numOscpFlexibilityAssets);
		final var datumDaily = new ArrayList<AggregateDatum>(
				numOscpCapacityGroups * numOscpFlexibilityAssets * numDays);
		for ( int i = 0; i < numOscpCapacityGroups; i++ ) {
			Long cgId = OscpTestUtils.saveCapacityGroup(jdbcTemplate, userId, "CG-%d".formatted(i),
					"CG-%d".formatted(i), cpId, coId);
			for ( int j = 0; j < numOscpFlexibilityAssets; j++ ) {
				String faIdent = "CG-%d Asset-%d".formatted(i, j);

				var streamMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_ZONE.getId(),
						ObjectDatumKind.Node, nodeId, faIdent, iProps, eProps, null);
				streamMetas.add(streamMeta);

				BigDecimal energy = BigDecimal.ZERO;
				for ( int t = 0; t < numDays; t++ ) {
					BigDecimal power = new BigDecimal(1_000_000L * (t + 1));
					BigDecimal[] iData = new BigDecimal[] { power };
					BigDecimal[][] iStats = new BigDecimal[][] {
							// count, min, max
							new BigDecimal[] { new BigDecimal(10L), new BigDecimal(0L), power } };

					BigDecimal[] aData = new BigDecimal[] { new BigDecimal(1_000_000L * (t + 1)) };
					BigDecimal end = energy.add(power);
					BigDecimal[][] aStats = new BigDecimal[][] {
							// diff, start, end
							new BigDecimal[] { power, energy, end } };
					datumDaily.add(new AggregateDatumEntity(streamMeta.getStreamId(),
							startDay.plus(t, ChronoUnit.DAYS), Aggregation.Day,
							DatumProperties.propertiesOf(iData, aData, null, null),
							DatumPropertiesStatistics.statisticsOf(iStats, aStats)));
					energy = end;
				}

				OscpTestUtils.saveFlexibilityAsset(jdbcTemplate, userId, faIdent, faIdent, cgId, nodeId,
						faIdent, iProps, eProps);
			}
		}

		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, streamMetas);
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datumDaily);

		allTableData(log, jdbcTemplate, "solardatm.agg_datm_daily", "stream_id,ts_start");
		allTableData(log, jdbcTemplate, "solaroscp.oscp_cg_conf", "id");

		// WHEN
		List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
				select * from solarbill.billing_usage_details(?, ?, ?, ?)
				""", userId, Timestamp.valueOf(month.toLocalDateTime()),
				Timestamp.valueOf(month.plusMonths(1).toLocalDateTime()),
				Timestamp.valueOf(month.plusMonths(1).toLocalDateTime()));

		// THEN
		log.debug("Got billing_usage_details: [{}]",
				rows.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		then(rows).as("Single row returned for billing usage").hasSize(1);

		// @formatter:off
		then(rows.get(0))
			.as("No properties in")
			.containsEntry("prop_in", null)

			.as("No datum out")
			.containsEntry("datum_out", null)

			.as("No flux data in")
			.containsEntry("flux_data_in", null)

			.as("No datum stored")
			.containsEntry("datum_stored", null)

			.as("No instructions")
			.containsEntry("instr_issued", null)

			.as("No flex data out")
			.containsEntry("flux_data_out",null)

			.as("No OCPP chargers")
			.containsEntry("ocpp_chargers", null)

			.as("OSCP capacity groups is sum of entities")
			.containsEntry("oscp_cap_groups", (long)numOscpCapacityGroups)

			.as("OSCP capacity is sum of max instantaneous watts property values")
			.containsEntry("oscp_cap", 1_000_000L * numDays * numOscpCapacityGroups * numOscpFlexibilityAssets)

			.as("No DNP3 data points")
			.containsEntry("dnp3_data_points", null)

			.as("No OAuth client credentials")
			.containsEntry("oauth_client_creds", null)

			;
		// @formatter:on
	}

}
