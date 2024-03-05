/* ==================================================================
 * JdbcInputDataEntityDao.java - 5/03/2024 11:00:13 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.din.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForCompositeKey;
import net.solarnetwork.central.din.dao.InputDataEntityDao;
import net.solarnetwork.central.din.dao.jdbc.sql.SelectInputDataEntity;
import net.solarnetwork.central.din.dao.jdbc.sql.UpsertInputDataEntity;
import net.solarnetwork.central.din.dao.jdbc.sql.UpsertInputDataReturnPrevious;
import net.solarnetwork.central.din.domain.InputDataEntity;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link InputDataEntityDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcInputDataEntityDao implements InputDataEntityDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcInputDataEntityDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends InputDataEntity> getObjectType() {
		return InputDataEntity.class;
	}

	@Override
	public UserLongStringCompositePK save(InputDataEntity entity) {
		final var sql = new UpsertInputDataEntity(entity);
		int count = jdbcOps.update(sql);
		return (count > 0 ? entity.getId() : null);
	}

	@Override
	public InputDataEntity get(UserLongStringCompositePK id) {
		var sql = new SelectInputDataEntity(id);
		var results = jdbcOps.query(sql, InputDataEntityRowMapper.INSTANCE);
		return (results != null && !results.isEmpty() ? results.get(0) : null);
	}

	@Override
	public Collection<InputDataEntity> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solardin.din_input_data";
	private static final String[] PK_COLUMN_NAMES = new String[] { "user_id", "node_id", "source_id" };

	@Override
	public void delete(InputDataEntity entity) {
		DeleteForCompositeKey sql = new DeleteForCompositeKey(
				requireNonNullArgument(entity, "entity").getId(), TABLE_NAME, PK_COLUMN_NAMES);
		jdbcOps.update(sql);
	}

	private static final PreparedStatementCallback<byte[]> DATA_CALLBACK = new PreparedStatementCallback<byte[]>() {

		@Override
		public byte[] doInPreparedStatement(PreparedStatement ps)
				throws SQLException, DataAccessException {
			if ( ps.execute() ) {
				try (ResultSet rs = ps.getResultSet()) {
					if ( rs.next() ) {
						return rs.getBytes(1);
					}
				}
			}
			return null;
		}
	};

	@Override
	public byte[] getAndPut(UserLongStringCompositePK id, byte[] data) {
		UpsertInputDataReturnPrevious sql = new UpsertInputDataReturnPrevious(id, data);
		return jdbcOps.execute(sql, DATA_CALLBACK);
	}

}
