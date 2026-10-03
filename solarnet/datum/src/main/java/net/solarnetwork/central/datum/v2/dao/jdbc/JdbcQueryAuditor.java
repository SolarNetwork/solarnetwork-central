/* ==================================================================
 * JdbcAuditor.java - 14/02/2018 10:11:12 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.sql.Connection;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.common.dao.jdbc.BaseJdbcDatumIdServiceAuditor;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.domain.FilterMatch;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.util.StatTracker;

/**
 * {@link QueryAuditor} implementation that uses JDBC statements to update audit
 * data when datum is queried.
 *
 * <p>
 * This class uses a {@link Clock} to determine the "audit date". The default
 * clock uses an hour-based tick settings so that audit counts are grouped into
 * hour-based time buckets, and thus result in hour-based rows in the database:
 * </p>
 *
 * <pre>
 * <code>
 * Clock.tick(Clock.systemUTC(), Duration.ofHours(1))
 * </code>
 * </pre>
 *
 * <p>
 * This class opens and maintains a single JDBC {@link Connection} in a
 * dedicated thread. All database updates are buffered in memory and then
 * flushed to the database after the configured {@code flushDelay}. If the
 * connection is lost, a new connection will be created. When the service is
 * shut down, any updates not yet flushed are written before it stops.
 * </p>
 *
 * <p>
 * Counts are keyed by node ID and source ID.
 * </p>
 *
 * @author matt
 * @version 2.5
 */
public class JdbcQueryAuditor extends BaseJdbcDatumIdServiceAuditor implements QueryAuditor {

	/** The default value for the {@code statLogUpdateCount} property. */
	public static final int DEFAULT_STAT_LOG_UPDATE_COUNT = 500;

	/**
	 * The default value for the {@code serviceIncrementSql} property.
	 *
	 * @since 2.5
	 */
	public static final String DEFAULT_SERVICE_INCREMENT_SQL = "{call solardatm.audit_increment_datum_q_count(?,?,?,?)}";

	private static final ThreadLocal<Map<GeneralNodeDatumPK, Integer>> auditResultMap = ThreadLocal
			.withInitial(HashMap::new);

	/**
	 * Constructor.
	 *
	 * @param dataSource
	 *        the JDBC data source to use
	 */
	public JdbcQueryAuditor(DataSource dataSource) {
		this(dataSource, new ConcurrentHashMap<>(64));
	}

	/**
	 * Constructor.
	 *
	 * @param dataSource
	 *        the JDBC data source to use
	 * @param nodeSourceCounters
	 *        the map to use for tracking counts for node datum; the map must
	 *        perform {@code compute()} atomically, as {@link ConcurrentHashMap}
	 *        does
	 * @throws IllegalArgumentException
	 *         if any parameter is {@code null}
	 */
	public JdbcQueryAuditor(DataSource dataSource,
			ConcurrentMap<DatumId, AtomicInteger> nodeSourceCounters) {
		this(Clock.tick(Clock.systemUTC(), Duration.ofHours(1)), dataSource, nodeSourceCounters,
				new StatTracker("QueryAuditor", null, LoggerFactory.getLogger(JdbcQueryAuditor.class),
						1000));
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use; use an appropriate tick duration for auditing
	 *        date derivation
	 * @param dataSource
	 *        the JDBC data source to use
	 * @param nodeSourceCounters
	 *        the map to use for tracking counts for node datum; the map must
	 *        perform {@code compute()} atomically, as {@link ConcurrentHashMap}
	 *        does
	 * @param statCounter
	 *        the statistics to track
	 * @throws IllegalArgumentException
	 *         if any parameter is {@code null}
	 * @since 2.1
	 */
	public JdbcQueryAuditor(Clock clock, DataSource dataSource,
			ConcurrentMap<DatumId, AtomicInteger> nodeSourceCounters, StatTracker statCounter) {
		super(dataSource, nodeSourceCounters, clock, statCounter);
		setServiceIncrementSql(DEFAULT_SERVICE_INCREMENT_SQL);
		setStatLogUpdateCount(DEFAULT_STAT_LOG_UPDATE_COUNT);
	}

	@Override
	public Clock getAuditClock() {
		return clock;
	}

	@Override
	public <T extends FilterMatch<GeneralNodeDatumPK>> void auditNodeDatumFilterResults(
			GeneralNodeDatumFilter filter, FilterResults<T, GeneralNodeDatumPK> results) {
		final int returnedCount = (results != null ? results.getReturnedResultCount() : 0);
		// if no results, no count
		if ( results == null || returnedCount < 1 ) {
			return;
		}

		// configure date to current time; the clock is expected to truncate if desired
		final Instant auditDate = Instant.now(clock);

		final Map<GeneralNodeDatumPK, Integer> resultMap = auditResultMap.get();

		// try shortcut for single node + source
		Long[] nodeIds = filter.getNodeIds();
		String[] sourceIds = filter.getSourceIds();
		if ( nodeIds != null && nodeIds.length == 1 && sourceIds != null && sourceIds.length == 1 ) {
			GeneralNodeDatumPK pk = nodeDatumKey(auditDate, nodeIds[0], sourceIds[0]);
			addNodeSourceCount(pk, returnedCount);
			resultMap.put(pk, resultMap.getOrDefault(pk, 0) + returnedCount);
			return;
		}

		// coalesce counts by key first to simplify inserts into counters
		Map<GeneralNodeDatumPK, Integer> counts = new HashMap<>(returnedCount);
		for ( FilterMatch<GeneralNodeDatumPK> result : results ) {
			GeneralNodeDatumPK id = nonnull(result.getId(), "ID");
			GeneralNodeDatumPK pk = nodeDatumKey(auditDate, nonnull(id.getNodeId(), "nodeId"),
					nonnull(id.getSourceId(), "sourceId"));
			counts.compute(pk, (_, v) -> v == null ? 1 : v + 1);
		}

		// insert counts
		for ( Map.Entry<GeneralNodeDatumPK, Integer> me : counts.entrySet() ) {
			GeneralNodeDatumPK key = me.getKey();
			Integer val = me.getValue();
			addNodeSourceCount(key, val);
			resultMap.put(key, resultMap.getOrDefault(key, 0) + val);
		}
	}

	@Override
	public void addNodeDatumAuditResults(Map<GeneralNodeDatumPK, Integer> results) {
		for ( Map.Entry<GeneralNodeDatumPK, Integer> me : results.entrySet() ) {
			GeneralNodeDatumPK key = me.getKey();
			Integer val = me.getValue();
			addNodeSourceCount(key, val);
		}
	}

	@Override
	public Map<GeneralNodeDatumPK, Integer> currentAuditResults() {
		return auditResultMap.get();
	}

	@Override
	public void resetCurrentAuditResults() {
		auditResultMap.get().clear();
	}

	@Override
	public void auditNodeDatum(Datum datum) {
		if ( datum == null || datum.getKind() != ObjectDatumKind.Node || datum.getSourceId() == null ) {
			return;
		}
		addServiceCount(DatumId.nodeId(nonnull(datum.getObjectId(), "objectId"),
				nonnull(datum.getSourceId(), "sourceId"), clock.instant()), 1);
	}

	private static GeneralNodeDatumPK nodeDatumKey(Instant date, Long nodeId, String sourceId) {
		return new GeneralNodeDatumPK(nodeId, date, sourceId);
	}

	private void addNodeSourceCount(GeneralNodeDatumPK key, int count) {
		addServiceCount(DatumId.nodeId(key.getNodeId(), key.getSourceId(), key.getCreated()), count);
	}

	@Override
	public String getPingTestName() {
		return "JDBC Query Auditor";
	}

}
