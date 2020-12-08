/* ==================================================================
 * JdbcAuditDatumEntityDao.java - 20/11/2020 10:11:29 am
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

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import net.solarnetwork.central.datum.v2.dao.AuditDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.AuditDatumDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectAccumulativeAuditDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectAuditDatum;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.dao.FilterResults;

/**
 * {@link JdbcOperations} based implementation of {@link AuditDatumDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcAuditDatumEntityDao implements AuditDatumDao {

	private final JdbcOperations jdbcTemplate;

	/**
	 * Constructor.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @throws IllegalArgumentException
	 *         if {@code jdbcTemplate} is {@literal null}
	 */
	public JdbcAuditDatumEntityDao(JdbcOperations jdbcTemplate) {
		super();
		if ( jdbcTemplate == null ) {
			throw new IllegalArgumentException("The jdbcTemplate argument must not be null.");
		}
		this.jdbcTemplate = jdbcTemplate;
	}

	private FilterResults<AuditDatumRollup, DatumPK> findFiltered(AuditDatumCriteria filter,
			PreparedStatementCreator sql) {
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter must be provided.");
		}
		return DatumJdbcUtils.executeFilterQuery(jdbcTemplate, filter, sql,
				AuditDatumEntityRollupRowMapper.INSTANCE);
	}

	@Override
	public FilterResults<AuditDatumRollup, DatumPK> findAuditDatumFiltered(AuditDatumCriteria filter) {
		return findFiltered(filter, new SelectAuditDatum(filter));
	}

	@Override
	public FilterResults<AuditDatumRollup, DatumPK> findAccumulativeAuditDatumFiltered(
			AuditDatumCriteria filter) {
		return findFiltered(filter, new SelectAccumulativeAuditDatum(filter));
	}

}
