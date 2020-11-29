/* ==================================================================
 * JdbcReadingDatumEntityDao.java - 17/11/2020 4:06:41 pm
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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectReadingDifference;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.dao.FilterResults;

/**
 * {@link JdbcOperations} based implementation of {@link ReadingDatumDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class JdbcReadingDatumEntityDao implements ReadingDatumDao {

	private final JdbcOperations jdbcTemplate;

	/**
	 * Constructor.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @throws IllegalArgumentException
	 *         if {@code jdbcTemplate} is {@literal null}
	 */
	public JdbcReadingDatumEntityDao(JdbcOperations jdbcTemplate) {
		super();
		if ( jdbcTemplate == null ) {
			throw new IllegalArgumentException("The jdbcTemplate argument must not be null.");
		}
		this.jdbcTemplate = jdbcTemplate;
	}

	private RowMapper<ReadingDatum> mapper(Aggregation agg) {
		if ( agg == null ) {
			agg = Aggregation.None;
		}
		switch (agg) {
			case Hour:
				return ReadingDatumEntityRowMapper.HOUR_INSTANCE;

			case Day:
				return ReadingDatumEntityRowMapper.DAY_INSTANCE;

			case Month:
				return ReadingDatumEntityRowMapper.MONTH_INSTANCE;

			default:
				return ReadingDatumEntityRowMapper.INSTANCE;
		}
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<ReadingDatum, DatumPK> findDatumReadingFiltered(ReadingDatumCriteria filter) {
		if ( filter == null || filter.getReadingType() == null ) {
			throw new IllegalArgumentException("The filter reading type must be provided.");
		}
		PreparedStatementCreator creator = null;
		switch (filter.getReadingType()) {
			case Difference:
			case DifferenceWithin:
			case NearestDifference:
				creator = new SelectReadingDifference(filter);
				break;

			// TODO
			//case CalculatedAt:
			//case CalculatedAtDifference:

		}

		return DatumSqlUtils.executeFilterQuery(jdbcTemplate, filter, creator,
				mapper(filter.getAggregation()));
	}

}
