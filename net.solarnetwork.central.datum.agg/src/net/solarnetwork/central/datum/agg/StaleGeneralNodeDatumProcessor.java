/* ==================================================================
 * StaleGeneralNodeDatumProcessor.java - Aug 27, 2014 6:18:01 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import org.osgi.service.event.EventAdmin;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.datum.biz.DatumAppEventAcceptor;
import net.solarnetwork.central.datum.domain.AggregateUpdatedEventInfo;
import net.solarnetwork.central.datum.domain.BasicDatumAppEvent;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.util.OptionalServiceCollection;

/**
 * Job to process "stale" general node datum reporting aggregate data.
 * 
 * This job executes a JDBC procedure, which is expected to return a result set
 * of 0 or 1 rows for the processed stale record. If the procedure returns no
 * result rows, the job stops immediately.
 * 
 * If {@code taskCount} is higher than {@code 1} then {@code taskCount} threads
 * will be spawned and each process {@code maximumRowCount / taskCount} rows.
 * 
 * @author matt
 * @version 1.4
 */
public class StaleGeneralNodeDatumProcessor extends TieredStoredProcedureStaleDatumProcessor {

	/**
	 * The SQL result column name for the processed datum node ID.
	 * 
	 * @since 1.4
	 */
	public static final String NODE_ID_COLUMN_NAME = "node_id";

	/**
	 * The SQL result column name for the processed datum source ID.
	 * 
	 * @since 1.4
	 */
	public static final String SOURCE_ID_COLUMN_NAME = "source_id";

	/**
	 * The SQL result column name for the processed datum aggregation key.
	 * 
	 * @since 1.4
	 */
	public static final String AGGREGATION_KEY_COLUMN_NAME = "agg_kind";

	/**
	 * The SQL result column name for the processed datum timestamp.
	 * 
	 * @since 1.4
	 */
	public static final String AGGREGATION_TIME_START_COLUMN_NAME = "ts_start";

	private OptionalServiceCollection<DatumAppEventAcceptor> datumAppEventAcceptors;

	/**
	 * Construct with properties.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 */
	public StaleGeneralNodeDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
		super(eventAdmin, jdbcOps, "stale general data");
		setJdbcCall("{call solaragg.process_one_agg_stale_datum(?)}");
	}

	@Override
	protected void processResultRow(final ResultSet rs) throws SQLException {
		final OptionalServiceCollection<DatumAppEventAcceptor> services = getDatumAppEventAcceptors();
		if ( services == null ) {
			return;
		}
		final BasicDatumAppEvent event = extractAppEvent(rs);
		if ( event == null ) {
			return;
		}
		final ExecutorService executor = getExecutorService();
		Runnable task = new Runnable() {

			@Override
			public void run() {
				final Iterable<DatumAppEventAcceptor> acceptors = (services != null ? services.services()
						: Collections.emptySet());
				for ( DatumAppEventAcceptor acceptor : acceptors ) {
					try {
						acceptor.offerDatumEvent(event);
					} catch ( Throwable t ) {
						Throwable root = t;
						while ( root.getCause() != null ) {
							root = root.getCause();
						}
						log.error("Error offering datum event {} to {}: {}", event, acceptor,
								root.toString(), t);
					}
				}
			}

		};
		if ( executor != null ) {
			executor.execute(task);
		} else {
			task.run();
		}
	}

	private BasicDatumAppEvent extractAppEvent(ResultSet rs) throws SQLException {
		long nodeId = rs.getLong(NODE_ID_COLUMN_NAME);
		if ( rs.wasNull() ) {
			return null;
		}
		String sourceId = rs.getString(SOURCE_ID_COLUMN_NAME);
		if ( sourceId == null ) {
			return null;
		}
		String aggKind = rs.getString(AGGREGATION_KEY_COLUMN_NAME);
		Aggregation agg;
		try {
			agg = Aggregation.forKey(aggKind);
		} catch ( IllegalArgumentException e ) {
			return null;
		}
		Timestamp ts = rs.getTimestamp(AGGREGATION_TIME_START_COLUMN_NAME);
		if ( ts == null ) {
			return null;
		}
		AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo(agg,
				Instant.ofEpochMilli(ts.getTime()));
		return new BasicDatumAppEvent(AggregateUpdatedEventInfo.AGGREGATE_UPDATED_TOPIC,
				info.toEventProperties(), nodeId, sourceId);
	}

	/**
	 * Get the aggregate process type.
	 * 
	 * @return the type
	 */
	public String getAggregateProcessType() {
		return getTierProcessType();
	}

	/**
	 * Set the type of aggregate data to process. This is the first parameter
	 * passed to the JDBC procedure.
	 * 
	 * @param aggregateProcessType
	 *        the type to set
	 */
	public void setAggregateProcessType(String aggregateProcessType) {
		setTierProcessType(aggregateProcessType);
	}

	/**
	 * Get the maximum aggregate rows to process per procedure call.
	 * 
	 * @return the maximum row count
	 */
	public int getAggregateProcessMax() {
		Integer max = getTierProcessMax();
		return (max != null ? max.intValue() : 0);
	}

	/**
	 * Set the maximum number of aggregate rows to process per procedure call.
	 * 
	 * <p>
	 * This is the second parameter passed to the JDBC procedure. Default is
	 * {@code 1}.
	 * </p>
	 * 
	 * @param aggregateProcessMax
	 *        the maximum number of rows
	 */
	public void setAggregateProcessMax(int aggregateProcessMax) {
		setTierProcessMax(aggregateProcessMax);
	}

	/**
	 * Get a collection of {@link DatumAppEventAcceptor} services to publish
	 * {@link AggregateUpdatedEventInfo#AGGREGATE_UPDATED_TOPIC} events to.
	 * 
	 * @return the services
	 * @since 1.4
	 */
	public OptionalServiceCollection<DatumAppEventAcceptor> getDatumAppEventAcceptors() {
		return datumAppEventAcceptors;
	}

	/**
	 * Set a collection of {@link DatumAppEventAcceptor} services to publish
	 * {@link AggregateUpdatedEventInfo#AGGREGATE_UPDATED_TOPIC} events to.
	 * 
	 * @param datumAppEventAcceptors
	 *        the services to set
	 * @since 1.4
	 */
	public void setDatumAppEventAcceptors(
			OptionalServiceCollection<DatumAppEventAcceptor> datumAppEventAcceptors) {
		this.datumAppEventAcceptors = datumAppEventAcceptors;
	}

}
