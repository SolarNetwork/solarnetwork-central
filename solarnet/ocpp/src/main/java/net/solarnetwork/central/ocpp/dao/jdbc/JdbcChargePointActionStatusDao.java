/* ==================================================================
 * JdbcChargePointActionStatusDao.java - 16/11/2022 5:40:42 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusDao;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusFilter;
import net.solarnetwork.central.ocpp.dao.jdbc.sql.SelectChargePointActionStatus;
import net.solarnetwork.central.ocpp.dao.jdbc.sql.UpsertChargePointIdentifierActionTimestamp;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatus;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatusKey;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link ChargePointActionStatusDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargePointActionStatusDao implements ChargePointActionStatusDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcChargePointActionStatusDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public void updateActionTimestamp(Long userId, String chargePointIdentifier, Integer connectorId,
			String action, Instant date) {
		var sql = new UpsertChargePointIdentifierActionTimestamp(userId, chargePointIdentifier,
				connectorId, action, date);
		jdbcOps.update(sql);
	}

	@Override
	public FilterResults<ChargePointActionStatus, ChargePointActionStatusKey> findFiltered(
			ChargePointActionStatusFilter filter, List<SortDescriptor> sorts, Integer offset,
			Integer max) {
		requireNonNullArgument(filter, "filter");
		final PreparedStatementCreator sql = new SelectChargePointActionStatus(filter);
		List<ChargePointActionStatus> list = jdbcOps.query(sql,
				ChargePointActionStatusRowMapper.INSTANCE);
		return BasicFilterResults.filterResults(list, null, (long) list.size(), list.size());
	}

	@Override
	public void findFilteredStream(ChargePointActionStatusFilter filter,
			FilteredResultsProcessor<ChargePointActionStatus> processor,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) throws IOException {
		requireNonNullArgument(filter, "filter");
		requireNonNullArgument(processor, "processor");
		final PreparedStatementCreator sql = new SelectChargePointActionStatus(filter);
		final RowMapper<ChargePointActionStatus> mapper = ChargePointActionStatusRowMapper.INSTANCE;
		processor.start(null, null, null, Collections.emptyMap());
		try {
			jdbcOps.execute(sql, new PreparedStatementCallback<Void>() {

				@Override
				public Void doInPreparedStatement(PreparedStatement ps)
						throws SQLException, DataAccessException {
					try (ResultSet rs = ps.executeQuery()) {
						int row = 0;
						while ( rs.next() ) {
							ChargePointActionStatus d = mapper.mapRow(rs, ++row);
							processor.handleResultItem(d);
						}
					} catch ( IOException e ) {
						throw new RuntimeException(e);
					}
					return null;
				}
			});
		} catch ( RuntimeException e ) {
			if ( e.getCause() instanceof IOException ) {
				throw (IOException) e.getCause();
			}
			throw e;
		}
	}

}
