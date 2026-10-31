/* ==================================================================
 * JdbcDatumEntityDao_StreamAliasTests.java - 1/11/2026 9:12:47 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static net.solarnetwork.central.datum.v2.domain.ObjectDatumId.nodeId;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumAuxiliaryEntityDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcObjectDatumStreamAliasGrantEntityDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.dao.BasicBulkExportOptions;
import net.solarnetwork.dao.BulkExportingDao.ExportCallback;
import net.solarnetwork.dao.BulkExportingDao.ExportCallbackAction;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test that statements that change or report on the datum of a stream never act
 * on the original stream of an alias, when stream aliases are included in the
 * criteria.
 *
 * <p>
 * Those statements key on the stream ID, and an alias normally has an ID of its
 * own that matches no datum. The {@code da_datm_alias_stream_id_checker}
 * trigger keeps it that way, so these tests bypass the trigger to create an
 * alias with the ID of the original stream, owned by a different user, to show
 * that the statements protect the stream without it.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_StreamAliasTests extends BaseDatumJdbcTestSupport {

	private static final Long HOST_USER = -9001L;
	private static final Long OWNER_USER = -9002L;
	private static final String ORIG_SOURCE = "s/orig";
	private static final String ALIAS_SOURCE = "s/alias";
	private static final String CRAFTED_SOURCE = "s/crafted";
	private static final LocalDateTime DATUM_DATE = LocalDateTime.of(2026, 1, 1, 10, 0);

	private JdbcDatumEntityDao dao;
	private JdbcDatumAuxiliaryEntityDao auxDao;
	private Long hostNodeId;
	private Long ownerNodeId;
	private ObjectDatumStreamMetadata orig;
	private ObjectDatumStreamAliasEntity alias;
	private Instant datumTime;

	@BeforeEach
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
		auxDao = new JdbcDatumAuxiliaryEntityDao(jdbcTemplate);
		setupTestLocation();

		setupTestUser(HOST_USER, "host@localhost");
		setupTestUser(OWNER_USER, "owner@localhost");
		hostNodeId = ownedNode(HOST_USER);
		jdbcTemplate.update("UPDATE solaruser.user_node SET private = TRUE WHERE node_id = ?",
				hostNodeId);
		ownerNodeId = ownedNode(OWNER_USER);

		orig = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ, Node, ownerNodeId,
				ORIG_SOURCE, new String[] { "x" }, new String[] { "w" }, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(orig));

		final var grant = new ObjectDatumStreamAliasGrantEntity(OWNER_USER, 0L, Instant.now(),
				HOST_USER);
		grant.setEnabled(true);
		grant.setNodeIds(new Long[] { ownerNodeId });
		new JdbcObjectDatumStreamAliasGrantEntityDao(jdbcTemplate).create(OWNER_USER, grant);

		final Instant now = Instant.now();
		alias = new ObjectDatumStreamAliasEntity(UUID.randomUUID(), now, now, Node, hostNodeId,
				ALIAS_SOURCE, ownerNodeId, ORIG_SOURCE);
		DatumDbUtils.insertObjectDatumStreamAliases(log, jdbcTemplate, List.of(alias));

		jdbcTemplate.execute(
				"ALTER TABLE solardatm.da_datm_alias DISABLE TRIGGER da_datm_alias_stream_id_checker");
		DatumDbUtils.insertObjectDatumStreamAliases(log, jdbcTemplate,
				List.of(new ObjectDatumStreamAliasEntity(orig.getStreamId(), now, now, Node, hostNodeId,
						CRAFTED_SOURCE, ownerNodeId, ORIG_SOURCE)));
		jdbcTemplate.execute(
				"ALTER TABLE solardatm.da_datm_alias ENABLE TRIGGER da_datm_alias_stream_id_checker");

		datumTime = DATUM_DATE.atZone(ZoneId.of(TEST_TZ)).toInstant();
		DatumDbUtils.insertDatum(log, jdbcTemplate,
				List.of(new DatumEntity(orig.getStreamId(), datumTime, now,
						propertiesOf(new BigDecimal[] { BigDecimal.ONE },
								new BigDecimal[] { BigDecimal.TEN }, null, null))));
		DatumDbUtils.insertDatumAuxiliary(log, jdbcTemplate,
				List.of(new DatumAuxiliaryEntity(orig.getStreamId(), datumTime.plusSeconds(60),
						DatumAuxiliaryType.Reset, now, null, null, null, null)));
	}

	private Long ownedNode(Long userId) {
		final Long nodeId = randomLong();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);
		return nodeId;
	}

	private BasicDatumCriteria dateRange(BasicDatumCriteria filter) {
		filter.setStartDate(datumTime.minusSeconds(3600));
		filter.setEndDate(datumTime.plusSeconds(3600));
		return filter;
	}

	private BasicDatumCriteria localDateRange(BasicDatumCriteria filter) {
		filter.setLocalStartDate(DATUM_DATE.minusHours(1));
		filter.setLocalEndDate(DATUM_DATE.plusHours(1));
		return filter;
	}

	private BasicDatumCriteria aliasFilter() {
		final var filter = new BasicDatumCriteria();
		filter.setUserId(HOST_USER);
		filter.setNodeId(hostNodeId);
		filter.setSourceId(ALIAS_SOURCE);
		filter.setIncludeStreamAliases(true);
		return filter;
	}

	private BasicDatumCriteria craftedFilter() {
		final var filter = aliasFilter();
		filter.setSourceId(CRAFTED_SOURCE);
		return filter;
	}

	private BasicDatumCriteria ownerFilter() {
		final var filter = new BasicDatumCriteria();
		filter.setUserId(OWNER_USER);
		filter.setNodeId(ownerNodeId);
		filter.setSourceId(ORIG_SOURCE);
		return filter;
	}

	private int streamRowCount(String tableName) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM solardatm.%s WHERE stream_id = ?::uuid".formatted(tableName),
				Integer.class, orig.getStreamId().toString());
	}

	private static long datumCount(DatumRecordCounts counts) {
		return (counts != null && counts.getDatumCount() != null ? counts.getDatumCount() : 0L);
	}

	private List<GeneralNodeDatumFilterMatch> export(BasicDatumCriteria filter) {
		final List<GeneralNodeDatumFilterMatch> result = new ArrayList<>(1);
		dao.bulkExport(new ExportCallback<GeneralNodeDatumFilterMatch>() {

			@Override
			public void didBegin(Long totalResultCountEstimate) {
				// nothing to do
			}

			@Override
			public ExportCallbackAction handle(GeneralNodeDatumFilterMatch d) {
				result.add(d);
				return ExportCallbackAction.CONTINUE;
			}
		}, new BasicBulkExportOptions("test",
				Map.of(DatumEntityDao.EXPORT_PARAMETER_DATUM_CRITERIA, filter)));
		return result;
	}

	@Test
	public void find_aliasResolves() {
		// WHEN
		final var result = dao.findFiltered(dateRange(aliasFilter()));

		// THEN
		// @formatter:off
		then(result.getReturnedResultCount())
			.as("Alias owner can query the original stream")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void find_craftedAliasResolves() {
		// WHEN
		final var result = dao.findFiltered(dateRange(craftedFilter()));

		// THEN
		// @formatter:off
		then(result.getReturnedResultCount())
			.as("Alias with original stream ID resolves, so only the statements under test protect it")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void export_aliasResolves() {
		// WHEN
		final var result = export(dateRange(aliasFilter()));

		// THEN
		// @formatter:off
		then(result)
			.as("Datum of original stream exported using alias IDs")
			.singleElement()
			.satisfies(d -> {
				then(d.getId().getNodeId()).isEqualTo(hostNodeId);
				then(d.getId().getSourceId()).isEqualTo(ALIAS_SOURCE);
			})
			;
		// @formatter:on
	}

	@Test
	public void export_grantDisabled() {
		// GIVEN
		jdbcTemplate.update("UPDATE solaruser.da_datm_alias_grant SET enabled = FALSE WHERE user_id = ?",
				OWNER_USER);

		// WHEN
		final var result = export(dateRange(aliasFilter()));

		// THEN
		// @formatter:off
		then(result)
			.as("Nothing exported after grant disabled")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void export_originalNodeTransferred() {
		// GIVEN
		final Long newOwner = -9003L;
		setupTestUser(newOwner, "new-owner@localhost");
		jdbcTemplate.update("UPDATE solaruser.user_node SET user_id = ? WHERE node_id = ?", newOwner,
				ownerNodeId);

		// WHEN
		final var result = export(dateRange(aliasFilter()));

		// THEN
		// @formatter:off
		then(result)
			.as("Nothing exported after original node transferred")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void deleteFiltered() {
		// WHEN
		final long aliasResult = dao.deleteFiltered(localDateRange(craftedFilter()));

		// THEN
		// @formatter:off
		then(aliasResult)
			.as("Nothing deleted through alias")
			.isZero()
			;
		then(streamRowCount("da_datm"))
			.as("Original stream unchanged")
			.isEqualTo(1)
			;
		then(dao.deleteFiltered(localDateRange(ownerFilter())))
			.as("Owner can delete with equivalent filter")
			.isEqualTo(1L)
			;
		// @formatter:on
	}

	@Test
	public void deleteForIds_aliasStreamId() {
		// GIVEN
		final var id = nodeId(alias.getStreamId(), hostNodeId, ALIAS_SOURCE, datumTime,
				Aggregation.None);

		// WHEN
		final var result = dao.deleteForIds(HOST_USER, Set.of(id));

		// THEN
		// @formatter:off
		then(result)
			.as("Nothing deleted through alias")
			.isEmpty()
			;
		then(streamRowCount("da_datm"))
			.as("Original stream unchanged")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void deleteForIds_originalStreamId() {
		// GIVEN
		final var id = nodeId(orig.getStreamId(), ownerNodeId, ORIG_SOURCE, datumTime, Aggregation.None);

		// WHEN
		final var result = dao.deleteForIds(HOST_USER, Set.of(id));

		// THEN
		// @formatter:off
		then(result)
			.as("Alias owner cannot delete original stream")
			.isEmpty()
			;
		then(streamRowCount("da_datm"))
			.as("Original stream unchanged")
			.isEqualTo(1)
			;
		then(dao.deleteForIds(OWNER_USER, Set.of(id)))
			.as("Owner can delete")
			.hasSize(1)
			;
		// @formatter:on
	}

	@Test
	public void countDatumRecords() {
		// WHEN
		final DatumRecordCounts aliasResult = dao.countDatumRecords(localDateRange(craftedFilter()));

		// THEN
		// @formatter:off
		then(datumCount(aliasResult))
			.as("Nothing counted through alias")
			.isZero()
			;
		then(datumCount(dao.countDatumRecords(localDateRange(ownerFilter()))))
			.as("Owner can count with equivalent filter")
			.isEqualTo(1L)
			;
		// @formatter:on
	}

	@Test
	public void markDatumAggregatesStale() {
		// WHEN
		final int aliasResult = dao.markDatumAggregatesStale(dateRange(craftedFilter()));

		// THEN
		// @formatter:off
		then(aliasResult)
			.as("Nothing marked through alias")
			.isZero()
			;
		then(streamRowCount("agg_stale_datm"))
			.as("Original stream unchanged")
			.isZero()
			;
		then(dao.markDatumAggregatesStale(dateRange(ownerFilter())))
			.as("Owner can mark with equivalent filter")
			.isPositive()
			;
		// @formatter:on
	}

	@Test
	public void findStaleAggregateDatum() {
		// GIVEN
		dao.markDatumAggregatesStale(dateRange(ownerFilter()));

		// WHEN
		final var aliasResult = dao.findStaleAggregateDatum(dateRange(craftedFilter()));

		// THEN
		// @formatter:off
		then(aliasResult.getReturnedResultCount())
			.as("Nothing found through alias")
			.isZero()
			;
		then(dao.findStaleAggregateDatum(dateRange(ownerFilter())).getReturnedResultCount())
			.as("Owner can find with equivalent filter")
			.isPositive()
			;
		// @formatter:on
	}

	@Test
	public void findAuxiliary() {
		// GIVEN
		final BasicDatumCriteria aliasFilter = dateRange(craftedFilter());
		aliasFilter.setDatumAuxiliaryType(DatumAuxiliaryType.Reset);
		final BasicDatumCriteria ownerFilter = dateRange(ownerFilter());
		ownerFilter.setDatumAuxiliaryType(DatumAuxiliaryType.Reset);

		// WHEN
		final var aliasResult = auxDao.findFiltered(aliasFilter, null, null, null);

		// THEN
		// @formatter:off
		then(aliasResult.getReturnedResultCount())
			.as("Nothing found through alias")
			.isZero()
			;
		then(auxDao.findFiltered(ownerFilter, null, null, null).getReturnedResultCount())
			.as("Owner can find with equivalent filter")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void deleteAuxiliary() {
		// GIVEN
		final BasicDatumCriteria aliasFilter = dateRange(craftedFilter());
		aliasFilter.setDatumAuxiliaryType(DatumAuxiliaryType.Reset);
		final BasicDatumCriteria ownerFilter = dateRange(ownerFilter());
		ownerFilter.setDatumAuxiliaryType(DatumAuxiliaryType.Reset);

		// WHEN
		final long aliasResult = auxDao.deleteFiltered(aliasFilter);

		// THEN
		// @formatter:off
		then(aliasResult)
			.as("Nothing deleted through alias")
			.isZero()
			;
		then(streamRowCount("da_datm_aux"))
			.as("Original stream unchanged")
			.isEqualTo(1)
			;
		then(auxDao.deleteFiltered(ownerFilter))
			.as("Owner can delete with equivalent filter")
			.isEqualTo(1L)
			;
		// @formatter:on
	}

	@Test
	public void updateAttributes_aliasStreamId() {
		// WHEN
		final var result = dao.updateAttributes(Node, alias.getStreamId(), hostNodeId, "s/hacked",
				new String[] { "hacked" }, null, null);

		// THEN
		// @formatter:off
		then(result)
			.as("Alias ID does not match a stream")
			.isNull()
			;
		then(jdbcTemplate.queryForList(
				"SELECT node_id, source_id, names_i FROM solardatm.da_datm_meta WHERE stream_id = ?::uuid",
				orig.getStreamId().toString()))
			.as("Original stream unchanged")
			.singleElement()
			.satisfies(row -> {
				then(row.get("node_id")).isEqualTo(ownerNodeId);
				then(row.get("source_id")).isEqualTo(ORIG_SOURCE);
			})
			;
		// @formatter:on
	}

}
