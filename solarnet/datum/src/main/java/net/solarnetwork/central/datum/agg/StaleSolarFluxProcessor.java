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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.job.TieredStaleRecordProcessor;
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
 * @version 2.0
 * @since 1.7
 */
public class StaleSolarFluxProcessor extends TieredStaleRecordProcessor {

	private final DatumEntityDao datumDao;
	private final DatumProcessor publisher;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 * @param datumDao
	 *        the DAO to use for finding the most recent datum data to post to
	 *        SolarFlux
	 * @param publisher
	 *        the processor to publish the stale SolarFlux data
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public StaleSolarFluxProcessor(JdbcOperations jdbcOps, DatumEntityDao datumDao,
			DatumProcessor publisher) {
		super(jdbcOps, "stale SolarFlux data");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.publisher = requireNonNullArgument(publisher, "publisher");
		setGroupId("Datum");
		setMaximumWaitMs(1800000L);
		setTierProcessType("*");
		setJdbcCall(SelectStaleFluxDatum.ANY_ONE_FOR_UPDATE.getSql());
	}

	@Override
	protected final int execute(AtomicInteger remainingCount) {
		if ( !publisher.isConfigured() ) {
			return 0;
		}
		final MutableInt processedCount = new MutableInt(0);
		try {
			getJdbcOps().execute((ConnectionCallback<Void>) con -> {
				con.setAutoCommit(false);
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
									assert stale != null;
									filter.setAggregation(stale.getKind());
									filter.setStreamId(stale.getStreamId());
									ObjectDatumStreamFilterResults<Datum, DatumPK> results = datumDao
											.findFiltered(filter);
									if ( results.getReturnedResultCount() > 0 ) {
										Datum datum = results.iterator().next();
										if ( datum != null ) {
											GeneralNodeDatum gnd = DatumUtils.toGeneralNodeDatum(datum,
													results.metadataForStreamId(datum.getStreamId()));
											handled = publisher.processDatum(gnd, stale.getKind());
										}
									} else {
										log.warn(
												"Most recent {} datum for stream {} not found (not node stream?).",
												stale.getKind(), stale.getStreamId());
										handled = true;
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
								processedCount.increment();
							}
							con.commit();
						}
					} while ( resultCount > 0 && remainingCount.get() > 0 );
				}
				return null;
			});
		} catch ( PessimisticLockingFailureException e ) {
			log.warn("{} processing {} with call {}: {}", e.getClass().getSimpleName(),
					getTaskDescription(), getJdbcCall(), e.getMessage());
		} catch ( Throwable e ) {
			log.error("{} processing {} with call {}: {}", e.getClass().getSimpleName(),
					getTaskDescription(), getJdbcCall(), e.getMessage(), e);
		}
		return processedCount.intValue();
	}

	/**
	 * Get the publisher.
	 *
	 * @return the publisher
	 */
	public DatumProcessor getPublisher() {
		return publisher;
	}

}
