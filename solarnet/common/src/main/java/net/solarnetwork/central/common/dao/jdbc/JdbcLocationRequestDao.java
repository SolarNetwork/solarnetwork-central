/* ==================================================================
 * JdbcLocationRequestDao.java - 19/05/2022 2:08:33 pm
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

package net.solarnetwork.central.common.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import net.solarnetwork.central.common.dao.LocationRequestCriteria;
import net.solarnetwork.central.common.dao.LocationRequestDao;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteLocationRequest;
import net.solarnetwork.central.common.dao.jdbc.sql.InsertLocationRequest;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectLocationRequest;
import net.solarnetwork.central.common.dao.jdbc.sql.UpdateLocationRequest;
import net.solarnetwork.central.domain.LocationRequest;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link LocationRequestDao}.
 *
 * @author matt
 * @version 1.1
 * @since 1.3
 */
public class JdbcLocationRequestDao implements LocationRequestDao {

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcLocationRequestDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public FilterResults<LocationRequest, Long> findFiltered(LocationRequestCriteria filter,
			@Nullable List<SortDescriptor> sorts, @Nullable Long offset, @Nullable Integer max) {
		SelectLocationRequest sql = new SelectLocationRequest(filter); // TODO: support sorts, offset, max
		List<LocationRequest> list = jdbcOps.query(sql, LocationRequestRowMapper.INSTANCE);
		return BasicFilterResults.filterResults(list, null, (long) list.size(), list.size());
	}

	@Override
	public Class<? extends LocationRequest> getObjectType() {
		return LocationRequest.class;
	}

	@Override
	public Long save(LocationRequest entity) {
		Long result;
		if ( entity.getId() == null ) {
			final InsertLocationRequest sql = new InsertLocationRequest(entity);
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcOps.update(sql, keyHolder);
			Map<String, Object> keys = keyHolder.getKeys();
			Object id = keys != null ? keys.get("id") : null;
			result = (id instanceof Long n ? n : null);
		} else {
			final UpdateLocationRequest sql = new UpdateLocationRequest(entity);
			int count = jdbcOps.update(sql);
			result = (count > 0 ? entity.getId() : null);
		}
		return nonnull(result, "id");
	}

	@Override
	public @Nullable LocationRequest get(Long id) {
		SelectLocationRequest sql = new SelectLocationRequest(id);
		List<LocationRequest> list = jdbcOps.query(sql, LocationRequestRowMapper.INSTANCE);
		return (!list.isEmpty() ? list.getFirst() : null);
	}

	@Override
	public Collection<LocationRequest> getAll(@Nullable List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(LocationRequest entity) {
		jdbcOps.update(new DeleteLocationRequest(
				requireNonNullArgument(requireNonNullArgument(entity, "entity").getId(), "entity.id")));
	}

	@Override
	public List<LocationRequest> find(Long id, @Nullable LocationRequestCriteria filter) {
		return jdbcOps.query(new SelectLocationRequest(id, filter), LocationRequestRowMapper.INSTANCE);
	}

	@Override
	public int delete(Long id, @Nullable LocationRequestCriteria filter) {
		return jdbcOps.update(new DeleteLocationRequest(id, filter));
	}

}
