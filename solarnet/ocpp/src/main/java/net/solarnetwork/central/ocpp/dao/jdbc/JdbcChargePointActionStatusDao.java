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
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusDao;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusFilter;
import net.solarnetwork.central.ocpp.dao.jdbc.sql.UpsertChargePointActionStatus;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatus;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatusKey;
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
	public FilterResults<ChargePointActionStatus, ChargePointActionStatusKey> findFiltered(
			ChargePointActionStatusFilter filter, List<SortDescriptor> sorts, Integer offset,
			Integer max) {
		// TODO implement
		List<ChargePointActionStatus> list = Collections.emptyList();
		return BasicFilterResults.filterResults(list, null, (long) list.size(), list.size());
	}

	@Override
	public void updateTimestamp(ChargePointActionStatusKey id, Instant ts) {
		UpsertChargePointActionStatus sql = new UpsertChargePointActionStatus(
				new ChargePointActionStatus(id, ts));
		jdbcOps.update(sql);

	}

}
