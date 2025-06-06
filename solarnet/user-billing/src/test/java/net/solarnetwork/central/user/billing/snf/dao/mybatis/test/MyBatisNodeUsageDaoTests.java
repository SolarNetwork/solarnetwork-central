/* ==================================================================
 * MyBatisNodeUsageDaoTests.java - 22/07/2020 10:40:26 AM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.dao.mybatis.test;

import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.dao.AuditUserServiceEntity.dailyAuditUserService;
import static net.solarnetwork.central.test.CommonDbTestUtils.allTableData;
import static net.solarnetwork.central.user.billing.snf.domain.NamedCost.forTier;
import static net.solarnetwork.central.user.billing.snf.domain.NodeUsages.CLOUD_INTEGRATIONS_DATA_KEY;
import static net.solarnetwork.central.user.billing.snf.domain.UsageTier.tier;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dao.AuditNodeServiceEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.domain.AuditUserServiceValue;
import net.solarnetwork.central.user.billing.snf.dao.mybatis.MyBatisNodeUsageDao;
import net.solarnetwork.central.user.billing.snf.domain.NamedCost;
import net.solarnetwork.central.user.billing.snf.domain.NodeUsage;
import net.solarnetwork.central.user.billing.snf.domain.UsageTier;
import net.solarnetwork.central.user.billing.snf.domain.UsageTiers;
import net.solarnetwork.central.user.billing.snf.test.OcppTestUtils;
import net.solarnetwork.central.user.billing.snf.test.OscpTestUtils;

/**
 * Test cases for the {@link MyBatisNodeUsageDao}.
 *
 * @author matt
 * @version 2.0
 */
public class MyBatisNodeUsageDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final String TEST_TZ = "UTC";
	private static final ZoneId TEST_ZONE = ZoneId.of(TEST_TZ);

	private static final String[] USAGE_KEYS_SORTED;
	static {
		USAGE_KEYS_SORTED = new String[] { NodeUsage.DATUM_PROPS_IN_KEY, NodeUsage.DATUM_OUT_KEY,
				NodeUsage.DATUM_DAYS_STORED_KEY };
		Arrays.sort(USAGE_KEYS_SORTED);
	}

	private MyBatisNodeUsageDao dao;
	private Long userId;
	private Long locId;
	private Long nodeId;

	@BeforeEach
	public void setup() {
		dao = new MyBatisNodeUsageDao();
		dao.setSqlSessionTemplate(getSqlSessionTemplate());

		userId = UUID.randomUUID().getMostSignificantBits();
		setupTestUser(userId);

		locId = UUID.randomUUID().getMostSignificantBits();
		setupTestLocation(locId, TEST_TZ);

		nodeId = createTestNode(locId, userId);
	}

	private Long createTestNode(Long locId, Long userId) {
		Long nodeId = UUID.randomUUID().getMostSignificantBits();
		setupTestNode(nodeId, locId);
		setupTestUserNode(userId, nodeId);
		saveNodeName(nodeId, String.format("Test Node %d", nodeId));
		return nodeId;
	}

	@Test
	public void tiersForDate_wayBack() {
		// GIVEN
		final LocalDate date = LocalDate.of(2010, 1, 1);

		// WHEN
		UsageTiers result = dao.effectiveUsageTiers(date);

		assertThat("Effective tiers returned", result, notNullValue());
		assertThat("Effective date", result.getDate(), equalTo(date));
		List<UsageTier> tiers = result.getTiers();
		assertThat("3 usage tiers available", tiers, hasSize(3));
		String[] expectedCosts = new String[] { "0.000000006", "0.000002", "0.000009" };
		LocalDate expectedDate = LocalDate.of(2008, 1, 1);
		for ( int i = 0; i < USAGE_KEYS_SORTED.length; i++ ) {
			assertThat("Usage tier " + i, tiers.get(i),
					is(equalTo(tier(USAGE_KEYS_SORTED[i], 0, expectedCosts[i], expectedDate))));
		}
	}

	@Test
	public void tiersForDate_202007() {
		// GIVEN
		final LocalDate date = LocalDate.of(2020, 7, 1);

		// WHEN
		UsageTiers result = dao.effectiveUsageTiers(date);

		assertThat("Effective tiers returned", result, notNullValue());
		assertThat("Effective date", result.getDate(), equalTo(date));
		List<UsageTier> tiers = result.getTiers();
		assertThat("3x4 usage tiers available", tiers, hasSize(12));

		String[][] expectedCosts = new String[][] {
				new String[] { "0.0000004", "0.0000002", "0.00000005", "0.000000006" },
				new String[] { "0.000002", "0.000001", "0.0000005", "0.0000002" },
				new String[] { "0.000009", "0.000006", "0.000004", "0.000002" }, };
		LocalDate expectedDate = LocalDate.of(2020, 6, 1);
		for ( int i = 0; i < USAGE_KEYS_SORTED.length; i += 4 ) {
			assertThat("Usage tier " + i, tiers.get(i),
					is(equalTo(tier(USAGE_KEYS_SORTED[i], 0, expectedCosts[i][0], expectedDate))));
			assertThat("Usage tier " + i + 1, tiers.get(i + 1),
					is(equalTo(tier(USAGE_KEYS_SORTED[i], 50_000, expectedCosts[i][1], expectedDate))));
			assertThat("Usage tier " + i + 2, tiers.get(i + 2),
					is(equalTo(tier(USAGE_KEYS_SORTED[i], 400_000, expectedCosts[i][2], expectedDate))));
			assertThat("Usage tier " + i + 3, tiers.get(i + 3), is(
					equalTo(tier(USAGE_KEYS_SORTED[i], 1_000_000, expectedCosts[i][3], expectedDate))));
		}
	}

	@Test
	public void tiersForDate_202107() {
		// GIVEN
		final LocalDate date = LocalDate.of(2021, 7, 1);

		// WHEN
		UsageTiers result = dao.effectiveUsageTiers(date);

		assertThat("Effective tiers returned", result, notNullValue());
		assertThat("Effective date", result.getDate(), equalTo(date));
		List<UsageTier> tiers = result.getTiers();
		assertThat("3x4 usage tiers available", tiers, hasSize(12));
		// @formatter:off
		String[][] expectedTiers = new String[][] {
				new String[] { "datum-days-stored", "0", 			"0.00000005" },
				new String[] { "datum-days-stored", "10000000", 	"0.00000001" },
				new String[] { "datum-days-stored", "1000000000", 	"0.000000003" },
				new String[] { "datum-days-stored", "100000000000", "0.000000002" },
				new String[] { "datum-out", "0", 				"0.0000001" },
				new String[] { "datum-out", "10000000", 		"0.00000004" },
				new String[] { "datum-out", "1000000000", 		"0.000000004" },
				new String[] { "datum-out", "100000000000", 	"0.000000001" },
				new String[] { "datum-props-in", "0", 			"0.000005" },
				new String[] { "datum-props-in", "500000", 		"0.000003" },
				new String[] { "datum-props-in", "10000000", 	"0.0000008" },
				new String[] { "datum-props-in", "500000000",	"0.0000002" },
				};
		// @formatter:on
		LocalDate expectedDate = LocalDate.of(2021, 7, 1);
		for ( int i = 0; i < expectedTiers.length; i++ ) {
			assertThat("Usage tier " + i, tiers.get(i), is(equalTo(tier(expectedTiers[i][0],
					parseLong(expectedTiers[i][1]), expectedTiers[i][2], expectedDate))));
		}
	}

	@Test
	public void tiersForDate_202211() {
		// GIVEN
		final LocalDate date = LocalDate.of(2022, 11, 1);

		// WHEN
		UsageTiers result = dao.effectiveUsageTiers(date);

		assertThat("Effective tiers returned", result, notNullValue());
		assertThat("Effective date", result.getDate(), equalTo(date));
		List<UsageTier> tiers = result.getTiers();
		assertThat("5x4 usage tiers available", tiers, hasSize(20));
		// @formatter:off
		String[][] expectedTiers = new String[][] {
				new String[] { "datum-days-stored", "0", 			"0.00000005" },
				new String[] { "datum-days-stored", "10000000", 	"0.00000001" },
				new String[] { "datum-days-stored", "1000000000", 	"0.000000003" },
				new String[] { "datum-days-stored", "100000000000", "0.000000002" },
				new String[] { "datum-out", "0", 				"0.0000001" },
				new String[] { "datum-out", "10000000", 		"0.00000004" },
				new String[] { "datum-out", "1000000000", 		"0.000000004" },
				new String[] { "datum-out", "100000000000", 	"0.000000001" },
				new String[] { "datum-props-in", "0", 			"0.000005" },
				new String[] { "datum-props-in", "500000", 		"0.000003" },
				new String[] { "datum-props-in", "10000000", 	"0.0000008" },
				new String[] { "datum-props-in", "500000000",	"0.0000002" },
				new String[] { "ocpp-chargers", 	"0", 		"2"},
				new String[] { "ocpp-chargers", 	"250", 		"1"},
				new String[] { "ocpp-chargers", 	"12500", 	"0.5"},
				new String[] { "ocpp-chargers", 	"500000", 	"0.3"},
				new String[] { "oscp-cap-groups", 	"0", 		"50"},
				new String[] { "oscp-cap-groups", 	"30", 		"30"},
				new String[] { "oscp-cap-groups", 	"100", 		"15"},
				new String[] { "oscp-cap-groups", 	"300", 		"10"},
				};
		// @formatter:on
		LocalDate expectedDate = LocalDate.of(2022, 11, 1);
		for ( int i = 0; i < expectedTiers.length; i++ ) {
			assertThat("Usage tier " + i, tiers.get(i), is(equalTo(tier(expectedTiers[i][0],
					parseLong(expectedTiers[i][1]), expectedTiers[i][2], expectedDate))));
		}
	}

	@Test
	public void usageForUser_none() {
		// GIVEN
		final LocalDate month = LocalDate.of(2020, 1, 1);

		// WHEN
		List<NodeUsage> results = dao.findUsageForAccount(userId, month, null);

		// THEN
		assertThat("Results non-null but empty", results, hasSize(0));
	}

	@Test
	public void usageForUser_oneNodeOneSource_wayBack() {
		// GIVEN
		final LocalDate month = LocalDate.of(2010, 1, 1);
		final String sourceId = "S1";

		// add 10 days worth of audit data
		final int numDays = 10;
		for ( int dayOffset = 0; dayOffset < numDays; dayOffset++ ) {
			Instant day = month.plusDays(dayOffset).atStartOfDay(TEST_ZONE).toInstant();
			addAuditAccumulatingDatumDaily(nodeId, sourceId, day, 1000, 2000, 3000, 4000);
			addAuditDatumDaily(nodeId, sourceId, day, 100, 200, 2_500L, 300, (short) 400, true);
		}

		debugRows("solardatm.aud_acc_datm_daily", "ts_start");
		debugQuery(format(
				"select * from solarbill.billing_node_tier_details(%d, '2010-01-01'::timestamp, '2010-02-01'::timestamp, '2010-01-01'::date)",
				userId));

		// WHEN
		List<NodeUsage> results = dao.findUsageForAccount(userId, month, month.plusMonths(1));

		// THEN
		assertThat("Results non-null with single result", results, hasSize(1));
		NodeUsage usage = results.get(0);
		assertThat("Properties in count aggregated", usage.getDatumPropertiesIn(),
				equalTo(BigInteger.valueOf(100L * numDays)));
		assertThat("Datum out count aggregated", usage.getDatumOut(),
				equalTo(BigInteger.valueOf(200L * numDays)));
		assertThat("Datum stored count aggregated", usage.getDatumDaysStored(),
				equalTo(BigInteger.valueOf((1000L + 2000L + 3000L + 4000L) * numDays)));

		// see {@link #tiersForDate_wayBack()}
		UsageTiers tiers = dao.effectiveUsageTiers(month);
		Map<String, List<UsageTier>> tierMap = tiers.tierMap();

		assertThat("Properties in cost", usage.getDatumPropertiesInCost(),
				equalTo(new BigDecimal(usage.getDatumPropertiesIn())
						.multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost())));
		assertThat("Datum out cost", usage.getDatumOutCost().setScale(3),
				equalTo(new BigDecimal(usage.getDatumOut())
						.multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost()).setScale(3)));
		assertThat("Datum stored cost", usage.getDatumDaysStoredCost(),
				equalTo(new BigDecimal(usage.getDatumDaysStored())
						.multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost())));
	}

	@Test
	public void usageForUser_oneNodeOneSource_moreRecent() {
		// GIVEN
		final LocalDate month = LocalDate.of(2020, 7, 1);
		final String sourceId = "S1";

		// add 10 days worth of audit data
		final int numDays = 10;
		for ( int dayOffset = 0; dayOffset < numDays; dayOffset++ ) {
			Instant day = month.plusDays(dayOffset).atStartOfDay(TEST_ZONE).toInstant();
			addAuditAccumulatingDatumDaily(nodeId, sourceId, day, 1000000, 2000000, 3000000, 4000000);
			addAuditDatumDaily(nodeId, sourceId, day, 100000, 200000, 250_000L, 300000, (short) 400000,
					true);
		}

		debugRows("solardatm.aud_acc_datm_daily", "ts_start");
		debugQuery(format(
				"select * from solarbill.billing_usage_tier_details(%d, '2020-07-01'::timestamp, '2020-08-01'::timestamp, '2020-07-01'::date)",
				userId));

		// WHEN
		UsageTiers tiers = dao.effectiveUsageTiers(month);
		Map<String, List<UsageTier>> tierMap = tiers.tierMap();

		List<NodeUsage> r1 = dao.findNodeUsageForAccount(userId, month, month.plusMonths(1));
		List<NodeUsage> r2 = dao.findUsageForAccount(userId, month, month.plusMonths(1));

		// THEN
		int i = 0;
		for ( List<NodeUsage> results : Arrays.asList(r1, r2) ) {
			assertThat("Results non-null with single result", results, hasSize(1));
			NodeUsage usage = results.get(0);
			if ( i == 0 ) {
				assertThat("Node ID present for node-level usage", usage.getId(), equalTo(nodeId));
				assertThat("Node usage description is node name", usage.getDescription(),
						equalTo(format("Test Node %d", nodeId)));
			} else {
				assertThat("No node ID for account-level usage", usage.getId(), nullValue());
			}
			assertThat("Properties in count aggregated", usage.getDatumPropertiesIn(),
					equalTo(BigInteger.valueOf(100000L * numDays)));
			assertThat("Datum out count aggregated", usage.getDatumOut(),
					equalTo(BigInteger.valueOf(200000L * numDays)));
			assertThat("Datum stored count aggregated", usage.getDatumDaysStored(),
					equalTo(BigInteger.valueOf((1000000L + 2000000L + 3000000L + 4000000L) * numDays)));

			// see {@link #tiersForDate_wayBack()}
			Map<String, List<NamedCost>> tiersBreakdown = usage.getTiersCostBreakdown();
			List<NamedCost> propsInTiersCost = tiersBreakdown.get(NodeUsage.DATUM_PROPS_IN_KEY);
			assertThat("Properties in cost tier count", propsInTiersCost, hasSize(3));
			List<NamedCost> datumOutTiersCost = tiersBreakdown.get(NodeUsage.DATUM_OUT_KEY);
			assertThat("Datum out cost tier count", datumOutTiersCost, hasSize(4));
			List<NamedCost> datumStoredTiersCost = tiersBreakdown.get(NodeUsage.DATUM_DAYS_STORED_KEY);
			assertThat("Datum stored cost tier count", datumStoredTiersCost, hasSize(4));

			/*-
			min=0		tier_prop_in=50000	cost_prop_in=0.000009	prop_in_cost=0.450000	tier_datum_stored=50000		cost_datum_stored=4E-7	datum_stored_cost=0.0200000		tier_datum_out=50000	cost_datum_out=0.000002	datum_out_cost=0.100000		total_cost=0.57}
			min=50000	tier_prop_in=350000	cost_prop_in=0.000006	prop_in_cost=2.100000	tier_datum_stored=350000	cost_datum_stored=2E-7	datum_stored_cost=0.0700000		tier_datum_out=350000	cost_datum_out=0.000001	datum_out_cost=0.350000		total_cost=2.52}
			min=400000	tier_prop_in=600000	cost_prop_in=0.000004	prop_in_cost=2.400000	tier_datum_stored=600000	cost_datum_stored=5E-8	datum_stored_cost=0.03000000	tier_datum_out=600000	cost_datum_out=5E-7		datum_out_cost=0.3000000	total_cost=2.73}
			min=1000000	tier_prop_in=0		cost_prop_in=0.000002	prop_in_cost=0.000000	tier_datum_stored=99000000	cost_datum_stored=6E-9	datum_stored_cost=0.594000000	tier_datum_out=1000000	cost_datum_out=2E-7		datum_out_cost=0.2000000	total_cost=0.79}
			*/

			// costs only available with account-level usage
			if ( i == 1 ) {
				// @formatter:off
				assertThat("Properties in cost", usage.getDatumPropertiesInCost().setScale(3), equalTo(
								new BigDecimal("50000") .multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost())
						.add(	new BigDecimal("350000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()))
						.add(	new BigDecimal("600000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(2).getCost()))
						.setScale(3)
						));
				assertThat("Properties in cost tiers", propsInTiersCost, contains(
						NamedCost.forTier(1, "50000", 	new BigDecimal("50000") .multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "350000", 	new BigDecimal("350000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()).toString()),
						NamedCost.forTier(3, "600000", 	new BigDecimal("600000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(2).getCost()).toString())));

				assertThat("Datum out cost", usage.getDatumOutCost().setScale(3), equalTo(
								new BigDecimal("50000")  .multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost())
						.add(	new BigDecimal("350000") .multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(1).getCost()))
						.add(	new BigDecimal("600000") .multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(2).getCost()))
						.add(	new BigDecimal("1000000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(3).getCost()))
						.setScale(3)
						));
				assertThat("Datum out cost tiers", datumOutTiersCost, contains(
						NamedCost.forTier(1, "50000", 	new BigDecimal("50000")  .multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "350000", 	new BigDecimal("350000") .multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(1).getCost()).toString()),
						NamedCost.forTier(3, "600000", 	new BigDecimal("600000") .multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(2).getCost()).toString()),
						NamedCost.forTier(4, "1000000", new BigDecimal("1000000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(3).getCost()).toString())));

				assertThat("Datum stored cost", usage.getDatumDaysStoredCost().setScale(3), equalTo(
								new BigDecimal("50000")   .multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost())
						.add(	new BigDecimal("350000")  .multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()))
						.add(	new BigDecimal("600000")  .multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(2).getCost()))
						.add(	new BigDecimal("99000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(3).getCost()))
						.setScale(3)
						));
				assertThat("Datum stored cost tiers", datumStoredTiersCost, contains(
						NamedCost.forTier(1, "50000", 		new BigDecimal("50000")   .multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "350000", 		new BigDecimal("350000")  .multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()).toString()),
						NamedCost.forTier(3, "600000", 		new BigDecimal("600000")  .multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(2).getCost()).toString()),
						NamedCost.forTier(4, "99000000",	new BigDecimal("99000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(3).getCost()).toString())));
				// @formatter:on
			}
			i++;
		}

	}

	@Test
	public void usageForAccount_twoNodesOneSource_moreRecent() {
		// GIVEN
		Long nodeId2 = createTestNode(locId, userId);
		final LocalDate month = LocalDate.of(2020, 7, 1);
		final String sourceId = "S1";

		// add 10 days worth of audit data
		final int numDays = 10;
		for ( int dayOffset = 0; dayOffset < numDays; dayOffset++ ) {
			Instant day = month.plusDays(dayOffset).atStartOfDay(TEST_ZONE).toInstant();
			addAuditAccumulatingDatumDaily(nodeId, sourceId, day, 1_000_000, 2_000_000, 3_000_000,
					4_000_000);
			addAuditAccumulatingDatumDaily(nodeId2, sourceId, day, 500_000, 1_500_000, 2_500_000,
					3_500_000);

			addAuditDatumDaily(nodeId, sourceId, day, 100_000, 200_000, 2_500_000L, 300_000,
					(short) 400_000, true);
			addAuditDatumDaily(nodeId2, sourceId, day, 50_000, 150_000, 1_500_000L, 250_000,
					(short) 350_000, true);
		}

		debugRows("solardatm.aud_acc_datm_daily", "ts_start");
		debugQuery(format(
				"select * from solarbill.billing_usage_tier_details(%d, '2020-07-01'::timestamp, '2020-08-01'::timestamp, '2020-07-01'::date)",
				userId));

		// WHEN
		List<NodeUsage> results = dao.findUsageForAccount(userId, month, month.plusMonths(1));

		// THEN
		assertThat("Results non-null with single result", results, hasSize(1));
		NodeUsage usage = results.get(0);
		assertThat("No node ID for account-level usage", usage.getId(), nullValue());
		assertThat("Properties in count aggregated", usage.getDatumPropertiesIn(),
				equalTo(BigInteger.valueOf(150_000L * numDays)));
		assertThat("Datum out count aggregated", usage.getDatumOut(),
				equalTo(BigInteger.valueOf(350_000L * numDays)));
		assertThat("Datum stored count aggregated", usage.getDatumDaysStored(), equalTo(
				BigInteger.valueOf((1_500_000L + 3_500_000L + 5_500_000L + 7_500_000L) * numDays)));

		// see {@link #tiersForDate_wayBack()}
		Map<String, List<NamedCost>> tiersBreakdown = usage.getTiersCostBreakdown();
		List<NamedCost> propsInTiersCost = tiersBreakdown.get(NodeUsage.DATUM_PROPS_IN_KEY);
		assertThat("Properties in cost tier count", propsInTiersCost, hasSize(4));
		List<NamedCost> datumOutTiersCost = tiersBreakdown.get(NodeUsage.DATUM_OUT_KEY);
		assertThat("Datum out cost tier count", datumOutTiersCost, hasSize(4));
		List<NamedCost> datumStoredTiersCost = tiersBreakdown.get(NodeUsage.DATUM_DAYS_STORED_KEY);
		assertThat("Datum stored cost tier count", datumStoredTiersCost, hasSize(4));

		/*-
		min=0,       prop_in=1500000, tier_prop_in=50000,  cost_prop_in=0.000009, prop_in_cost=0.450000, datum_stored=180000000, tier_datum_stored=50000, cost_datum_stored=4E-7, datum_stored_cost=0.0200000, datum_out=3500000, tier_datum_out=50000, cost_datum_out=0.000002, datum_out_cost=0.100000, total_cost=0.57}
		min=50000,   prop_in=1500000, tier_prop_in=350000, cost_prop_in=0.000006, prop_in_cost=2.100000, datum_stored=180000000, tier_datum_stored=350000, cost_datum_stored=2E-7, datum_stored_cost=0.0700000, datum_out=3500000, tier_datum_out=350000, cost_datum_out=0.000001, datum_out_cost=0.350000, total_cost=2.52
		min=400000,  prop_in=1500000, tier_prop_in=600000, cost_prop_in=0.000004, prop_in_cost=2.400000, datum_stored=180000000, tier_datum_stored=600000, cost_datum_stored=5E-8, datum_stored_cost=0.03000000, datum_out=3500000, tier_datum_out=600000, cost_datum_out=5E-7, datum_out_cost=0.3000000, total_cost=2.73
		min=1000000, prop_in=1500000, tier_prop_in=500000, cost_prop_in=0.000002, prop_in_cost=1.000000, datum_stored=180000000, tier_datum_stored=179000000, cost_datum_stored=6E-9, datum_stored_cost=1.074000000, datum_out=3500000, tier_datum_out=2500000, cost_datum_out=2E-7, datum_out_cost=0.5000000, total_cost=2.57
		*/

		UsageTiers tiers = dao.effectiveUsageTiers(month);
		Map<String, List<UsageTier>> tierMap = tiers.tierMap();

		// @formatter:off
		assertThat("Properties in cost", usage.getDatumPropertiesInCost().setScale(3), equalTo(
						new BigDecimal("50000") .multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost())
				.add(	new BigDecimal("350000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()))
				.add(	new BigDecimal("600000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(2).getCost()))
				.add(	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(3).getCost()))
				.setScale(3)
				));
		assertThat("Properties in cost tiers", propsInTiersCost, contains(
				NamedCost.forTier(1, "50000", 	new BigDecimal("50000") .multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost()).toString()),
				NamedCost.forTier(2, "350000", 	new BigDecimal("350000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()).toString()),
				NamedCost.forTier(3, "600000", 	new BigDecimal("600000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(2).getCost()).toString()),
				NamedCost.forTier(4, "500000", 	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(3).getCost()).toString())
				));

		assertThat("Datum out cost", usage.getDatumOutCost().setScale(3), equalTo(
						new BigDecimal("50000")  .multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost())
				.add(	new BigDecimal("350000") .multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(1).getCost()))
				.add(	new BigDecimal("600000") .multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(2).getCost()))
				.add(	new BigDecimal("2500000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(3).getCost()))
				.setScale(3)
				));
		assertThat("Datum out cost tiers", datumOutTiersCost, contains(
				NamedCost.forTier(1, "50000", 	new BigDecimal("50000")  .multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost()).toString()),
				NamedCost.forTier(2, "350000", 	new BigDecimal("350000") .multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(1).getCost()).toString()),
				NamedCost.forTier(3, "600000", 	new BigDecimal("600000") .multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(2).getCost()).toString()),
				NamedCost.forTier(4, "2500000", new BigDecimal("2500000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(3).getCost()).toString())
				));

		assertThat("Datum stored cost", usage.getDatumDaysStoredCost().setScale(3), equalTo(
						new BigDecimal("50000")    .multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost())
				.add(	new BigDecimal("350000")   .multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()))
				.add(	new BigDecimal("600000")   .multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(2).getCost()))
				.add(	new BigDecimal("179000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(3).getCost()))
				.setScale(3)
				));
		assertThat("Datum stored cost tiers", datumStoredTiersCost, contains(
				NamedCost.forTier(1, "50000", 		new BigDecimal("50000")    .multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost()).toString()),
				NamedCost.forTier(2, "350000", 		new BigDecimal("350000")   .multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()).toString()),
				NamedCost.forTier(3, "600000", 		new BigDecimal("600000")   .multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(2).getCost()).toString()),
				NamedCost.forTier(4, "179000000",	new BigDecimal("179000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(3).getCost()).toString())
				));
		// @formatter:on
	}

	protected void addOcppCharger(Long userId, Long nodeId, String ident, String vendor, String model) {
		OcppTestUtils.saveOcppCharger(jdbcTemplate, userId, nodeId, ident, vendor, model, true);
	}

	protected void addDnp3Server(Long userId, Long serverId) {
		jdbcTemplate.update("""
				INSERT INTO solardnp3.dnp3_server (user_id, id, enabled, cname)
				VALUES (?, ?, TRUE, ?)
				""", userId, serverId, "Server %d".formatted(serverId));
	}

	protected void addDnp3Measurement(Long userId, Long serverId, Integer idx, Long nodeId) {
		jdbcTemplate.update("""
				INSERT INTO solardnp3.dnp3_server_meas
					(user_id, server_id, idx, enabled, node_id, source_id, pname, mtype)
				VALUES (?, ?, ?, TRUE, ?, 'm/1', 'p', 'a')
				""", userId, serverId, idx, nodeId);
	}

	protected void addDnp3Control(Long userId, Long serverId, Integer idx, Long nodeId) {
		jdbcTemplate.update("""
				INSERT INTO solardnp3.dnp3_server_ctrl
					(user_id, server_id, idx, enabled, node_id, control_id, ctype)
				VALUES (?, ?, ?, TRUE, ?, 's/1', 'B')
				""", userId, serverId, idx, nodeId);
	}

	protected void addAuditInstructionsIssuedDaily(Long nodeId, Instant ts, Long count) {
		DatumDbUtils.insertAuditNodeServiceValueDaily(log, jdbcTemplate, Collections
				.singleton(AuditNodeServiceEntity.dailyAuditNodeService(nodeId, "inst", ts, count)));
	}

	@Test
	public void usageForUser_oneNodeOneSource_withOcppOscp() {
		// GIVEN
		final LocalDate month = LocalDate.of(2022, 11, 1);
		final String sourceId = "S1";

		// add 10 days worth of audit data
		final int numDays = 10;
		for ( int dayOffset = 0; dayOffset < numDays; dayOffset++ ) {
			Instant day = month.plusDays(dayOffset).atStartOfDay(TEST_ZONE).toInstant();
			addAuditAccumulatingDatumDaily(nodeId, sourceId, day, 1000000, 2000000, 3000000, 4000000);
			addAuditDatumDaily(nodeId, sourceId, day, 100000, 200000, 2_500_000L, 300000, (short) 400000,
					true);
		}

		debugRows("solardatm.aud_acc_datm_daily", "ts_start");
		debugQuery(format(
				"select * from solarbill.billing_usage_tier_details(%d, '2022-11-01'::timestamp, '2022-12-01'::timestamp, '2022-11-01'::date)",
				userId));

		final int numOcppChargers = 2000;
		for ( int i = 0; i < numOcppChargers; i++ ) {
			addOcppCharger(userId, nodeId, "cp-%d".formatted(i), "ACME", "Test");
		}
		debugQuery("""
				SELECT COUNT(*) FROM solarev.ocpp_charge_point WHERE user_id = %d AND enabled = TRUE
				""".formatted(userId));

		final Long fpId = OscpTestUtils.saveFlexibilityProviderAuthId(jdbcTemplate, userId,
				randomUUID().toString(), false);
		final Long cpId = OscpTestUtils.saveCapacityProvider(jdbcTemplate, userId, fpId, "CP");
		final Long coId = OscpTestUtils.saveCapacityOptimizer(jdbcTemplate, userId, fpId, "CO");
		final int numOscpCapacityGroups = 200;
		for ( int i = 0; i < numOscpCapacityGroups; i++ ) {
			OscpTestUtils.saveCapacityGroup(jdbcTemplate, userId, "CG-%d".formatted(i),
					"CG-%d".formatted(i), cpId, coId);
		}
		debugQuery("""
				SELECT COUNT(*) FROM solaroscp.oscp_cg_conf WHERE user_id = %d AND enabled = TRUE
				""".formatted(userId));

		// WHEN
		UsageTiers tiers = dao.effectiveUsageTiers(month);
		Map<String, List<UsageTier>> tierMap = tiers.tierMap();

		List<NodeUsage> r1 = dao.findNodeUsageForAccount(userId, month, month.plusMonths(1));
		List<NodeUsage> r2 = dao.findUsageForAccount(userId, month, month.plusMonths(1));

		// THEN
		int i = 0;
		for ( List<NodeUsage> results : Arrays.asList(r1, r2) ) {
			assertThat("Results non-null with single result", results, hasSize(1));
			NodeUsage usage = results.get(0);
			if ( i == 0 ) {
				assertThat("Node ID present for node-level usage", usage.getId(), equalTo(nodeId));
				assertThat("Node usage description is node name", usage.getDescription(),
						equalTo(format("Test Node %d", nodeId)));
			} else {
				assertThat("No node ID for account-level usage", usage.getId(), nullValue());
			}
			assertThat("Properties in count aggregated", usage.getDatumPropertiesIn(),
					equalTo(BigInteger.valueOf(100000L * numDays)));
			assertThat("Datum out count aggregated", usage.getDatumOut(),
					equalTo(BigInteger.valueOf(200000L * numDays)));
			assertThat("Datum stored count aggregated", usage.getDatumDaysStored(),
					equalTo(BigInteger.valueOf((1000000L + 2000000L + 3000000L + 4000000L) * numDays)));

			// see tiersForDate_202211
			Map<String, List<NamedCost>> tiersBreakdown = usage.getTiersCostBreakdown();
			List<NamedCost> propsInTiersCost = tiersBreakdown.get(NodeUsage.DATUM_PROPS_IN_KEY);
			assertThat("Properties in cost tier count", propsInTiersCost, hasSize(2));
			List<NamedCost> datumOutTiersCost = tiersBreakdown.get(NodeUsage.DATUM_OUT_KEY);
			assertThat("Datum out cost tier count", datumOutTiersCost, hasSize(1));
			List<NamedCost> datumStoredTiersCost = tiersBreakdown.get(NodeUsage.DATUM_DAYS_STORED_KEY);
			assertThat("Datum stored cost tier count", datumStoredTiersCost, hasSize(2));

			if ( i == 0 ) {
				List<NamedCost> ocppChargersTiersCost = tiersBreakdown.get(NodeUsage.OCPP_CHARGERS_KEY);
				assertThat("No node-level OCPP charger costs", ocppChargersTiersCost, hasSize(0));
				List<NamedCost> oscpCapacityGroupsTiersCost = tiersBreakdown
						.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY);
				assertThat("No node-level OSCP capacity group costs", oscpCapacityGroupsTiersCost,
						hasSize(0));
			} else {
				List<NamedCost> ocppChargersTiersCost = tiersBreakdown.get(NodeUsage.OCPP_CHARGERS_KEY);
				assertThat("Account-level OCPP charger costs", ocppChargersTiersCost, hasSize(2));
				List<NamedCost> oscpCapacityGroupsTiersCost = tiersBreakdown
						.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY);
				assertThat("Account-level OSCP capacity group costs", oscpCapacityGroupsTiersCost,
						hasSize(3));
				/*-
				datum-props-in=[
					NamedCost{name=Tier 1, quantity=500000, cost=2.500000},
					NamedCost{name=Tier 2, quantity=500000, cost=1.500000}],
				datum-out=[
					NamedCost{name=Tier 1, quantity=2000000, cost=0.2000000}],
				datum-days-stored=[
					NamedCost{name=Tier 1, quantity=10000000, cost=0.50000000},
					NamedCost{name=Tier 2, quantity=90000000, cost=0.90000000}],
				ocpp-chargers=[
					NamedCost{name=Tier 1, quantity=250, cost=500},
					NamedCost{name=Tier 2, quantity=1750, cost=1750}],
				oscp-cap-groups=[
					NamedCost{name=Tier 1, quantity=30, cost=1500},
					NamedCost{name=Tier 2, quantity=70, cost=2100},
					NamedCost{name=Tier 3, quantity=100, cost=1500}]
				*/
				// @formatter:off
				assertThat("Properties in cost", usage.getDatumPropertiesInCost().setScale(3), equalTo(
								new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost())
						.add(	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("Properties in cost tiers", propsInTiersCost, contains(
						NamedCost.forTier(1, "500000", 	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "500000", 	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()).toString())));

				assertThat("Datum out cost", usage.getDatumOutCost().setScale(3), equalTo(
								new BigDecimal("2000000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost())
						.setScale(3)
						));
				assertThat("Datum out cost tiers", datumOutTiersCost, contains(
						NamedCost.forTier(1, "2000000", new BigDecimal("2000000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost()).toString())));

				assertThat("Datum stored cost", usage.getDatumDaysStoredCost().setScale(3), equalTo(
								new BigDecimal("10000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost())
						.add(	new BigDecimal("90000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("Datum stored cost tiers", datumStoredTiersCost, contains(
						NamedCost.forTier(1, "10000000", 	new BigDecimal("10000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "90000000", 	new BigDecimal("90000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()).toString())));

				assertThat("OCPP charger cost", usage.getOcppChargersCost().setScale(3), equalTo(
								new BigDecimal("250") .multiply(tierMap.get(NodeUsage.OCPP_CHARGERS_KEY).get(0).getCost())
						.add(	new BigDecimal("1750").multiply(tierMap.get(NodeUsage.OCPP_CHARGERS_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("OCPP Charger cost tiers", ocppChargersTiersCost, contains(
						NamedCost.forTier(1, "250",  new BigDecimal("250") .multiply(tierMap.get(NodeUsage.OCPP_CHARGERS_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "1750", new BigDecimal("1750").multiply(tierMap.get(NodeUsage.OCPP_CHARGERS_KEY).get(1).getCost()).toString())));

				assertThat("OSCP Capacity Groups cost", usage.getOscpCapacityGroupsCost().setScale(3), equalTo(
								new BigDecimal("30") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(0).getCost())
						.add(	new BigDecimal("70") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(1).getCost()))
						.add(	new BigDecimal("100").multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(2).getCost()))
						.setScale(3)
						));
				assertThat("OSCP Capacity Groups cost tiers", oscpCapacityGroupsTiersCost, contains(
						NamedCost.forTier(1, "30",  new BigDecimal("30") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "70",  new BigDecimal("70") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(1).getCost()).toString()),
						NamedCost.forTier(3, "100", new BigDecimal("100").multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(2).getCost()).toString())
						));
				// @formatter:on
			}
			i++;
		}

	}

	@Test
	public void usageForUser_oneNodeOneSource_withOcppOscpDnp3() {
		// GIVEN
		final LocalDate month = LocalDate.of(2023, 11, 1);
		final String sourceId = "S1";

		// add 10 days worth of audit data
		final int numDays = 10;
		for ( int dayOffset = 0; dayOffset < numDays; dayOffset++ ) {
			Instant day = month.plusDays(dayOffset).atStartOfDay(TEST_ZONE).toInstant();
			addAuditAccumulatingDatumDaily(nodeId, sourceId, day, 1000000, 2000000, 3000000, 4000000);
			addAuditDatumDaily(nodeId, sourceId, day, 100000, 200000, 2_500_000L, 300000, (short) 400000,
					true);
		}

		debugRows("solardatm.aud_acc_datm_daily", "ts_start");
		debugQuery(format(
				"select * from solarbill.billing_usage_tier_details(%d, '2023-11-01'::timestamp, '2023-12-01'::timestamp, '2023-11-01'::date)",
				userId));

		final int numOcppChargers = 2000;
		for ( int i = 0; i < numOcppChargers; i++ ) {
			addOcppCharger(userId, nodeId, "cp-%d".formatted(i), "ACME", "Test");
		}
		debugQuery("""
				SELECT COUNT(*) FROM solarev.ocpp_charge_point WHERE user_id = %d AND enabled = TRUE
				""".formatted(userId));

		final Long fpId = OscpTestUtils.saveFlexibilityProviderAuthId(jdbcTemplate, userId,
				randomUUID().toString(), false);
		final Long cpId = OscpTestUtils.saveCapacityProvider(jdbcTemplate, userId, fpId, "CP");
		final Long coId = OscpTestUtils.saveCapacityOptimizer(jdbcTemplate, userId, fpId, "CO");
		final int numOscpCapacityGroups = 200;
		for ( int i = 0; i < numOscpCapacityGroups; i++ ) {
			OscpTestUtils.saveCapacityGroup(jdbcTemplate, userId, "CG-%d".formatted(i),
					"CG-%d".formatted(i), cpId, coId);
		}
		debugQuery("""
				SELECT COUNT(*) AS oscp_cg_count
				FROM solaroscp.oscp_cg_conf WHERE user_id = %d AND enabled = TRUE
				""".formatted(userId));

		final int maxServers = 20;
		final int numDnp3DataPoints = 1000;
		final SecureRandom rng = new SecureRandom();
		int dnp3CountM = 0;
		int dnp3CountC = 0;
		final Set<Long> dnp3ServerIds = new LinkedHashSet<>();
		for ( int i = 0; i < numDnp3DataPoints; i++ ) {
			Long serverId = rng.nextLong(maxServers);
			if ( !dnp3ServerIds.contains(serverId) ) {
				addDnp3Server(userId, serverId);
				dnp3ServerIds.add(serverId);
			}
			if ( rng.nextBoolean() ) {
				addDnp3Measurement(userId, serverId, dnp3CountM++, nodeId);
			} else {
				addDnp3Control(userId, serverId, dnp3CountC++, nodeId);
			}
		}
		debugQuery("""
				SELECT COUNT(*) AS dnp3_meas_count
				FROM solardnp3.dnp3_server_meas WHERE user_id = %d AND enabled = TRUE
				""".formatted(userId));

		debugQuery("""
				SELECT COUNT(*) AS dnp3_ctrl_count
				FROM solardnp3.dnp3_server_ctrl WHERE user_id = %d AND enabled = TRUE
				""".formatted(userId));

		// WHEN
		UsageTiers tiers = dao.effectiveUsageTiers(month);
		Map<String, List<UsageTier>> tierMap = tiers.tierMap();

		List<NodeUsage> r1 = dao.findNodeUsageForAccount(userId, month, month.plusMonths(1));
		List<NodeUsage> r2 = dao.findUsageForAccount(userId, month, month.plusMonths(1));

		// THEN
		int i = 0;
		for ( List<NodeUsage> results : Arrays.asList(r1, r2) ) {
			assertThat("Results non-null with single result", results, hasSize(1));
			NodeUsage usage = results.get(0);
			if ( i == 0 ) {
				assertThat("Node ID present for node-level usage", usage.getId(), equalTo(nodeId));
				assertThat("Node usage description is node name", usage.getDescription(),
						equalTo(format("Test Node %d", nodeId)));
			} else {
				assertThat("No node ID for account-level usage", usage.getId(), nullValue());
			}
			assertThat("Properties in count aggregated", usage.getDatumPropertiesIn(),
					equalTo(BigInteger.valueOf(100000L * numDays)));
			assertThat("Datum out count aggregated", usage.getDatumOut(),
					equalTo(BigInteger.valueOf(200000L * numDays)));
			assertThat("Datum stored count aggregated", usage.getDatumDaysStored(),
					equalTo(BigInteger.valueOf((1000000L + 2000000L + 3000000L + 4000000L) * numDays)));

			// see tiersForDate_202211
			Map<String, List<NamedCost>> tiersBreakdown = usage.getTiersCostBreakdown();
			List<NamedCost> propsInTiersCost = tiersBreakdown.get(NodeUsage.DATUM_PROPS_IN_KEY);
			assertThat("Properties in cost tier count", propsInTiersCost, hasSize(2));
			List<NamedCost> datumOutTiersCost = tiersBreakdown.get(NodeUsage.DATUM_OUT_KEY);
			assertThat("Datum out cost tier count", datumOutTiersCost, hasSize(1));
			List<NamedCost> datumStoredTiersCost = tiersBreakdown.get(NodeUsage.DATUM_DAYS_STORED_KEY);
			assertThat("Datum stored cost tier count", datumStoredTiersCost, hasSize(2));

			if ( i == 0 ) {
				List<NamedCost> ocppChargersTiersCost = tiersBreakdown.get(NodeUsage.OCPP_CHARGERS_KEY);
				assertThat("No node-level OCPP charger costs", ocppChargersTiersCost, hasSize(0));
				List<NamedCost> oscpCapacityGroupsTiersCost = tiersBreakdown
						.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY);
				assertThat("No node-level OSCP capacity group costs", oscpCapacityGroupsTiersCost,
						hasSize(0));
				List<NamedCost> dnp3DataPointsTiersCost = tiersBreakdown
						.get(NodeUsage.DNP3_DATA_POINTS_KEY);
				assertThat("No node-level DNP3 Data Points costs", dnp3DataPointsTiersCost, hasSize(0));
			} else {
				List<NamedCost> ocppChargersTiersCost = tiersBreakdown.get(NodeUsage.OCPP_CHARGERS_KEY);
				assertThat("Account-level OCPP charger costs", ocppChargersTiersCost, hasSize(2));
				List<NamedCost> oscpCapacityGroupsTiersCost = tiersBreakdown
						.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY);
				assertThat("Account-level OSCP capacity group costs", oscpCapacityGroupsTiersCost,
						hasSize(3));
				List<NamedCost> dnp3DataPointsTiersCost = tiersBreakdown
						.get(NodeUsage.DNP3_DATA_POINTS_KEY);
				assertThat("Account-level DNP3 Data Points costs", dnp3DataPointsTiersCost, hasSize(4));
				/*-
				datum-props-in=[
					NamedCost{name=Tier 1, quantity=500000, cost=2.500000},
					NamedCost{name=Tier 2, quantity=500000, cost=1.500000}],
				datum-out=[
					NamedCost{name=Tier 1, quantity=2000000, cost=0.2000000}],
				datum-days-stored=[
					NamedCost{name=Tier 1, quantity=10000000, cost=0.50000000},
					NamedCost{name=Tier 2, quantity=90000000, cost=0.90000000}],
				ocpp-chargers=[
					NamedCost{name=Tier 1, quantity=250, cost=500},
					NamedCost{name=Tier 2, quantity=1750, cost=1750}],
				oscp-cap-groups=[
					NamedCost{name=Tier 1, quantity=30, cost=1500},
					NamedCost{name=Tier 2, quantity=70, cost=2100},
					NamedCost{name=Tier 3, quantity=100, cost=1500}]
				dnp3-data-points=[
					NamedCost{name=Tier 1, quantity=20, cost=20},
					NamedCost{name=Tier 2, quantity=80, cost=48.0},
					NamedCost{name=Tier 3, quantity=400, cost=160.0},
					NamedCost{name=Tier 4, quantity=500, cost=100.0}]
				*/
				// @formatter:off
				assertThat("Properties in cost", usage.getDatumPropertiesInCost().setScale(3), equalTo(
								new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost())
						.add(	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("Properties in cost tiers", propsInTiersCost, contains(
						NamedCost.forTier(1, "500000", 	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "500000", 	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()).toString())));

				assertThat("Datum out cost", usage.getDatumOutCost().setScale(3), equalTo(
								new BigDecimal("2000000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost())
						.setScale(3)
						));
				assertThat("Datum out cost tiers", datumOutTiersCost, contains(
						NamedCost.forTier(1, "2000000", new BigDecimal("2000000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost()).toString())));

				assertThat("Datum stored cost", usage.getDatumDaysStoredCost().setScale(3), equalTo(
								new BigDecimal("10000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost())
						.add(	new BigDecimal("90000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("Datum stored cost tiers", datumStoredTiersCost, contains(
						NamedCost.forTier(1, "10000000", 	new BigDecimal("10000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "90000000", 	new BigDecimal("90000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()).toString())));

				assertThat("OCPP charger cost", usage.getOcppChargersCost().setScale(3), equalTo(
								new BigDecimal("250") .multiply(tierMap.get(NodeUsage.OCPP_CHARGERS_KEY).get(0).getCost())
						.add(	new BigDecimal("1750").multiply(tierMap.get(NodeUsage.OCPP_CHARGERS_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("OCPP Charger cost tiers", ocppChargersTiersCost, contains(
						NamedCost.forTier(1, "250",  new BigDecimal("250") .multiply(tierMap.get(NodeUsage.OCPP_CHARGERS_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "1750", new BigDecimal("1750").multiply(tierMap.get(NodeUsage.OCPP_CHARGERS_KEY).get(1).getCost()).toString())));

				assertThat("OSCP Capacity Groups cost", usage.getOscpCapacityGroupsCost().setScale(3), equalTo(
								new BigDecimal("30") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(0).getCost())
						.add(	new BigDecimal("70") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(1).getCost()))
						.add(	new BigDecimal("100").multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(2).getCost()))
						.setScale(3)
						));
				assertThat("OSCP Capacity Groups cost tiers", oscpCapacityGroupsTiersCost, contains(
						NamedCost.forTier(1, "30",  new BigDecimal("30") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "70",  new BigDecimal("70") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(1).getCost()).toString()),
						NamedCost.forTier(3, "100", new BigDecimal("100").multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(2).getCost()).toString())
						));

				assertThat("DNP3 Data Points cost", usage.getDnp3DataPointsCost().setScale(3), equalTo(
								new BigDecimal("20") .multiply(tierMap.get(NodeUsage.DNP3_DATA_POINTS_KEY).get(0).getCost())
						.add(	new BigDecimal("80") .multiply(tierMap.get(NodeUsage.DNP3_DATA_POINTS_KEY).get(1).getCost()))
						.add(	new BigDecimal("400").multiply(tierMap.get(NodeUsage.DNP3_DATA_POINTS_KEY).get(2).getCost()))
						.add(	new BigDecimal("500").multiply(tierMap.get(NodeUsage.DNP3_DATA_POINTS_KEY).get(3).getCost()))
						.setScale(3)
						));
				assertThat("DNP3 Data Points cost tiers", dnp3DataPointsTiersCost, contains(
						NamedCost.forTier(1, "20",  new BigDecimal("20") .multiply(tierMap.get(NodeUsage.DNP3_DATA_POINTS_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "80",  new BigDecimal("80") .multiply(tierMap.get(NodeUsage.DNP3_DATA_POINTS_KEY).get(1).getCost()).toString()),
						NamedCost.forTier(3, "400", new BigDecimal("400").multiply(tierMap.get(NodeUsage.DNP3_DATA_POINTS_KEY).get(2).getCost()).toString()),
						NamedCost.forTier(4, "500", new BigDecimal("500").multiply(tierMap.get(NodeUsage.DNP3_DATA_POINTS_KEY).get(3).getCost()).toString())
						));
				// @formatter:on
			}
			i++;
		}

	}

	@Test
	public void usageForUser_oneNodeOneSource_withInstructions() {
		// GIVEN
		final LocalDate month = LocalDate.of(2023, 11, 1);
		final String sourceId = "S1";

		// add 10 days worth of audit data
		final int numDays = 10;
		for ( int dayOffset = 0; dayOffset < numDays; dayOffset++ ) {
			Instant day = month.plusDays(dayOffset).atStartOfDay(TEST_ZONE).toInstant();
			addAuditAccumulatingDatumDaily(nodeId, sourceId, day, 1_000_000, 2_000_000, 3_000_000,
					4_000_000);
			addAuditDatumDaily(nodeId, sourceId, day, 100_000, 200_000, 2_500_000L, 300_000,
					(short) 400_000, true);
			addAuditInstructionsIssuedDaily(nodeId, day, 100_000L);
		}

		debugRows("solardatm.aud_acc_datm_daily", "ts_start");
		debugRows("solardatm.aud_node_daily", "ts_start");
		debugQuery(format(
				"select * from solarbill.billing_usage_tier_details(%d, '%s'::timestamp, '%s'::timestamp, '%s'::date)",
				userId, month.toString(), month.plusMonths(1).toString(), month.toString()));

		// WHEN
		UsageTiers tiers = dao.effectiveUsageTiers(month);
		Map<String, List<UsageTier>> tierMap = tiers.tierMap();

		List<NodeUsage> r1 = dao.findNodeUsageForAccount(userId, month, month.plusMonths(1));
		List<NodeUsage> r2 = dao.findUsageForAccount(userId, month, month.plusMonths(1));

		// THEN
		int i = 0;
		for ( List<NodeUsage> results : Arrays.asList(r1, r2) ) {
			assertThat("Results non-null with single result", results, hasSize(1));
			NodeUsage usage = results.get(0);
			if ( i == 0 ) {
				assertThat("Node ID present for node-level usage", usage.getId(), equalTo(nodeId));
				assertThat("Node usage description is node name", usage.getDescription(),
						equalTo(format("Test Node %d", nodeId)));
			} else {
				assertThat("No node ID for account-level usage", usage.getId(), nullValue());
			}
			assertThat("Properties in count aggregated", usage.getDatumPropertiesIn(),
					equalTo(BigInteger.valueOf(100_000L * numDays)));
			assertThat("Datum out count aggregated", usage.getDatumOut(),
					equalTo(BigInteger.valueOf(200_000L * numDays)));
			assertThat("Datum stored count aggregated", usage.getDatumDaysStored(), equalTo(
					BigInteger.valueOf((1_000_000L + 2_000_000L + 3_000_000L + 4_000_000L) * numDays)));
			assertThat("Instructions issued count aggregated", usage.getInstructionsIssued(),
					equalTo(BigInteger.valueOf(100_000L * numDays)));

			// see tiersForDate_202311
			Map<String, List<NamedCost>> tiersBreakdown = usage.getTiersCostBreakdown();
			List<NamedCost> propsInTiersCost = tiersBreakdown.get(NodeUsage.DATUM_PROPS_IN_KEY);
			assertThat("Properties in cost tier count", propsInTiersCost, hasSize(2));
			List<NamedCost> datumOutTiersCost = tiersBreakdown.get(NodeUsage.DATUM_OUT_KEY);
			assertThat("Datum out cost tier count", datumOutTiersCost, hasSize(1));
			List<NamedCost> datumStoredTiersCost = tiersBreakdown.get(NodeUsage.DATUM_DAYS_STORED_KEY);
			assertThat("Datum stored cost tier count", datumStoredTiersCost, hasSize(2));
			List<NamedCost> instructionsIssuedTiersCost = tiersBreakdown
					.get(NodeUsage.INSTRUCTIONS_ISSUED_KEY);
			assertThat("Instructions issued cost tier count", instructionsIssuedTiersCost, hasSize(3));

			if ( i == 0 ) {
				List<NamedCost> ocppChargersTiersCost = tiersBreakdown.get(NodeUsage.OCPP_CHARGERS_KEY);
				assertThat("No node-level OCPP charger costs", ocppChargersTiersCost, hasSize(0));
				List<NamedCost> oscpCapacityGroupsTiersCost = tiersBreakdown
						.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY);
				assertThat("No node-level OSCP capacity group costs", oscpCapacityGroupsTiersCost,
						hasSize(0));
			} else {
				List<NamedCost> ocppChargersTiersCost = tiersBreakdown.get(NodeUsage.OCPP_CHARGERS_KEY);
				assertThat("Account-level OCPP charger costs", ocppChargersTiersCost, hasSize(0));
				List<NamedCost> oscpCapacityGroupsTiersCost = tiersBreakdown
						.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY);
				assertThat("Account-level OSCP capacity group costs", oscpCapacityGroupsTiersCost,
						hasSize(0));
				/*-
				datum-props-in=[
					NamedCost{name=Tier 1, quantity=500000, cost=2.500000},
					NamedCost{name=Tier 2, quantity=500000, cost=1.500000}],
				datum-out=[
					NamedCost{name=Tier 1, quantity=2000000, cost=0.2000000}],
				datum-days-stored=[
					NamedCost{name=Tier 1, quantity=10000000, cost=0.50000000},
					NamedCost{name=Tier 2, quantity=90000000, cost=0.90000000}],
				instr-issued=[
					NamedCost{name=Tier 1, quantity=10000, cost=1.0000},
					NamedCost{name=Tier 2, quantity=90000, cost=4.50000},
					NamedCost{name=Tier 3, quantity=900000, cost=18.00000}]
				ocpp-chargers=[
					NamedCost{name=Tier 1, quantity=250, cost=500},
					NamedCost{name=Tier 2, quantity=1750, cost=1750}],
				oscp-cap-groups=[
					NamedCost{name=Tier 1, quantity=30, cost=1500},
					NamedCost{name=Tier 2, quantity=70, cost=2100},
					NamedCost{name=Tier 3, quantity=100, cost=1500}]
				*/
				// @formatter:off
				assertThat("Properties in cost", usage.getDatumPropertiesInCost().setScale(3), equalTo(
								new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost())
						.add(	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("Properties in cost tiers", propsInTiersCost, contains(
						NamedCost.forTier(1, "500000", 	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "500000", 	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()).toString())));

				assertThat("Datum out cost", usage.getDatumOutCost().setScale(3), equalTo(
								new BigDecimal("2000000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost())
						.setScale(3)
						));
				assertThat("Datum out cost tiers", datumOutTiersCost, contains(
						NamedCost.forTier(1, "2000000", new BigDecimal("2000000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost()).toString())));

				assertThat("Datum stored cost", usage.getDatumDaysStoredCost().setScale(3), equalTo(
								new BigDecimal("10000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost())
						.add(	new BigDecimal("90000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("Datum stored cost tiers", datumStoredTiersCost, contains(
						NamedCost.forTier(1, "10000000", 	new BigDecimal("10000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "90000000", 	new BigDecimal("90000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()).toString())));

				assertThat("Instructions issued cost", usage.getInstructionsIssuedCost().setScale(3), equalTo(
								        new BigDecimal("10000").multiply(tierMap.get(NodeUsage.INSTRUCTIONS_ISSUED_KEY).get(0).getCost())
								.add(	new BigDecimal("90000").multiply(tierMap.get(NodeUsage.INSTRUCTIONS_ISSUED_KEY).get(1).getCost()))
								.add(	new BigDecimal("900000").multiply(tierMap.get(NodeUsage.INSTRUCTIONS_ISSUED_KEY).get(2).getCost()))
						.setScale(3)
						));
				assertThat("Instructions issued cost tiers", instructionsIssuedTiersCost, contains(
						NamedCost.forTier(1, "10000", 	new BigDecimal("10000").multiply(tierMap.get(NodeUsage.INSTRUCTIONS_ISSUED_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "90000", 	new BigDecimal("90000").multiply(tierMap.get(NodeUsage.INSTRUCTIONS_ISSUED_KEY).get(1).getCost()).toString()),
						NamedCost.forTier(3, "900000", 	new BigDecimal("900000").multiply(tierMap.get(NodeUsage.INSTRUCTIONS_ISSUED_KEY).get(2).getCost()).toString())));

				// @formatter:on
			}
			i++;
		}
	}

	@Test
	public void usageForUser_oneNodeOneSource_withOscpCap() {
		// GIVEN
		final LocalDate month = LocalDate.of(2024, 4, 1);
		final String sourceId = "S1";
		setupDatumStream(nodeId, sourceId);

		// add 10 days worth of audit data
		final int numDays = 10;
		for ( int dayOffset = 0; dayOffset < numDays; dayOffset++ ) {
			Instant day = month.plusDays(dayOffset).atStartOfDay(TEST_ZONE).toInstant();
			addAuditAccumulatingDatumDaily(nodeId, sourceId, day, 1000000, 2000000, 3000000, 4000000);
			addAuditDatumDaily(nodeId, sourceId, day, 100000, 200000, 2_500_000L, 300000, (short) 400000,
					true);
		}

		final Long fpId = OscpTestUtils.saveFlexibilityProviderAuthId(jdbcTemplate, userId,
				randomUUID().toString(), false);
		final Long cpId = OscpTestUtils.saveCapacityProvider(jdbcTemplate, userId, fpId, "CP");
		final Long coId = OscpTestUtils.saveCapacityOptimizer(jdbcTemplate, userId, fpId, "CO");
		final int numOscpCapacityGroups = 150;
		final int numOscpFlexibilityAssets = 1;
		final String[] iProps = new String[] { "watts" };
		final String[] eProps = new String[] { "wattHours" };
		final Instant startDay = month.atStartOfDay(ZoneOffset.UTC).toInstant();
		for ( int i = 0; i < numOscpCapacityGroups; i++ ) {
			Long cgId = OscpTestUtils.saveCapacityGroup(jdbcTemplate, userId, "CG-%d".formatted(i),
					"CG-%d".formatted(i), cpId, coId);
			for ( int j = 0; j < numOscpFlexibilityAssets; j++ ) {
				String faIdent = "CG-%d Asset-%d".formatted(i, j);

				UUID streamId = setupDatumStream(nodeId, faIdent, iProps, eProps);

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
					addAggregateDatumDaily(streamId, startDay.plus(t, ChronoUnit.DAYS), iData, iStats,
							aData, aStats);
					energy = end;
				}

				OscpTestUtils.saveFlexibilityAsset(jdbcTemplate, userId, faIdent, faIdent, cgId, nodeId,
						faIdent, iProps, eProps);
			}
		}

		debugRows("solardatm.aud_acc_datm_daily", "ts_start");
		debugRows("solardatm.agg_datm_daily", "stream_id,ts_start");
		debugQuery(format(
				"select * from solarbill.billing_usage_tier_details(%d, '2024-04-01'::timestamp, '2024-05-01'::timestamp, '2024-04-01'::date)",
				userId));

		debugQuery("""
				SELECT COUNT(*) AS oscp_cg_count
				FROM solaroscp.oscp_cg_conf WHERE user_id = %d AND enabled = TRUE
				""".formatted(userId));

		// WHEN
		UsageTiers tiers = dao.effectiveUsageTiers(month);
		Map<String, List<UsageTier>> tierMap = tiers.tierMap();

		List<NodeUsage> r1 = dao.findNodeUsageForAccount(userId, month, month.plusMonths(1));
		List<NodeUsage> r2 = dao.findUsageForAccount(userId, month, month.plusMonths(1));

		// THEN
		int i = 0;
		for ( List<NodeUsage> results : Arrays.asList(r1, r2) ) {
			assertThat("Results non-null with single result", results, hasSize(1));
			NodeUsage usage = results.get(0);
			if ( i == 0 ) {
				assertThat("Node ID present for node-level usage", usage.getId(), equalTo(nodeId));
				assertThat("Node usage description is node name", usage.getDescription(),
						equalTo(format("Test Node %d", nodeId)));
			} else {
				assertThat("No node ID for account-level usage", usage.getId(), nullValue());
			}
			assertThat("Properties in count aggregated", usage.getDatumPropertiesIn(),
					equalTo(BigInteger.valueOf(100000L * numDays)));
			assertThat("Datum out count aggregated", usage.getDatumOut(),
					equalTo(BigInteger.valueOf(200000L * numDays)));
			assertThat("Datum stored count aggregated", usage.getDatumDaysStored(),
					equalTo(BigInteger.valueOf((1000000L + 2000000L + 3000000L + 4000000L) * numDays)));

			// see tiersForDate_202211
			Map<String, List<NamedCost>> tiersBreakdown = usage.getTiersCostBreakdown();
			List<NamedCost> propsInTiersCost = tiersBreakdown.get(NodeUsage.DATUM_PROPS_IN_KEY);
			assertThat("Properties in cost tier count", propsInTiersCost, hasSize(2));
			List<NamedCost> datumOutTiersCost = tiersBreakdown.get(NodeUsage.DATUM_OUT_KEY);
			assertThat("Datum out cost tier count", datumOutTiersCost, hasSize(1));
			List<NamedCost> datumStoredTiersCost = tiersBreakdown.get(NodeUsage.DATUM_DAYS_STORED_KEY);
			assertThat("Datum stored cost tier count", datumStoredTiersCost, hasSize(2));

			if ( i == 0 ) {
				List<NamedCost> ocppChargersTiersCost = tiersBreakdown.get(NodeUsage.OCPP_CHARGERS_KEY);
				assertThat("No node-level OCPP charger costs", ocppChargersTiersCost, hasSize(0));
				List<NamedCost> oscpCapacityGroupsTiersCost = tiersBreakdown
						.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY);
				assertThat("No node-level OSCP capacity group costs", oscpCapacityGroupsTiersCost,
						hasSize(0));
				List<NamedCost> dnp3DataPointsTiersCost = tiersBreakdown
						.get(NodeUsage.DNP3_DATA_POINTS_KEY);
				assertThat("No node-level DNP3 Data Points costs", dnp3DataPointsTiersCost, hasSize(0));
			} else {
				List<NamedCost> ocppChargersTiersCost = tiersBreakdown.get(NodeUsage.OCPP_CHARGERS_KEY);
				assertThat("No account-level OCPP charger costs", ocppChargersTiersCost, hasSize(0));
				List<NamedCost> oscpCapacityGroupsTiersCost = tiersBreakdown
						.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY);
				assertThat("Account-level OSCP capacity group costs", oscpCapacityGroupsTiersCost,
						hasSize(2));
				List<NamedCost> oscpCapacityTiersCost = tiersBreakdown.get(NodeUsage.OSCP_CAPACITY_KEY);
				assertThat("Account-level OSCP capacity costs", oscpCapacityTiersCost, hasSize(4));
				List<NamedCost> dnp3DataPointsTiersCost = tiersBreakdown
						.get(NodeUsage.DNP3_DATA_POINTS_KEY);
				assertThat("No account-level DNP3 Data Points costs", dnp3DataPointsTiersCost,
						hasSize(0));
				/*-
				datum-props-in=[
					NamedCost{name=Tier 1, quantity=500000, cost=2.500000},
					NamedCost{name=Tier 2, quantity=500000, cost=1.500000}],
				datum-out=[
					NamedCost{name=Tier 1, quantity=2000000, cost=0.2000000}],
				datum-days-stored=[
					NamedCost{name=Tier 1, quantity=10000000, cost=0.50000000},
					NamedCost{name=Tier 2, quantity=90000000, cost=0.90000000}],
				ocpp-chargers=[
					NamedCost{name=Tier 1, quantity=250, cost=500},
					NamedCost{name=Tier 2, quantity=1750, cost=1750}],
				oscp-cap-groups=[
					NamedCost{name=Tier 1, quantity=100, cost=200},
					NamedCost{name=Tier 2, quantity=50, cost=75.0}]
				oscp-cap=[
					NamedCost{name=Tier 1, quantity=6000000, cost=180.000000}
					NamedCost{name=Tier 2, quantity=34000000, cost=850.00000},
					NamedCost{name=Tier 3, quantity=60000000, cost=1050.000000},
					NamedCost{name=Tier 4, quantity=1400000000, cost=14000.00000}]
				*/
				// @formatter:off
				assertThat("Properties in cost", usage.getDatumPropertiesInCost().setScale(3), equalTo(
								new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost())
						.add(	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("Properties in cost tiers", propsInTiersCost, contains(
						NamedCost.forTier(1, "500000", 	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "500000", 	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()).toString())));

				assertThat("Datum out cost", usage.getDatumOutCost().setScale(3), equalTo(
								new BigDecimal("2000000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost())
						.setScale(3)
						));
				assertThat("Datum out cost tiers", datumOutTiersCost, contains(
						NamedCost.forTier(1, "2000000", new BigDecimal("2000000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost()).toString())));

				assertThat("Datum stored cost", usage.getDatumDaysStoredCost().setScale(3), equalTo(
								new BigDecimal("10000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost())
						.add(	new BigDecimal("90000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("Datum stored cost tiers", datumStoredTiersCost, contains(
						NamedCost.forTier(1, "10000000", 	new BigDecimal("10000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "90000000", 	new BigDecimal("90000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()).toString())));

				assertThat("OSCP Capacity Groups cost", usage.getOscpCapacityGroupsCost().setScale(3), equalTo(
								new BigDecimal("100") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(0).getCost())
						.add(	new BigDecimal("50") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("OSCP Capacity Groups cost tiers", oscpCapacityGroupsTiersCost, contains(
						NamedCost.forTier(1, "100",  new BigDecimal("100") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "50",  new BigDecimal("50") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY).get(1).getCost()).toString())
						));

				assertThat("OSCP Capacity cost", usage.getOscpCapacityCost().setScale(3), equalTo(
								new BigDecimal("6000000") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_KEY).get(0).getCost())
						.add(	new BigDecimal("34000000") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_KEY).get(1).getCost()))
						.add(	new BigDecimal("60000000").multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_KEY).get(2).getCost()))
						.add(	new BigDecimal("1400000000").multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_KEY).get(3).getCost()))
						.setScale(3)
						));
				assertThat("OSCP Capacity cost tiers", oscpCapacityTiersCost, contains(
						NamedCost.forTier(1, "6000000",  new BigDecimal("6000000") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "34000000",  new BigDecimal("34000000") .multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_KEY).get(1).getCost()).toString()),
						NamedCost.forTier(3, "60000000", new BigDecimal("60000000").multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_KEY).get(2).getCost()).toString()),
						NamedCost.forTier(4, "1400000000", new BigDecimal("1400000000").multiply(tierMap.get(NodeUsage.OSCP_CAPACITY_KEY).get(3).getCost()).toString())
						));

				// @formatter:on
			}
			i++;
		}

	}

	@Test
	public void usageForUser_oneNodeOneSource_withFluxDataIn() {
		// GIVEN
		final LocalDate month = LocalDate.of(2024, 11, 1);
		final String sourceId = "S1";
		setupDatumStream(nodeId, sourceId);

		// add 10 days worth of audit data
		final int numDays = 10;
		for ( int dayOffset = 0; dayOffset < numDays; dayOffset++ ) {
			Instant day = month.plusDays(dayOffset).atStartOfDay(TEST_ZONE).toInstant();
			addAuditAccumulatingDatumDaily(nodeId, sourceId, day, 1_000_000, 2_000_000, 3_000_000,
					4_000_000);
			addAuditDatumDaily(nodeId, sourceId, day, 100_000L, 200_000L, 250_000_000L, 300_000,
					(short) 400_000, true);
		}

		debugRows("solardatm.aud_acc_datm_daily", "ts_start");
		debugRows("solardatm.aud_datm_monthly", "ts_start");
		debugQuery(format(
				"select * from solarbill.billing_usage_tier_details(%d, '%s'::timestamp, '%s'::timestamp, '%s'::date)",
				userId, month.toString(), month.plusMonths(1).toString(), month.toString()));

		// WHEN
		UsageTiers tiers = dao.effectiveUsageTiers(month);
		Map<String, List<UsageTier>> tierMap = tiers.tierMap();

		List<NodeUsage> r1 = dao.findNodeUsageForAccount(userId, month, month.plusMonths(1));
		List<NodeUsage> r2 = dao.findUsageForAccount(userId, month, month.plusMonths(1));

		// THEN
		int i = 0;
		for ( List<NodeUsage> results : Arrays.asList(r1, r2) ) {
			assertThat("Results non-null with single result", results, hasSize(1));
			NodeUsage usage = results.get(0);
			if ( i == 0 ) {
				assertThat("Node ID present for node-level usage", usage.getId(), equalTo(nodeId));
				assertThat("Node usage description is node name", usage.getDescription(),
						equalTo(format("Test Node %d", nodeId)));
			} else {
				assertThat("No node ID for account-level usage", usage.getId(), nullValue());
			}
			assertThat("Properties in count aggregated", usage.getDatumPropertiesIn(),
					equalTo(BigInteger.valueOf(100_000L * numDays)));
			assertThat("Datum out count aggregated", usage.getDatumOut(),
					equalTo(BigInteger.valueOf(200_000L * numDays)));
			assertThat("Datum stored count aggregated", usage.getDatumDaysStored(), equalTo(
					BigInteger.valueOf((1_000_000L + 2_000_000L + 3_000_000L + 4_000_000L) * numDays)));
			assertThat("Flux in count aggregated", usage.getFluxDataIn(),
					equalTo(BigInteger.valueOf(2_500_000_000L)));

			// see tiersForDate_202411
			Map<String, List<NamedCost>> tiersBreakdown = usage.getTiersCostBreakdown();
			List<NamedCost> propsInTiersCost = tiersBreakdown.get(NodeUsage.DATUM_PROPS_IN_KEY);
			assertThat("Properties in cost tier count", propsInTiersCost, hasSize(2));
			List<NamedCost> datumOutTiersCost = tiersBreakdown.get(NodeUsage.DATUM_OUT_KEY);
			assertThat("Datum out cost tier count", datumOutTiersCost, hasSize(1));
			List<NamedCost> datumStoredTiersCost = tiersBreakdown.get(NodeUsage.DATUM_DAYS_STORED_KEY);
			assertThat("Datum stored cost tier count", datumStoredTiersCost, hasSize(2));
			List<NamedCost> fluxDataInTiersCost = tiersBreakdown.get(NodeUsage.FLUX_DATA_IN_KEY);
			assertThat("Flux data in cost tier count", fluxDataInTiersCost, hasSize(2));

			if ( i == 0 ) {
				List<NamedCost> ocppChargersTiersCost = tiersBreakdown.get(NodeUsage.OCPP_CHARGERS_KEY);
				assertThat("No node-level OCPP charger costs", ocppChargersTiersCost, hasSize(0));
				List<NamedCost> oscpCapacityGroupsTiersCost = tiersBreakdown
						.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY);
				assertThat("No node-level OSCP capacity group costs", oscpCapacityGroupsTiersCost,
						hasSize(0));
			} else {
				List<NamedCost> ocppChargersTiersCost = tiersBreakdown.get(NodeUsage.OCPP_CHARGERS_KEY);
				assertThat("Account-level OCPP charger costs", ocppChargersTiersCost, hasSize(0));
				List<NamedCost> oscpCapacityGroupsTiersCost = tiersBreakdown
						.get(NodeUsage.OSCP_CAPACITY_GROUPS_KEY);
				assertThat("Account-level OSCP capacity group costs", oscpCapacityGroupsTiersCost,
						hasSize(0));
				/*-
				datum-props-in=[
					NamedCost{name=Tier 1, quantity=500000, cost=2.500000},
					NamedCost{name=Tier 2, quantity=500000, cost=1.500000}],
				datum-out=[
					NamedCost{name=Tier 1, quantity=2000000, cost=0.2000000}],
				datum-days-stored=[
					NamedCost{name=Tier 1, quantity=10000000, cost=0.50000000},
					NamedCost{name=Tier 2, quantity=90000000, cost=0.90000000}],
				flux-data-in=[
					NamedCost{name=Tier 1, quantity=1000000000, cost=10.00000000},
					NamedCost{name=Tier 2, quantity=1500000000, cost=9.000000000}]
				*/
				// @formatter:off
				assertThat("Properties in cost", usage.getDatumPropertiesInCost().setScale(3), equalTo(
								new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost())
						.add(	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("Properties in cost tiers", propsInTiersCost, contains(
						NamedCost.forTier(1, "500000", 	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "500000", 	new BigDecimal("500000").multiply(tierMap.get(NodeUsage.DATUM_PROPS_IN_KEY).get(1).getCost()).toString())));

				assertThat("Datum out cost", usage.getDatumOutCost().setScale(3), equalTo(
								new BigDecimal("2000000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost())
						.setScale(3)
						));
				assertThat("Datum out cost tiers", datumOutTiersCost, contains(
						NamedCost.forTier(1, "2000000", new BigDecimal("2000000").multiply(tierMap.get(NodeUsage.DATUM_OUT_KEY).get(0).getCost()).toString())));

				assertThat("Datum stored cost", usage.getDatumDaysStoredCost().setScale(3), equalTo(
								new BigDecimal("10000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost())
						.add(	new BigDecimal("90000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("Datum stored cost tiers", datumStoredTiersCost, contains(
						NamedCost.forTier(1, "10000000", 	new BigDecimal("10000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "90000000", 	new BigDecimal("90000000").multiply(tierMap.get(NodeUsage.DATUM_DAYS_STORED_KEY).get(1).getCost()).toString())));

				assertThat("Flux data in cost", usage.getFluxDataInCost().setScale(3), equalTo(
								        new BigDecimal("1000000000").multiply(tierMap.get(NodeUsage.FLUX_DATA_IN_KEY).get(0).getCost())
								.add(	new BigDecimal("1500000000").multiply(tierMap.get(NodeUsage.FLUX_DATA_IN_KEY).get(1).getCost()))
						.setScale(3)
						));
				assertThat("Flux data in cost tiers", fluxDataInTiersCost, contains(
						NamedCost.forTier(1, "1000000000", 	new BigDecimal("1000000000").multiply(tierMap.get(NodeUsage.FLUX_DATA_IN_KEY).get(0).getCost()).toString()),
						NamedCost.forTier(2, "1500000000", 	new BigDecimal("1500000000").multiply(tierMap.get(NodeUsage.FLUX_DATA_IN_KEY).get(1).getCost()).toString())));
				// @formatter:on
			}
			i++;
		}

	}

	@Test
	public void usageForUser_withCloudIntegrationsData() {
		// GIVEN
		final LocalDate month = LocalDate.of(2025, 2, 1);

		// add 10 days worth of audit data
		final int numDays = 10;

		final var auditUserServiceDaily = new ArrayList<AuditUserServiceValue>(numDays);

		for ( int dayOffset = 0; dayOffset < numDays; dayOffset++ ) {
			Instant day = month.plusDays(dayOffset).atStartOfDay(TEST_ZONE).toInstant();
			// populate Cloud Integrations data audit records
			auditUserServiceDaily.add(dailyAuditUserService(userId, "ccio", day, 655_250_000L));
		}
		DatumDbUtils.insertAuditUserServiceValueDaily(log, jdbcTemplate, auditUserServiceDaily);

		allTableData(log, jdbcTemplate, "solardatm.aud_user_daily", "user_id,service,ts_start");
		debugQuery(format(
				"select * from solarbill.billing_usage_tier_details(%d, '%s'::timestamp, '%s'::timestamp, '%s'::date)",
				userId, month.toString(), month.plusMonths(1).toString(), month.toString()));

		// WHEN
		UsageTiers tiers = dao.effectiveUsageTiers(month);
		Map<String, List<UsageTier>> tierMap = tiers.tierMap();

		List<NodeUsage> r1 = dao.findNodeUsageForAccount(userId, month, month.plusMonths(1));
		List<NodeUsage> r2 = dao.findUsageForAccount(userId, month, month.plusMonths(1));

		// THEN
		// @formatter:off
		then(r1).as("No node-level usage for account").isEmpty();

		then(r2)
			.as("One account-level usage")
			.hasSize(1)
			.element(0)
			.as("No node ID for account-level usage")
			.returns(null, from(NodeUsage::getId))
			.as("No datum properties in")
			.returns(BigInteger.ZERO, from(NodeUsage::getDatumPropertiesIn))
			.as("No datum out")
			.returns(BigInteger.ZERO, from(NodeUsage::getDatumOut))
			.as("No datum days stored")
			.returns(BigInteger.ZERO, from(NodeUsage::getDatumDaysStored))
			.as("No instructions issued")
			.returns(BigInteger.ZERO, from(NodeUsage::getInstructionsIssued))
			.as("No flux data out")
			.returns(BigInteger.ZERO, from(NodeUsage::getFluxDataOut))
			.as("No OSCP capacity groups")
			.returns(BigInteger.ZERO, from(NodeUsage::getOscpCapacityGroups))
			.as("No OSCP capacity")
			.returns(BigInteger.ZERO, from(NodeUsage::getOscpCapacity))
			.as("No OAuth client credentials")
			.returns(BigInteger.ZERO, from(NodeUsage::getOauthClientCredentials))
			.as("Cloud Integrations data aggregated across month")
			.returns(BigInteger.valueOf(655_250_000L * numDays), from(NodeUsage::getCloudIntegrationsData))
			.satisfies(u -> {
				then(u.getTiersCostBreakdown())
					.as("Has all costs")
					.hasSize(12)
					.as("Has cloud integrations costs")
					.hasEntrySatisfying(CLOUD_INTEGRATIONS_DATA_KEY, tiersCost -> {
						then(tiersCost)
							.contains(
								  forTier(1, "1000000000", new BigDecimal("1000000000").multiply(
										  tierMap.get(CLOUD_INTEGRATIONS_DATA_KEY).get(0).getCost()).toString())
								, forTier(2, "5552500000", new BigDecimal("5552500000").multiply(
										  tierMap.get(CLOUD_INTEGRATIONS_DATA_KEY).get(1).getCost()).toString())
							)
							;
					})
					.satisfies(tiersCost -> {
						// verify all OTHER costs are empty
						var nonCloudIntegrationsTiers = tiersCost.entrySet().stream()
							.filter(e -> !CLOUD_INTEGRATIONS_DATA_KEY.equals(e.getKey()))
							.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))
							;
						then(nonCloudIntegrationsTiers)
							.allSatisfy((k, v) -> {
								then(v)
									.as("No costs for %s (only Cloud Integrations data)", k)
									.isEmpty()
									;
							})
							;
					})
					;
			})
			;
	}

}
