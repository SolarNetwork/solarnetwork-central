/* ==================================================================
 * JdbcSolarNodeMetadataDao.java - 12/11/2024 8:34:36 pm
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

package net.solarnetwork.central.common.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.common.dao.SolarNodeMetadataFilter;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteSolarNodeMetadata;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectSolarNodeMetadata;
import net.solarnetwork.central.common.dao.jdbc.sql.StoreSolarNodeMetadata;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link SolarNodeMetadata} DAO.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcSolarNodeMetadataDao implements SolarNodeMetadataDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcSolarNodeMetadataDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends SolarNodeMetadata> getObjectType() {
		return SolarNodeMetadata.class;
	}

	@Override
	public Long save(SolarNodeMetadata entity) {
		var sql = new StoreSolarNodeMetadata(entity);
		jdbcOps.execute(sql, PreparedStatement::executeUpdate);
		return entity.getId();
	}

	@Override
	public SolarNodeMetadata get(Long id) {
		var filter = new BasicCoreCriteria();
		filter.setNodeId(id);
		var sql = new SelectSolarNodeMetadata(filter);
		List<SolarNodeMetadata> list = jdbcOps.query(sql, SolarNodeMetadataRowMapper.INSTANCE);
		return (!list.isEmpty() ? list.getFirst() : null);
	}

	@Override
	public Collection<SolarNodeMetadata> getAll(List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(SolarNodeMetadata entity) {
		jdbcOps.update(new DeleteSolarNodeMetadata(requireNonNullArgument(entity, "entity").getId()));
	}

	@Override
	public FilterResults<SolarNodeMetadata, Long> findFiltered(SolarNodeMetadataFilter filter,
			List<SortDescriptor> sorts, Long offset, Integer max) {
		var sql = new SelectSolarNodeMetadata(filter);
		List<SolarNodeMetadata> list = jdbcOps.query(sql, SolarNodeMetadataRowMapper.INSTANCE);
		return BasicFilterResults.filterResults(list, null, (long) list.size(), list.size());
	}

}
