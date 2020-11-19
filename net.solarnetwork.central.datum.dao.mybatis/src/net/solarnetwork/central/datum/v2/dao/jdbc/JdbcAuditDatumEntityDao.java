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
import net.solarnetwork.central.datum.v2.dao.AuditDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.AuditDatumDao;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.Aggregation;
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

	private static Aggregation aggregationForAuditDatumCriteria(AuditDatumCriteria filter) {
		// limit aggregation to specific supported ones
		Aggregation aggregation = Aggregation.Day;
		if ( filter != null && filter.getAggregation() != null ) {
			switch (filter.getAggregation()) {
				case Hour:
				case Day:
				case Month:
					aggregation = filter.getAggregation();
					break;

				default:
					// ignore all others
			}
		}
		return aggregation;
	}

	@Override
	public FilterResults<AuditDatumRollup, DatumPK> findAuditDatumFiltered(AuditDatumCriteria filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FilterResults<AuditDatumRollup, DatumPK> findAccumulativeAuditDatumFiltered(
			AuditDatumCriteria filter) {
		// TODO Auto-generated method stub
		return null;
	}

}
