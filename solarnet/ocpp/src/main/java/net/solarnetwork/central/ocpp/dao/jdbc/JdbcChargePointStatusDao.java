/* ==================================================================
 * JdbcChargePointStatusDao.java - 17/11/2022 6:43:47 am
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

import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
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
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusDao;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusFilter;
import net.solarnetwork.central.ocpp.dao.jdbc.sql.SelectChargePointStatus;
import net.solarnetwork.central.ocpp.dao.jdbc.sql.UpsertChargePointConnectionStatus;
import net.solarnetwork.central.ocpp.dao.jdbc.sql.UpsertChargePointIdentifierConnectionStatus;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC based implementation of {@link ChargePointStatusDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargePointStatusDao implements ChargePointStatusDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcChargePointStatusDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public FilterResults<ChargePointStatus, UserLongCompositePK> findFiltered(
			ChargePointStatusFilter filter, List<SortDescriptor> sorts, Integer offset, Integer max) {
		// TODO implement
		List<ChargePointStatus> list = Collections.emptyList();
		return BasicFilterResults.filterResults(list, null, (long) list.size(), list.size());
	}

	@Override
	public void updateConnectionStatus(Long userId, String chargePointIdentifier, String connectedTo,
			Instant connectionDate) {
		UpsertChargePointIdentifierConnectionStatus sql = new UpsertChargePointIdentifierConnectionStatus(
				userId, chargePointIdentifier,
				new ChargePointStatus(unassignedEntityIdKey(userId), null, connectedTo, connectionDate));
		jdbcOps.update(sql);
	}

	@Override
	public void updateConnectionStatus(UserLongCompositePK id, String connectedTo,
			Instant connectionDate) {
		UpsertChargePointConnectionStatus sql = new UpsertChargePointConnectionStatus(
				new ChargePointStatus(id, null, connectedTo, connectionDate));
		jdbcOps.update(sql);
	}

	@Override
	public void findFilteredStream(ChargePointStatusFilter filter,
			FilteredResultsProcessor<ChargePointStatus> processor, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) throws IOException {
		requireNonNullArgument(filter, "filter");
		requireNonNullArgument(processor, "processor");
		final PreparedStatementCreator sql = new SelectChargePointStatus(filter);
		final RowMapper<ChargePointStatus> mapper = ChargePointStatusRowMapper.INSTANCE;
		processor.start(null, null, null, Collections.emptyMap());
		try {
			jdbcOps.execute(sql, new PreparedStatementCallback<Void>() {

				@Override
				public Void doInPreparedStatement(PreparedStatement ps)
						throws SQLException, DataAccessException {
					try (ResultSet rs = ps.executeQuery()) {
						int row = 0;
						while ( rs.next() ) {
							ChargePointStatus d = mapper.mapRow(rs, ++row);
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
