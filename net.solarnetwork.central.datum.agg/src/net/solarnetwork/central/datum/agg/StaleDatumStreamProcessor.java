/* ==================================================================
 * StaleDatumStreamProcessor.java - 23/11/2020 6:39:30 am
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

package net.solarnetwork.central.datum.agg;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.transaction.CannotCreateTransactionException;
import net.solarnetwork.central.datum.biz.DatumAppEventAcceptor;
import net.solarnetwork.central.datum.domain.AggregateUpdatedEventInfo;
import net.solarnetwork.central.datum.domain.BasicDatumAppEvent;
import net.solarnetwork.central.datum.v2.dao.jdbc.ObjectDatumIdRowMapper;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.util.OptionalServiceCollection;

/**
 * Job to process "stale" datum stream aggregate data.
 * 
 * <p>
 * This job executes a JDBC procedure, which is expected to return a result set
 * of 0 or 1 rows for the processed stale record. If the procedure returns no
 * result rows, the job stops immediately.
 * </p>
 * 
 * <p>
 * If {@code taskCount} is higher than {@code 1} then {@code taskCount} threads
 * will be spawned and each process {@code maximumRowCount / taskCount} rows.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.14
 */
public class StaleDatumStreamProcessor extends TieredStoredProcedureStaleDatumProcessor {

	/** The default {@code jdbcCall} value. */
	public static final String DEFAULT_SQL = "{call solardatm.process_one_agg_stale_datm(?)}";

	private OptionalServiceCollection<DatumAppEventAcceptor> datumAppEventAcceptors;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 */
	public StaleDatumStreamProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
		super(eventAdmin, jdbcOps, "stale datum");
		setJdbcCall(DEFAULT_SQL);
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
					} catch ( CannotCreateTransactionException | TransientDataAccessException
							| DataAccessResourceFailureException e ) {
						log.warn("Transient DB error offering datum event {} to {}: {}", event, acceptor,
								e.toString());
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
		ObjectDatumId id = ObjectDatumIdRowMapper.INSTANCE.mapRow(rs, 1);
		if ( id == null || !id.isValidAggregateObjectId(ObjectDatumKind.Node) ) {
			return null;
		}
		switch (id.getAggregation()) {
			case Hour:
			case Day:
			case Month:
				// allowed
				break;

			default:
				// not allowed
				return null;
		}

		AggregateUpdatedEventInfo info = new AggregateUpdatedEventInfo(id.getAggregation(),
				id.getTimestamp());
		return new BasicDatumAppEvent(AggregateUpdatedEventInfo.AGGREGATE_UPDATED_TOPIC,
				info.toEventProperties(), id.getObjectId(), id.getSourceId());
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
