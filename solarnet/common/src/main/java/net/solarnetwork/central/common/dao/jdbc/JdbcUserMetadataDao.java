/* ==================================================================
 * JdbcUserMetadataDao.java - 24 Sept 2026 6:28:04 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteForId;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectUserMetadataEntity;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectUserMetadataPath;
import net.solarnetwork.central.common.dao.jdbc.sql.StoreUserMetadataEntity;
import net.solarnetwork.central.dao.BasicUserMetadataFilter;
import net.solarnetwork.central.dao.UserMetadataDao;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@link UserMetadataEntity} DAO.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserMetadataDao implements UserMetadataDao {

	private static final RowMapper<@Nullable String> PATH_ROW_MAPPER = SingleColumnRowMapper
			.newInstance(String.class);

	private final JdbcOperations jdbcOps;

	/**
	 * Constructor.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcUserMetadataDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
	}

	@Override
	public Class<? extends UserMetadataEntity> getObjectType() {
		return UserMetadataEntity.class;
	}

	@Override
	public Long save(UserMetadataEntity entity) {
		var sql = new StoreUserMetadataEntity(entity);
		jdbcOps.execute(sql, PreparedStatement::executeUpdate);
		return nonnull(entity.getId(), "id");
	}

	@Override
	public @Nullable UserMetadataEntity get(Long id) {
		var filter = new BasicUserMetadataFilter();
		filter.setUserId(id);
		var sql = new SelectUserMetadataEntity(filter);
		List<UserMetadataEntity> list = jdbcOps.query(sql, UserMetadataEntityRowMapper.INSTANCE);
		return (!list.isEmpty() ? list.getFirst() : null);
	}

	@Override
	public Collection<UserMetadataEntity> getAll(@Nullable List<SortDescriptor> sorts) {
		throw new UnsupportedOperationException();
	}

	private static final String TABLE_NAME = "solaruser.user_meta";
	private static final String PRIMARY_KEY_COLUMN_NAME = "user_id";

	@Override
	public void delete(UserMetadataEntity entity) {
		var sql = new DeleteForId(requireNonNullArgument(entity, "entity").getUserId(), TABLE_NAME,
				PRIMARY_KEY_COLUMN_NAME);
		jdbcOps.update(sql);
	}

	@Override
	public FilterResults<UserMetadataEntity, Long> findFiltered(UserMetadataFilter filter,
			@Nullable List<SortDescriptor> sorts, @Nullable Long offset, @Nullable Integer max) {
		UserMetadataFilter f = requireNonNullArgument(filter, "filter");
		if ( sorts != null || offset != null || max != null ) {
			// explicit sort/pagination arguments override those of the given filter
			var criteria = new BasicUserMetadataFilter(filter);
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
		var sql = new SelectUserMetadataEntity(f);
		return executeFilterQuery(jdbcOps, f, sql, UserMetadataEntityRowMapper.INSTANCE);
	}

	@Override
	public @Nullable String jsonMetadataAtPath(UserMetadataFilter filter, String path) {
		final var sql = new SelectUserMetadataPath(filter, path);
		final List<@Nullable String> results = jdbcOps.query(sql, PATH_ROW_MAPPER);
		return (!results.isEmpty() ? results.getFirst() : null);
	}

}
