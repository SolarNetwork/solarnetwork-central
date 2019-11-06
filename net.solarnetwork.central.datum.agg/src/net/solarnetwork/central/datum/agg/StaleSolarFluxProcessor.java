/* ==================================================================
 * StaleSolarFluxProcessor.java - 1/11/2019 3:06:47 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.agg;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.dao.AggregationFilterableDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.util.OptionalService;

/**
 * Tiered stale datum processor that processes all tiers of stale SolarFlux
 * records and publishes the associated updated data to SolarFlux.
 * 
 * <p>
 * The {@code jdbcCall} property is used to query the database for the stale
 * data to process. This query must accept no parameters. It must return the
 * following columns, in the following order:
 * </p>
 * 
 * <ol>
 * <li><b>agg_kind</b> - the datum aggregate type (String)</li>
 * <li><b>node_id</b> - the node ID (Long)</li>
 * <li><b>source_id</b> - the source ID (String)</li>
 * </ol>
 * 
 * <p>
 * This query must support locking the returned row(s) for update, so that the
 * row can be deleted once processed and (ideally) skipped by other processors
 * executing the same query.
 * </p>
 * 
 * @author matt
 * @version 1.1
 * @since 1.7
 */
public class StaleSolarFluxProcessor extends TieredStaleDatumProcessor {

	/** The default value for the {@code jdbcCall} property. */
	public static final String DEFAULT_SQL_STALE_QUERY = "SELECT * FROM solaragg.agg_stale_flux LIMIT 1 FOR UPDATE SKIP LOCKED";

	private final AggregationFilterableDao<ReportingGeneralNodeDatumMatch, AggregateGeneralNodeDatumFilter> datumDao;
	private final OptionalService<AggregateDatumProcessor> publisher;
	private final AggregateSupportDao supportDao;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 * @param datumDao
	 *        the DAO to use for finding the most recent datum data to post to
	 *        SolarFlux
	 * @param publisher
	 *        the processor to publish the stale solar flux data
	 * @param supportDao
	 *        the support DAO
	 */
	public StaleSolarFluxProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps,
			GeneralNodeDatumDao datumDao, OptionalService<AggregateDatumProcessor> publisher,
			AggregateSupportDao supportDao) {
		super(eventAdmin, jdbcOps, "stale SolarFlux data");
		this.datumDao = datumDao;
		this.publisher = publisher;
		this.supportDao = supportDao;
		setJobGroup("Datum");
		setMaximumWaitMs(1800000L);
		setTierProcessType("*");
		setJdbcCall(DEFAULT_SQL_STALE_QUERY);
	}

	@Override
	protected final int execute(AtomicInteger remainingCount) {
		final AggregateDatumProcessor aggProcessor = publisher();
		if ( aggProcessor == null || !aggProcessor.isConfigured() ) {
			return 0;
		}
		return getJdbcOps().execute(new ConnectionCallback<Integer>() {

			@Override
			public Integer doInConnection(Connection con) throws SQLException, DataAccessException {
				con.setAutoCommit(false);
				int processedCount = 0;
				DatumFilterCommand filter = new DatumFilterCommand();
				filter.setMostRecent(true);
				int resultCount = 0;
				do {
					try (PreparedStatement query = con.prepareStatement(getJdbcCall(),
							ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
							ResultSet rs = query.executeQuery()) {
						resultCount = 0;
						while ( rs.next() ) {
							resultCount++;

							boolean handled = false;
							try {
								final Aggregation agg = Aggregation.forKey(rs.getString(1));
								filter.setAggregate(agg);
								final Long nodeId = rs.getLong(2);
								filter.setNodeId(nodeId);
								filter.setSourceId(rs.getString(3));
								FilterResults<ReportingGeneralNodeDatumMatch> results = datumDao
										.findAggregationFiltered(filter, null, null, null);
								ReportingGeneralNodeDatumMatch datum = null;
								for ( ReportingGeneralNodeDatumMatch match : results ) {
									// we only care about first match (and expect only one)
									datum = match;
									handled = true;
									break;
								}
								if ( datum != null ) {
									Long userId = userIdForNodeId(nodeId);
									handled = aggProcessor.processStaleAggregateDatum(userId, agg,
											datum);

								}
							} catch ( IllegalArgumentException e ) {
								log.error("Unsupported stale type: {}", e.toString());
								handled = true;
							} finally {
								if ( handled ) {
									rs.deleteRow();
								}
							}

							remainingCount.decrementAndGet();
							processedCount++;
						}
						con.commit();
					}
				} while ( resultCount > 0 && remainingCount.get() > 0 );
				return processedCount;
			}
		});
	}

	private Long userIdForNodeId(Long nodeId) {
		AggregateSupportDao dao = getSupportDao();
		if ( dao != null ) {
			return dao.userIdForNodeId(nodeId);
		}
		return null;
	}

	private AggregateDatumProcessor publisher() {
		OptionalService<AggregateDatumProcessor> s = getPublisher();
		return (s != null ? s.service() : null);
	}

	/**
	 * Get the publisher.
	 * 
	 * @return the publisher
	 */
	public OptionalService<AggregateDatumProcessor> getPublisher() {
		return publisher;
	}

	/**
	 * Get the aggregate support DAO.
	 * 
	 * @return the support DAO
	 */
	public AggregateSupportDao getSupportDao() {
		return supportDao;
	}

}
