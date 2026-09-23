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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.executeFilterQuery;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.common.dao.SolarNodeMetadataFilter;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForId;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectSolarNodeMetadata;
import net.solarnetwork.central.common.dao.jdbc.sql.StoreSolarNodeMetadata;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link SolarNodeMetadata} DAO.
 *
 * @author matt
 * @version 1.2
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
		return nonnull(entity.getId(), "id");
	}

	@Override
	public @Nullable SolarNodeMetadata get(Long id) {
		var filter = new BasicCoreCriteria();
		filter.setNodeId(id);
		var sql = new SelectSolarNodeMetadata(filter);
		List<SolarNodeMetadata> list = jdbcOps.query(sql, SolarNodeMetadataRowMapper.INSTANCE);
		return (!list.isEmpty() ? list.getFirst() : null);
	}

	@Override
	public Collection<SolarNodeMetadata> getAll(@Nullable List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solarnet.sn_node_meta";
	private static final String PRIMARY_KEY_COLUMN_NAME = "node_id";

	@Override
	public void delete(SolarNodeMetadata entity) {
		var sql = new DeleteForId(
				requireNonNullArgument(requireNonNullArgument(entity, "entity").getNodeId(),
						"entity.nodeId"),
				TABLE_NAME, PRIMARY_KEY_COLUMN_NAME);
		jdbcOps.update(sql);
	}

	@Override
	public FilterResults<SolarNodeMetadata, Long> findFiltered(SolarNodeMetadataFilter filter,
			@Nullable List<SortDescriptor> sorts, @Nullable Long offset, @Nullable Integer max) {
		SolarNodeMetadataFilter f = requireNonNullArgument(filter, "filter");
		if ( sorts != null || offset != null || max != null ) {
			// explicit sort/pagination arguments override those of the given filter
			var criteria = new BasicCoreCriteria(filter);
			if ( sorts != null ) {
				criteria.setSorts(sorts);
			}
			if ( offset != null ) {
				criteria.setOffset(offset);
			}
			if ( max != null ) {
				criteria.setMax(max);
			}
			f = criteria;
		}
		var sql = new SelectSolarNodeMetadata(f);
		return executeFilterQuery(jdbcOps, f, sql, SolarNodeMetadataRowMapper.INSTANCE);
	}

}
