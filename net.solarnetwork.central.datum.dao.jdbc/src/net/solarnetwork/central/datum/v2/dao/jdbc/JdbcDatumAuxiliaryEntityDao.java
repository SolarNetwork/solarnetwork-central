/* ==================================================================
 * JdbcDatumAuxiliaryEntityDao.java - 28/11/2020 8:54:23 am
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntityDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DeleteDatumAuxiliary;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.GetDatumAuxiliary;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.MoveDatumAuxiliary;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumAuxiliary;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.StoreDatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * {@link JdbcOperations} based implementation of
 * {@link DatumAuxiliaryEntityDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class JdbcDatumAuxiliaryEntityDao implements DatumAuxiliaryEntityDao {

	private final JdbcOperations jdbcTemplate;

	/**
	 * Constructor.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @throws IllegalArgumentException
	 *         if {@code jdbcTemplate} is {@literal null}
	 */
	public JdbcDatumAuxiliaryEntityDao(JdbcOperations jdbcTemplate) {
		super();
		if ( jdbcTemplate == null ) {
			throw new IllegalArgumentException("The jdbcTemplate argument must not be null.");
		}
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Class<? extends DatumAuxiliaryEntity> getObjectType() {
		return DatumAuxiliaryEntity.class;
	}

	@Override
	public DatumAuxiliaryPK save(DatumAuxiliaryEntity entity) {
		if ( entity.getTimestamp() == null ) {
			throw new IllegalArgumentException("The timestamp property is required.");
		}
		jdbcTemplate.execute(new StoreDatumAuxiliary(entity), new CallableStatementCallback<Void>() {

			@Override
			public Void doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				cs.execute();
				return null;
			}
		});
		return entity.getId();
	}

	@Override
	public DatumAuxiliaryEntity get(DatumAuxiliaryPK id) {
		List<DatumAuxiliary> result = jdbcTemplate.query(new GetDatumAuxiliary(id),
				DatumAuxiliaryEntityRowMapper.INSTANCE);
		return (!result.isEmpty() ? (DatumAuxiliaryEntity) result.get(0) : null);
	}

	@Override
	public Collection<DatumAuxiliaryEntity> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(DatumAuxiliaryEntity entity) {
		jdbcTemplate.update(new DeleteDatumAuxiliary(entity.getId()));

	}

	@Override
	public boolean move(DatumAuxiliaryPK from, DatumAuxiliaryEntity to) {
		// TODO Auto-generated method stub
		return jdbcTemplate.execute(new MoveDatumAuxiliary(from, to),
				new CallableStatementCallback<Boolean>() {

					@Override
					public Boolean doInCallableStatement(CallableStatement cs)
							throws SQLException, DataAccessException {
						cs.execute();
						return cs.getBoolean(1);
					}
				});
	}

	@Override
	public FilterResults<DatumAuxiliary, DatumAuxiliaryPK> findFiltered(DatumAuxiliaryCriteria filter,
			List<SortDescriptor> sorts, Integer offset, Integer max) {
		return DatumJdbcUtils.executeFilterQuery(jdbcTemplate, filter, new SelectDatumAuxiliary(filter),
				DatumAuxiliaryEntityRowMapper.INSTANCE);
	}

}
