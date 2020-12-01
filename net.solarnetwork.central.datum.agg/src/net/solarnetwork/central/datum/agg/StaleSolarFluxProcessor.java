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
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.jdbc.StaleFluxDatumRowMapper;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectStaleFluxDatum;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.StaleFluxDatum;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
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
 * <li><b>stream_id</b> - the stream ID (UUID)</li>
 * <li><b>agg_kind</b> - the datum aggregate type (String)</li>
 * </ol>
 * 
 * <p>
 * This query must support locking the returned row(s) for update, so that the
 * row can be deleted once processed and (ideally) skipped by other processors
 * executing the same query.
 * </p>
 * 
 * @author matt
 * @version 1.2
 * @since 1.7
 */
public class StaleSolarFluxProcessor extends TieredStaleDatumProcessor {

	private final DatumEntityDao datumDao;
	private final OptionalService<DatumProcessor> publisher;

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
	 */
	public StaleSolarFluxProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps,
			DatumEntityDao datumDao, OptionalService<DatumProcessor> publisher) {
		super(eventAdmin, jdbcOps, "stale SolarFlux data");
		this.datumDao = datumDao;
		this.publisher = publisher;
		setJobGroup("Datum");
		setMaximumWaitMs(1800000L);
		setTierProcessType("*");
		setJdbcCall(SelectStaleFluxDatum.ANY_ONE_FOR_UPDATE.getSql());
	}

	@Override
	protected final int execute(AtomicInteger remainingCount) {
		final DatumProcessor aggProcessor = publisher();
		if ( aggProcessor == null || !aggProcessor.isConfigured() ) {
			return 0;
		}
		return getJdbcOps().execute(new ConnectionCallback<Integer>() {

			@Override
			public Integer doInConnection(Connection con) throws SQLException, DataAccessException {
				con.setAutoCommit(false);
				int processedCount = 0;
				BasicDatumCriteria filter = new BasicDatumCriteria();
				filter.setMostRecent(true);
				int resultCount = 0;
				try (PreparedStatement query = con.prepareStatement(getJdbcCall(),
						ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
					do {
						try (ResultSet rs = query.executeQuery()) {
							resultCount = 0;
							while ( rs.next() ) {
								resultCount++;

								boolean handled = false;
								try {
									final StaleFluxDatum stale = StaleFluxDatumRowMapper.INSTANCE
											.mapRow(rs, resultCount);
									filter.setAggregation(stale.getKind());
									filter.setStreamId(stale.getStreamId());
									ObjectDatumStreamFilterResults<Datum, DatumPK> results = datumDao
											.findFiltered(filter);
									Datum datum = results.iterator().next();
									if ( datum != null ) {
										GeneralNodeDatum gnd = DatumUtils.toGeneralNodeDatum(datum,
												results.metadataForStream(datum.getStreamId()));
										handled = aggProcessor.processDatum(gnd, stale.getKind());
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
						} catch ( Throwable t ) {
							con.rollback();
						}
					} while ( resultCount > 0 && remainingCount.get() > 0 );
				}
				return processedCount;
			}
		});
	}

	private DatumProcessor publisher() {
		OptionalService<DatumProcessor> s = getPublisher();
		return (s != null ? s.service() : null);
	}

	/**
	 * Get the publisher.
	 * 
	 * @return the publisher
	 */
	public OptionalService<DatumProcessor> getPublisher() {
		return publisher;
	}

}
